package pi.pe.nativeandroidsample;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;


import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtcCertificatePem;
import org.webrtc.PeerConnection;
import org.webrtc.RendererCommon;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SoftwareVideoDecoderFactory;
import org.webrtc.SoftwareVideoEncoderFactory;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoTrack;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.security.MessageDigest;


public class MainActivity extends android.app.Activity {

    private static String Tag = "pi.pe.nativeandroidsample.MainActivity";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private PeerConnection peerConnection;
    private ArrayList queuedRemoteCandidates;
    private PeerConnectionFactory factory;
    private PeerConnectionFactory.Options options;
    private List<PeerConnection.IceServer> iceServers;
    private PeerConnection.Observer pcObserver;
    static private RtcCertificatePem certificate;
    private PhonoSDPtoJson phonoParser;
    private String near;
    private String far = null;
    private String nonce = null;
    private String session;
    private WebSocket webSocket;
    private String myNonsense = "";
    private  DataChannel videorelay;
    private EglBase rootEglBase ;



    // a keypair should:
    // a) be unique - one per device
    // b) be generated once at app install time by the webRTC framework
    // c) saved in an encrypted _local_ keystore on the device
    // d) only retrevied when needed and mem wiped asap
    // e) definitely not checked into a public github repo.

    void makeCertificate() {

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        String key = sharedPref.getString("pipeKey",null);
        String cert = sharedPref.getString("pipeCert",null);
        if ((key != null ) && (cert !=null)){
            certificate = new RtcCertificatePem(key,cert);
        } else {
            certificate = RtcCertificatePem.generateCertificate();
            key = certificate.privateKey;
            cert = certificate.certificate;
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("pipeKey",key);
            editor.putString("pipeCert",cert);
            editor.commit();
        }
    }

