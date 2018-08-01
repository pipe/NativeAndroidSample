package pi.pe.nativeandroidsample;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class PhonoSDPtoJsonUnitTest {

    String chromeVideo = "v=0\r\no=- 2242705449 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\na=group:BUNDLE audio video\r\na=msid-semantic: WMS mXFROJeVMQxDhCFH34Yukxots985y812wGPJ\r\nm=audio 49548 RTP/SAVPF 111 103 104 0 8 107 106 105 13 126\r\nc=IN IP4 192.67.4.11\r\na=rtcp:49548 IN IP4 192.67.4.11\r\na=candidate:521808905 1 udp 2113937151 192.67.4.11 49548 typ host generation 0\r\na=candidate:521808905 2 udp 2113937151 192.67.4.11 49548 typ host generation 0\r\na=ice-ufrag:rl/PIMG6Pd1h6Ymp\r\na=ice-pwd:jsymMG3rh3Fq1vK83jHyQVtt\r\na=ice-options:google-ice\r\na=fingerprint:sha-256 C0:F7:9C:63:AC:84:62:E9:0D:F5:3B:D9:F8:7E:53:29:E2:1F:44:41:84:D1:B6:D7:48:39:A5:64:1F:E7:B4:E4\r\na=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\r\na=sendrecv\r\na=mid:audio\r\na=rtcp-mux\r\na=crypto:1 AES_CM_128_HMAC_SHA1_80 inline:f4OO7dHJbCQsjecAC+0TFp6g6KBXyOub6yBmx+Xx\r\na=rtpmap:111 opus/48000/2\r\na=fmtp:111 minptime=10\r\na=rtpmap:103 ISAC/16000\r\na=rtpmap:104 ISAC/32000\r\na=rtpmap:0 PCMU/8000\r\na=rtpmap:8 PCMA/8000\r\na=rtpmap:107 CN/48000\r\na=rtpmap:106 CN/32000\r\na=rtpmap:105 CN/16000\r\na=rtpmap:13 CN/8000\r\na=rtpmap:126 telephone-event/8000\r\na=maxptime:60\r\na=ssrc:3666452233 cname:XsXQR1VfOels9+3s\r\na=ssrc:3666452233 msid:mXFROJeVMQxDhCFH34Yukxots985y812wGPJ mXFROJeVMQxDhCFH34Yukxots985y812wGPJa0\r\na=ssrc:3666452233 mslabel:mXFROJeVMQxDhCFH34Yukxots985y812wGPJ\r\na=ssrc:3666452233 label:mXFROJeVMQxDhCFH34Yukxots985y812wGPJa0\r\nm=video 49548 RTP/SAVPF 100 116 117\r\nc=IN IP4 192.67.4.11\r\na=rtcp:49548 IN IP4 192.67.4.11\r\na=candidate:521808905 1 udp 2113937151 192.67.4.11 49548 typ host generation 0\r\na=candidate:521808905 2 udp 2113937151 192.67.4.11 49548 typ host generation 0\r\na=ice-ufrag:rl/PIMG6Pd1h6Ymp\r\na=ice-pwd:jsymMG3rh3Fq1vK83jHyQVtt\r\na=ice-options:google-ice\r\na=fingerprint:sha-256 C0:F7:9C:63:AC:84:62:E9:0D:F5:3B:D9:F8:7E:53:29:E2:1F:44:41:84:D1:B6:D7:48:39:A5:64:1F:E7:B4:E4\r\na=extmap:2 urn:ietf:params:rtp-hdrext:toffset\r\na=sendrecv\r\na=mid:video\r\na=rtcp-mux\r\na=crypto:1 AES_CM_128_HMAC_SHA1_80 inline:f4OO7dHJbCQsjecAC+0TFp6g6KBXyOub6yBmx+Xx\r\na=rtpmap:100 VP8/90000\r\na=rtcp-fb:100 ccm fir\r\na=rtcp-fb:100 nack \r\na=rtpmap:116 red/90000\r\na=rtpmap:117 ulpfec/90000\r\na=ssrc:3255638847 cname:XsXQR1VfOels9+3s\r\na=ssrc:3255638847 msid:mXFROJeVMQxDhCFH34Yukxots985y812wGPJ mXFROJeVMQxDhCFH34Yukxots985y812wGPJv0\r\na=ssrc:3255638847 mslabel:mXFROJeVMQxDhCFH34Yukxots985y812wGPJ\r\na=ssrc:3255638847 label:mXFROJeVMQxDhCFH34Yukxots985y812wGPJv0\r\n";
    String candidateSdp = "a=candidate:1 1 udp 123 192.168.157.40 40877 typ host name rtp network_name en0 username root password mysecret generation 0";
    String json ="{'to':'7F5574EFC53A9D022E9FE553691579007421996A4E202944FB5CB43067AE5F0C','from':'BC9D96C47964E7F60B1027342B023EA374A2E6C5B8581A5B235131103732749E','type':'answer','session':'7F5574EFC53A9D022E9FE553691579007421996A4E202944FB5CB43067AE5F0C-BC9D96C47964E7F60B1027342B023EA374A2E6C5B8581A5B235131103732749E-1532680329052','sdp':{'contents':[{'candidates':[],'codecs':[],'ice':{'ufrag':'1g6ijvlht6e06','pwd':'3vjkad3la80ivkf8aetlgnfnif'},'media':{'type':'application','port':'1','proto':'DTLS/SCTP','sctpmap':['5000'],'pts':['5000']},'connection':{'nettype':'IN','addrtype':'IP4','address':'0.0.0.0'},'fingerprint':{'hash':'sha-256','print':'BC:9D:96:C4:79:64:E7:F6:0B:10:27:34:2B:02:3E:A3:74:A2:E6:C5:B8:58:1A:5B:23:51:31:10:37:32:74:9E','required':'1'},'mid':'data','setup':'passive','sctpmap':[{'port':5000,'app':'webrtc-datachannel','count':256}]}],'session':{'username':'-','id':'4648475892259889561','ver':'2','nettype':'IN','addrtype':'IP4','address':'127.0.0.1'},'group':{'type':'BUNDLE','contents':['data']}}}";

    String upgrade = "";

    public PhonoSDPtoJsonUnitTest() {

    }

    @Test
    public void parseTest() {
        PhonoSDPtoJson parser = new PhonoSDPtoJson();
        PhonoSDPtoJson.Contents c = parser.parseSDP(chromeVideo);
        String json = c.toJson();
        System.out.println(json);
        assert (json.length() > 0);
        PhonoSDPtoJson.Contents c2 = parser.makeContentsFromJson(json);
        System.out.println(c2.contents.size());
        assert c2.contents.size() == 2;
        String sdp2 = c2.toSDP();
        System.out.println(sdp2);
        assert (sdp2.length() > chromeVideo.length() / 2);
    }

    @Test
    public void candidateTest() {
        PhonoSDPtoJson parser = new PhonoSDPtoJson();
        String cjson = parser.parseCandidate(candidateSdp);
        System.out.println(cjson);
        assert (cjson.length() > 0);
        String c2 = parser.buildCandidate(cjson);
        System.out.println(c2);
        assert (c2.length() > candidateSdp.length() / 2);

    }

    @Test
    public void jsonTest() {
        PhonoSDPtoJson parser = new PhonoSDPtoJson();
        PhonoSDPtoJson.Message m = parser.makeMessageFromJson(json);
        assert (m.mtype.equals("answer"));
        assert (m.sdp.contents.size() ==1);
        assert (m.sdp.contents.get(0).setup.equals("passive"));
        assert (m.sdp.group.gtype.equals("BUNDLE"));
        String sdp = m.sdp.toSDP();
        assert (sdp.length() >0);
    }

   /* @Test
    public void upgradeTest() {
        PhonoSDPtoJson parser = new PhonoSDPtoJson();
        PhonoSDPtoJson.Message m = parser.makeMessageFromJson(upgrade);
        assert (m.mtype.equals("offer"));
        assert (m.info != null);
    }*/
}