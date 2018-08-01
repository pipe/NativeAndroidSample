package pi.pe.nativeandroidsample;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;


import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.PeerConnection;
import org.webrtc.RTCCertificate;
import org.webrtc.RendererCommon;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoTrack;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.security.MessageDigest;


public class MainActivity extends AppCompatActivity {

    private static String Tag = "pi.pe.nativeandroidsample.MainActivity";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private PeerConnection peerConnection;
    private ArrayList queuedRemoteCandidates;
    private PeerConnectionFactory factory;
    private PeerConnectionFactory.Options options;
    private List<PeerConnection.IceServer> iceServers;
    private PeerConnection.Observer pcObserver;
    private RTCCertificate certificate;
    private PhonoSDPtoJson phonoParser;
    private String near;
    private String far = null;
    private String nonce = null;
    private String session;
    private WebSocket webSocket;
    private String myNonsense = "";
    private  DataChannel videorelay;

    // just to be clear, this is a _BAD_ example - this keypair should:
    // a) be unique - one per device
    // b) be generated once at app install time by the webRTC framework
    // c) saved in an encrypted _local_ keystore
    // d) only retrevied when needed and mem wiped asap
    // e) definitely not checked into a public github repo.

    void makeCertificate() {

        certificate = new RTCCertificate(
                "-----BEGIN PRIVATE KEY-----\n" +
                        "MIIEwAIBADANBgkqhkiG9w0BAQEFAASCBKowggSmAgEAAoIBAQDjcPXFR75C4hxi\n" +
                        "LVGlT2XMUiWXr6DXaWQvTUGgqEV/tlLkSojCq23DlUaGY9kPAvvDN9PUfj/YifYe\n" +
                        "jDtOkAWwLftdPCi2rWNpvLVUw15oGP0N04DEohkLq/wz73+/Af3DvdLGuOxEX4in\n" +
                        "IwEA4UG+iRrNNTaTmIwXhwd09pjW7Catxxr1nMFz4rcPIYOKfAQd3ekxuWkBiY0Q\n" +
                        "d7uqY8+dsBbrmPVTilwWzcYzCmdAPmql6Tvn4O0E15FPS60y5auN1kk3QV4yR5qE\n" +
                        "6POePnhyDcqJQ3wE8uZ+4UxR0oBECNw4OAYh6/t8sA/y0BX+zdTmoGb6BSNC1dCm\n" +
                        "Kmzx3u87AgMBAAECggEBAIWiupWW6ODNkFXTQPS7qRmbbsEojX3kS9xLLXNjS6qV\n" +
                        "izDd5mtTZKQVkqGmC0R5wUncBJgHMiZeMYGTbclkcCMrcVU/4sArMo1PNtA+Frtd\n" +
                        "a1pzWmauw76K6B3v7ARj/CHF6BGhWBl4dIeX1qAYupNkZZ4LVSz15eJxEV2VAwrM\n" +
                        "DLaJ/cuwJU6TRW5SDSzs3L4GJJHDInw0VU9Z4io11mkAc+ao/vE/YiXctg7/m6NG\n" +
                        "5uj8VnglO0Tnshu/QEulMEvRPJsa6WRhKDCMQdSJJxsD4XoA2Xk3JO3SqfhbFCDQ\n" +
                        "DYZEeTdPYwpV/f5ulgt5JUJ5P/Lnch6O6S0ypx2TfFECgYEA/JtK0zoIKJ4vo3xT\n" +
                        "SzlxcSc2/x2zcNpnDviZX15uT7v2+Y6YnRj91LZMDh+BftjcSrJ4DHvZUh4ABvmy\n" +
                        "4NKkiLxk/zemPKv8jg6XF3Q7MXuOsx1NjamxlRuKWYUphbE1kEUMyahzp8/QYRKp\n" +
                        "3zoGSyGLLsztog8WAJfhrWKoAZMCgYEA5n8f7bt5d0pDojSVujiuqLk+b1Papf4t\n" +
                        "Pu5sPC+BQmF+obNQmokguEyMyDaZbJpYKHKxTAF7X4GeGa9v/eRc88Tp9OgYPuTm\n" +
                        "MwDgmz1fT0/T3znKp7gy3txYXalbrXnLPlh8kP5m+sIbkvNYYmFNUgwzie8YY4Sm\n" +
                        "enk6MYOChLkCgYEAr180zH6eiWyBEFRRE4mW24LpKKa7HF9Ua01mVZKerRaG+Wzp\n" +
                        "QS/HkbTaCngPFDyEfAt5Utls4BjZ1f3nFTTIa/G3gIRnEfopRYqVlP/p1Im+YVW3\n" +
                        "sOEd27IaE9piIGIOGNIHdb1QRjH9rlchvktvcRuhoGU/mWI12UWYtSIoF6cCgYEA\n" +
                        "pBLb7IMmDKc9i6o45q1QjuQGMIMVQlGzbXeUbic2sMTrujkFaGuycd5z3FUZU3hu\n" +
                        "xqQV1s+7PZnrq8b5RlaXYCLMkzvffm27+5RlTbwbKz7cx6rAujDhbUiWUl2q18q5\n" +
                        "xXCBg0jEjz07GZjFUoqG2p+61DXxO24Cf23p4bMGZmkCgYEAo/5jU0T4hHTTJJ1i\n" +
                        "NkkjVZoFUIbcnJOZPssdbIGlh5S2v/bWlt9UanrENWIlxvyJHGdYkt2GN0uQMd7m\n" +
                        "g7tC9reu62rFf9AcOPAx7KjRf/Xph8QZblv+Z/85kE4bv7J/g8aVr2tdZFyhtNPo\n" +
                        "Qb1L+4/ukPTgpRSquhpDtD0VrPM=\n" +
                        "-----END PRIVATE KEY-----\n",
                "-----BEGIN CERTIFICATE-----\n" +
                        "MIICnjCCAYYCCQCl8bejKUVbHTANBgkqhkiG9w0BAQsFADARMQ8wDQYDVQQDDAZ8\n" +
                        "cGlwZXwwHhcNMTgwNzI2MjAxMjAwWhcNMjgwNzIzMjAxMjAwWjARMQ8wDQYDVQQD\n" +
                        "DAZ8cGlwZXwwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDjcPXFR75C\n" +
                        "4hxiLVGlT2XMUiWXr6DXaWQvTUGgqEV/tlLkSojCq23DlUaGY9kPAvvDN9PUfj/Y\n" +
                        "ifYejDtOkAWwLftdPCi2rWNpvLVUw15oGP0N04DEohkLq/wz73+/Af3DvdLGuOxE\n" +
                        "X4inIwEA4UG+iRrNNTaTmIwXhwd09pjW7Catxxr1nMFz4rcPIYOKfAQd3ekxuWkB\n" +
                        "iY0Qd7uqY8+dsBbrmPVTilwWzcYzCmdAPmql6Tvn4O0E15FPS60y5auN1kk3QV4y\n" +
                        "R5qE6POePnhyDcqJQ3wE8uZ+4UxR0oBECNw4OAYh6/t8sA/y0BX+zdTmoGb6BSNC\n" +
                        "1dCmKmzx3u87AgMBAAEwDQYJKoZIhvcNAQELBQADggEBAD+1dVjEIwLSOKMdgxbH\n" +
                        "19REhClPP+sS6rv+W5MP7vGUFmysYEgh6vzGJSLJLXrL1Z9JREe9yqb/W2/iEC3x\n" +
                        "DPQJtOLMZa3qWW/05AA5EKoxx/TotbENakWkg5PAmejTvUfN4fIC3vlCKGvZ67On\n" +
                        "6U3a/AlJudzBtz9k8n+ZSTDGyJNjY5CmT992IqILMp4iMtvxHRVjGGTNljqIaqZ0\n" +
                        "5rtMn94rdNeEWrWlkps8MJr5nqTdJlS7ta/g8mUHjjBh1XLjSladPDUpdeuS3ANc\n" +
                        "xgEgS6OR2kvepBXDjT+myVqWhxICey0xVi7WmK56N/PmZaXO5rv0wfIBFwT+3Ag0\n" +
                        "Ka8=\n" +
                        "-----END CERTIFICATE-----\n");
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
        SurfaceViewRenderer svr = (SurfaceViewRenderer) findViewById(R.id.surfaceView);
        EglBase rootEglBase = EglBase.create();

        svr.init(rootEglBase.getEglBaseContext(), null);
        svr.setEnableHardwareScaler(true);
        svr.setMirror(false);
        svr.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
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
                Log.e(Tag, "to mixup");
            }
        } else {
            Log.e(Tag, "session mixup");
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
                session = near + "-" + far + "-" + System.currentTimeMillis();
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

        if (factory == null) {
            factory = PeerConnectionFactory.builder()
                    .setOptions(options)
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
        peerConnection = factory.createPeerConnection(rtcConfig, pcObserver, certificate.privateKey, certificate.certificate);
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

                Log.d(LTag, videorelay.label() + " --> "+ mess);
                PhonoSDPtoJson.Message me = phonoParser.makeMessageFromJson(mess);
                if (me.mtype.equalsIgnoreCase("offer")) {
                    SessionDescription rd = peerConnection.getRemoteDescription();
                    if (me.info != null) {
                        ArrayList <Patch> patches = Patch.videopatch(me);
                        String sdp = phonoParser.patch(rd.description, patches);
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
                Log.d(Tag,"created upgrade answer");
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
            far = sharedPref.getString("far",null);
        } else {
            SharedPreferences.Editor editor = sharedPref.edit();
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