    void makeIceServers() {

        iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:146.148.121.175:3478")
                .setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK).createIceServer()
        );

        iceServers.add(PeerConnection.IceServer.builder("turn:146.148.121.175:3478")
                .setUsername("panda").setPassword("panda")
                .setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK)
                .createIceServer());
    }

    void sendCandidate(IceCandidate ican){
        String canJson = phonoParser.makeCandidateMessage(ican.sdp, far, near, ican.sdpMLineIndex, myNonsense, session);
        Log.d(Tag,"sending "+canJson);
        webSocket.sendText(canJson);
    }

    void makePcO() {
        pcObserver = new PeerConnection.Observer() {

            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d(Tag + ".pco", "onSignalingChange " + signalingState.name());
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(Tag + ".pco", "onIceConnectionChange " + iceConnectionState.name());

            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {
                Log.d(Tag + ".pco", "onIceConnectionReceivingChange " + b);

            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d(Tag + ".pco", "onIceGatheringChange " + iceGatheringState.name());
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d(Tag + ".pco", "onIceCandidate " + iceCandidate.sdp);
                executor.execute(() -> sendCandidate(iceCandidate));
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d(Tag, "onAddStream video = "+ mediaStream.videoTracks.size());

            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {

            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.d(Tag + ".pco", "onDataChannel " + dataChannel.label());
            }

            @Override
            public void onRenegotiationNeeded() {
                Log.d(Tag + ".pco", "onRenegotiationNeeded ");
                makeDcOffer();
            }

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                Log.d(Tag, "onAddTrack: "+ rtpReceiver.track().kind());
                runOnUiThread(() -> addVideo(mediaStreams));
            }
        };

    }

    void addVideo(MediaStream[] mediaStreams){
        Log.d(Tag,"adding media stream(s)");
        SurfaceViewRenderer svr = (SurfaceViewRenderer) findViewById(R.id.surfaceView);

        svr.init(rootEglBase.getEglBaseContext(), null);
        svr.setEnableHardwareScaler(true);
        svr.setMirror(false);
        svr.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED);
        for (MediaStream m:mediaStreams){
            for (VideoTrack v:m.videoTracks){
                v.addSink(svr);
            }
        }
    }


    void setAnswer(SessionDescription ans){
        Log.d(Tag,"setting answer");
        peerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {

            }

            @Override
            public void onSetSuccess() {
                Log.d(Tag + " answer", "onSetSuccess ");
            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {
                Log.d(Tag + " answer", "onSetFailure " + s);

            }
        }, ans);
    }

    void setCandiate(String cs, String idx){
        IceCandidate can = new IceCandidate("data",Integer.parseInt(idx),cs);
        peerConnection.addIceCandidate(can);
    }

    void incommingMessage(String message) {
        PhonoSDPtoJson.Message from = phonoParser.makeMessageFromJson(message);
        if (from.session.equals(session)) {
            if (from.mto.equals(near)) {
                if (from.mfrom.equals(far)) {
                    switch (from.mtype) {
                        case "offer":
                            Log.d(Tag, "Got offer ");
                            break;
                        case "answer":
                            Log.d(Tag, "Got answer ");
                            String sdp = from.sdp.toSDP();
                            Log.d(Tag, "sdp is "+sdp);
                            SessionDescription ans = new SessionDescription(SessionDescription.Type.ANSWER,sdp);
                            executor.execute(()->setAnswer(ans));
                            break;
                        case "candidate":
                            Log.d(Tag, "Got candidate ");
                            String csdp = from.candidate.toSdp(null);
                            Log.d(Tag,csdp);
                            executor.execute(()->setCandiate(csdp,from.sdpMLineIndex));

                            break;
                        default:
                            Log.d(Tag, from.mtype);
                            break;
                    }
                } else {
                    Log.e(Tag, "not expecting message from you");
                }
            } else {
                Log.e(Tag, "to mixup expecting "+near+" got "+from.mto);
            }
        } else {
            Log.e(Tag, "session mixup "+from.session+" != "+session);
        }

    }

    void makeSocket(String toSend) throws IOException, WebSocketException {
        WebSocketFactory factory = new WebSocketFactory().setConnectionTimeout(5000);
        webSocket = factory.createSocket("wss://pi.pe/websocket/?finger=" + near);
        webSocket.addListener(new WebSocketAdapter() {
            @Override
            public void onTextMessage(WebSocket websocket, String message) throws Exception {
                Log.d(Tag, "got message " + message);
                incommingMessage(message);
            }
        });
        webSocket.connect();
        webSocket.sendText(toSend);
    }

    void sendOffer(String json) throws IOException, WebSocketException {
        if (webSocket == null) {
            makeSocket(json);
        } else {
            webSocket.sendText(json);
        }
    }

    String mkNonsense() {
        String sense = far + ":" + nonce + ":" + near;
        Log.d(Tag, "My sense " + sense);
        try {
            final MessageDigest d = MessageDigest.getInstance("SHA-256");
            byte enc[] = sense.getBytes();
            d.update(enc, 0, enc.length);
            byte[] result = new byte[d.getDigestLength()];
            d.digest(result, 0, result.length);
            myNonsense = getHex(result);
            Log.d(Tag, "My   Nonsense " + myNonsense);
        } catch (Exception x) {
            Log.e(Tag, "cant digest", x);
        }
        return myNonsense;
    }

    public static String getHex(byte[] in) {
        char cmap[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        StringBuffer ret = new StringBuffer();
        int top = in.length;
        for (int i = 0; i < top; i++) {
            ret.append(cmap[0x0f & (in[i] >>> 4)]);
            ret.append(cmap[in[i] & 0x0f]);
        }
        return ret.toString();
    }

    void makeDcOffer() {
        MediaConstraints sdpMediaConstraints = new MediaConstraints();
        sdpMediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "false"));
        peerConnection.createOffer(new SdpObserver() {
            String offerJson;

            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                PhonoSDPtoJson.Contents con = phonoParser.parseSDP(sessionDescription.description);
                near = con.contents.get(0).fingerprint.print.replace(":", "");
                Log.d(Tag + " offer", "nearprint " + near);
                if (session == null) {
                    session = near + "-" + far + "-" + System.currentTimeMillis();
                }
                String nonsense = (nonce != null) ? mkNonsense():"";
                offerJson = phonoParser.makeMessage(con, far, near, sessionDescription.type.name().toLowerCase(), nonsense, session);
                Log.d(Tag + " offer", "json " + offerJson);
                peerConnection.setLocalDescription(this, sessionDescription);
            }

            @Override
            public void onSetSuccess() {
                Log.d(Tag + " offer", "onSetSuccess ");
                executor.execute(() -> {
                    try {
                        sendOffer(offerJson);
                    } catch (Exception x) {
                        Log.e(Tag, "failed to send offer", x);
                    }
                });
            }

            @Override
            public void onCreateFailure(String s) {
                Log.d(Tag + " offer", "onCreateFailure " + s);

            }

            @Override
            public void onSetFailure(String s) {
                Log.d(Tag + " offer", "onSetFailure " + s);
            }
        }, sdpMediaConstraints);
    }

    void makePeerConnection() {

        VideoEncoderFactory encoderFactory;
        VideoDecoderFactory decoderFactory;
        encoderFactory = new DefaultVideoEncoderFactory(
                    rootEglBase.getEglBaseContext(), true , true);
        decoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());


        VideoCodecInfo [] decs = decoderFactory.getSupportedCodecs();
        boolean hard264 = false;
        for (VideoCodecInfo c:decs)  {
            Log.d(Tag,"Decoder: "+c.name.toUpperCase());

            for (String k:c.params.keySet()){
                String v = c.params.get(k);
                Log.d(Tag, "\t\t"+k + "=" + v);
            }
            if (c.name.toUpperCase().contains("H264")){
                if  (c.params.get("profile-level-id").equals("42e01f")) {
                    hard264 = true;
                    break;
                }
            }
        }
        Log.d(Tag , "hard264 =  " + hard264);

        if (!hard264){
            decoderFactory = new SoftwareVideoDecoderFactory();
            encoderFactory = new SoftwareVideoEncoderFactory();
        }
        /*if (options == null){
            options = new PeerConnectionFactory.Options();
        }*/
        if (factory == null){
            factory = PeerConnectionFactory.builder()
                //.setOptions(options)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();
        }

        if (iceServers == null) {
            makeIceServers();
        }

        if (certificate == null) {
            makeCertificate();
        }
        if (pcObserver == null) {
            makePcO();
        }

        queuedRemoteCandidates = new ArrayList<>();
        PeerConnection.RTCConfiguration rtcConfig =
                new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        rtcConfig.certificate = certificate;
        peerConnection = factory.createPeerConnection(rtcConfig, pcObserver);
        //peerConnection.getCertificate();
        DataChannel.Init init = new DataChannel.Init();
        videorelay = peerConnection.createDataChannel("videorelay", init);

        videorelay.registerObserver(new DataChannel.Observer() {
            String LTag = Tag+".videorelay";
            @Override
            public void onBufferedAmountChange(long l) {
                Log.d(LTag , "buffered amount " + l);
            }

            @Override
            public void onStateChange() {
                Log.d(LTag , videorelay.state().name());
                if (videorelay.state() == DataChannel.State.OPEN){
                    sendString(videorelay,"{\"type\":\"upgrade\",\"time\":\""+System.currentTimeMillis()+"\"}");
                }
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {

                int len  = buffer.data.remaining();
                byte []s = new byte[len];
                buffer.data.get(s);
                String mess = new String(s);

                Log.d(LTag, videorelay.label() + " <-- "+ mess);
                PhonoSDPtoJson.Message me = phonoParser.makeMessageFromJson(mess);
                if (me.mtype.equalsIgnoreCase("offer")) {
                    SessionDescription rd = peerConnection.getRemoteDescription();
                    if (me.info != null) {
                        ArrayList <Patch> patches = Patch.videopatch(me);
                        String sdp = phonoParser.patch(rd.description, patches);
                        Log.d(LTag,"patched SDP is :"+sdp);
                        SessionDescription nrd = new SessionDescription(SessionDescription.Type.OFFER, sdp);
                        executor.execute(() -> {
                            setNewOffer(nrd);
                        });
                    }
                }
                if (me.mtype.equals("ok")){
                    // do video here....
                }
            }
        });

        // Set INFO libjingle logging.
        // NOTE: this _must_ happen while |factory| is alive!
        Logging.enableLogToDebugOutput(Logging.Severity.LS_INFO);
    }



    private void setNewOffer(SessionDescription nrd) {
        Log.d(Tag,"going to set new remote description ");
        peerConnection.setRemoteDescription(new SdpObserver() {
                                                @Override
                                                public void onCreateSuccess(SessionDescription sessionDescription) {

                                                }

                                                @Override
                                                public void onSetSuccess() {
                                                    Log.d(Tag,"set new remote description ");
                                                    createUpgradeAnswer();
                                                }

                                                @Override
                                                public void onCreateFailure(String s) {

                                                }

                                                @Override
                                                public void onSetFailure(String s) {
                                                    Log.d(Tag,"failed to set new remote description "+s);
                                                }
                                            },
                nrd);
    }

    private void createUpgradeAnswer() {
        MediaConstraints sdpMediaConstraints = new MediaConstraints();
        sdpMediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "true"));
        peerConnection.createAnswer(new SdpObserver(){
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d(Tag,"created upgrade answer "+sessionDescription.description);
                executor.execute(() -> {
                    peerConnection.setLocalDescription(this,sessionDescription);
                });
            }

            @Override
            public void onSetSuccess() {
                Log.d(Tag,"set upgrade answer");
                executor.execute(() -> {
                    sendUpgradeAnswer();
                });
            }

            @Override
            public void onCreateFailure(String s) {
                Log.d(Tag,"Failed to create upgrade answer "+s);
            }

            @Override
            public void onSetFailure(String s) {
                Log.d(Tag,"Failed to set upgrade answer "+s);
            }
        },sdpMediaConstraints);
    }

    private void sendUpgradeAnswer() {
        SessionDescription desc = peerConnection.getLocalDescription();
        String mess = "{\"type\":\""+ desc.type.name().toLowerCase()+"\", \"sdp\":\"dummy\", \"tick\":\""+ System.currentTimeMillis()+"\"}";
        sendString(videorelay,mess);
    }

    void sendString(DataChannel channel, String mess){
        Charset charset =Charset.defaultCharset();

        ByteBuffer bb = ByteBuffer.wrap(mess.getBytes(charset));
        DataChannel.Buffer buffer = new DataChannel.Buffer(bb,false);
        Log.d(Tag,channel.label()+" --> "+mess);
        channel.send(buffer);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();
        Log.d(Tag,"intent.data ="+data);
        if (data != null) {
            String frag = data.getFragment();
            String bits[] = frag.split(":");
            if (bits.length == 2) {
                far = bits[0];
                nonce = bits[1];
            }
        }
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

        if (far == null){
            Log.d(Tag,"no QR passed in - lets see if it's in a store....");

            far = sharedPref.getString("far",null);
            if (far == null) {
                Log.d(Tag,"no far - nothing in store....");
            } else {
                Log.d(Tag,"far loaded from shared prefs");
                nonce ="";
            }
        } else {
            SharedPreferences.Editor editor = sharedPref.edit();
            Log.d(Tag,"far saved to shared prefs");
            editor.putString("far",far);
            editor.commit();
        }
        Log.d(Tag,"nonce ="+nonce+" far="+far);

        phonoParser = new PhonoSDPtoJson();
        setContentView(R.layout.activity_main);
        Context appContext = this.getApplicationContext();


        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(appContext)
                        .setFieldTrials("false")
                        .setEnableInternalTracer(true)
                        .createInitializationOptions());

        rootEglBase = EglBase.create();

        PeerConnectionFactory.startInternalTracingCapture(
                Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                        + "webrtc-trace.txt");
        if (far != null) {
            executor.execute(() -> {
                makePeerConnection();
            });
        } else {
            Log.d(Tag,"no friend");
        }
    }
}
