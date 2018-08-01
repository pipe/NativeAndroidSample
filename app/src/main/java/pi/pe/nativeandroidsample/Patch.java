package pi.pe.nativeandroidsample;

import java.util.ArrayList;
import java.util.Arrays;

public class Patch{
    String action;
    String at;
    String line;
    Integer field;
    ArrayList<String> lines;

    Patch(String action,String at, String line){
        this.action = action;
        this.at = at;
        this.line = line;
    }
    Patch(){

    }

    static ArrayList <Patch> videopatch(PhonoSDPtoJson.Message mess) {
        PhonoSDPtoJson.Info info = mess.info;
        ArrayList <Patch> patches = new ArrayList<>();
        // add an audio mid
        Patch p = new Patch();
            p.action= "increment";
            p.at= "o=-";
            p.field = 2;
        patches.add(p);
        patches.add( new Patch( "replace","a=group:BUNDLE", "a=group:BUNDLE " + info.datamid + " video"));
        patches.add( new Patch( "replace", "m=application", "m=application 9 DTLS/SCTP 5000"));

        p= new Patch("append","end", null);
        String lines [] = {
                "m=video 9 RTP/SAVPF " + info.vtype,
                "a=mid:video",
                "a=sendonly",
                "a=rtcp-mux",
                "a=rtpmap:" + info.vtype + " " + info.codec,
                "a=fmtp:" + info.vtype + " packetization-mode=1;profile-level-id=42e01f",
                "a=ssrc:" + info.csrc + " cname:drone",
                "a=ssrc:" + info.csrc + " mslabel:" + info.msid,
                "a=ssrc:" + info.csrc + " label:" + info.appdata,
                "a=ssrc:" + info.csrc + " msid:" + info.msid + " " + info.appdata,
        };
        p.lines = new ArrayList(Arrays.asList(lines));
        patches.add(p);

        patches.add( new Patch( "duplicate","a=mid:video","a=fingerprint:"));
        patches.add( new Patch( "duplicate","a=mid:video","a=ice-ufrag:"));
        patches.add( new Patch( "duplicate","a=mid:video","a=ice-pwd:"));
        patches.add( new Patch(  "duplicate","a=mid:video","a=setup:"));
        patches.add( new Patch("duplicate","a=mid:video","c=IN"));
        return  patches;

    }

}
