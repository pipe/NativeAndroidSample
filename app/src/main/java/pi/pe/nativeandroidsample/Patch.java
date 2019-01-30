package pi.pe.nativeandroidsample;

import java.util.ArrayList;
import java.util.Arrays;

public class Patch {
    String action;
    String at;
    String line;
    Integer field;
    ArrayList<String> lines;

    Patch(String action, String at, String line) {
        this.action = action;
        this.at = at;
        this.line = line;
    }

    Patch() {

    }

    static ArrayList<Patch> avpatch(PhonoSDPtoJson.Message mess) {
        ArrayList<Patch> patches = new ArrayList<>();
        // add an audio and video mids
        Patch p = new Patch();
        p.action = "increment";
        p.at = "o=-";
        p.field = 2;
        patches.add(p);
        patches.add(new Patch("replace", "a=group:BUNDLE", "a=group:BUNDLE " + mess.vinfo.datamid + " video audio"));
        patches.add(new Patch("replace", "m=application", "m=application 9 DTLS/SCTP 5000"));

        p = new Patch("append", "end", null);
        String vlines[] = {
                "m=video 9 UDP/TLS/RTP/SAVPF " + mess.vinfo.vtype,
                "a=mid:video",
                "a=sendonly",
                "a=rtcp-mux",
                "a=rtpmap:" + mess.vinfo.vtype + " " + mess.vinfo.codec,
                "a=fmtp:" + mess.vinfo.vtype + " packetization-mode=1;profile-level-id=42e01f",
                "a=ssrc:" + mess.vinfo.csrc + " cname:drone",
                "a=ssrc:" + mess.vinfo.csrc + " mslabel:" + mess.vinfo.msid,
                "a=ssrc:" + mess.vinfo.csrc + " label:" + mess.vinfo.appdata,
                "a=ssrc:" + mess.vinfo.csrc + " msid:" + mess.vinfo.msid + " " + mess.vinfo.appdata,
        };
        p.lines = new ArrayList(Arrays.asList(vlines));
        patches.add(p);

        patches.add(new Patch("duplicate", "a=mid:video", "a=fingerprint:"));
        patches.add(new Patch("duplicate", "a=mid:video", "a=ice-ufrag:"));
        patches.add(new Patch("duplicate", "a=mid:video", "a=ice-pwd:"));
        patches.add(new Patch("duplicate", "a=mid:video", "a=setup:"));
        patches.add(new Patch("duplicate", "a=mid:video", "c=IN"));

        p = new Patch("append", "end", null);
        String alines[] = {
                "m=audio 9 UDP/TLS/RTP/SAVP " + mess.ainfo.atype,
                "a=mid:audio",
                "a=sendrcv",
                "a=rtcp-mux",
                "a=rtpmap:" + mess.ainfo.atype + " " + mess.ainfo.codec,
                "a=ssrc:" + mess.ainfo.csrc + " cname:remoteaudio",
                "a=ssrc:" + mess.ainfo.csrc + " mslabel:" + mess.ainfo.msid,
                "a=ssrc:" + mess.ainfo.csrc + " label:" + mess.ainfo.appdata,
                "a=ssrc:" + mess.ainfo.csrc + " msid:" + mess.ainfo.msid + " " + mess.ainfo.appdata,
        };
        p.lines = new ArrayList(Arrays.asList(alines));
        patches.add(p);

        patches.add(new Patch("duplicate", "a=mid:audio", "a=fingerprint:"));
        patches.add(new Patch("duplicate", "a=mid:audio", "a=ice-ufrag:"));
        patches.add(new Patch("duplicate", "a=mid:audio", "a=ice-pwd:"));
        patches.add(new Patch("duplicate", "a=mid:audio", "a=setup:"));
        patches.add(new Patch("duplicate", "a=mid:audio", "c=IN"));

        return patches;
    }

    static ArrayList<Patch> offerpatch() {
        ArrayList<Patch> patches = new ArrayList<>();
        Patch p = new Patch();
        p.action = "increment";
        p.at = "o=-";
        p.field = 2;
        patches.add(p);
        return patches;
    }

    static ArrayList<Patch> videopatch(PhonoSDPtoJson.Message mess) {
        PhonoSDPtoJson.Info info = mess.vinfo;
        ArrayList<Patch> patches = new ArrayList<>();
        // add video mid
        Patch p = new Patch();
        p.action = "increment";
        p.at = "o=-";
        p.field = 2;
        patches.add(p);
        patches.add(new Patch("replace", "a=group:BUNDLE", "a=group:BUNDLE " + info.datamid + " video"));
        patches.add(new Patch("replace", "m=application", "m=application 9 DTLS/SCTP 5000"));

        p = new Patch("append", "end", null);
        String lines[] = {
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

        patches.add(new Patch("duplicate", "a=mid:video", "a=fingerprint:"));
        patches.add(new Patch("duplicate", "a=mid:video", "a=ice-ufrag:"));
        patches.add(new Patch("duplicate", "a=mid:video", "a=ice-pwd:"));
        patches.add(new Patch("duplicate", "a=mid:video", "a=setup:"));
        patches.add(new Patch("duplicate", "a=mid:video", "c=IN"));
        return patches;

    }

}
