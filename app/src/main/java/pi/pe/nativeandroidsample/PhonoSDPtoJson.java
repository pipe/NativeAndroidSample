package pi.pe.nativeandroidsample;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;

/* derived from Phono with original license quoted here */
/*!
 * Copyright 2013 Voxeo Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
public class PhonoSDPtoJson {

    static final String Tag = "PhonoSDPtoJson";

    static class Line {

        @SerializedName("type")
        public String ltype;
        public String contents;

        Line(String line) {
            String[] bits = line.split("=");
            ltype = bits[0];
            contents = bits[1];
        }
    }

    static class Attribute {

        public String key;
        public String[] params;

        Attribute(String attribute) {
            String[] s1 = attribute.split(":");
            key = s1[0];
            params = attribute.substring(attribute.indexOf(":") + 1).split(" ");
        }
    }

    static class Media {

        @SerializedName("type")

        public String mtype;
        public String port;
        public String proto;
        public String[] pts;

        Media(String media) {
            String[] s1 = media.split(" ");
            mtype = s1[0];
            port = s1[1];
            proto = s1[2];
            pts = media.substring((s1[0] + s1[1] + s1[2]).length() + 3).split(" ");
        }
    }

    static class Oline {

        public String username;
        @SerializedName("id")
        public String oid;
        public String ver, nettype, addrtype, address;

        Oline(String oline) {
            String[] s1 = oline.split(" ");
            username = s1[0];
            oid = s1[1];
            ver = s1[2];
            nettype = s1[3];
            addrtype = s1[4];
            address = s1[5];
        }
    }

    static class Cline {

        public String nettype, addrtype, address;

        Cline(String cline) {
            String[] s1 = cline.split(" ");
            nettype = s1[0];
            addrtype = s1[1];
            address = s1[2];
        }
    }

    static class Ice {
        String ufrag;
        String pwd;
        String options;
    }

    static class Candidate {

        public String foundation,
                component,
                protocol,
                priority,
                ip,
                port;
        @SerializedName("type")
        public String ctype;
        public String generation, username, password, raddr, rport;

        Candidate(String[] params) {
            foundation = params[0];
            component = params[1];
            protocol = params[2].toLowerCase();
            priority = params[3];
            ip = params[4];
            port = params[5];
            int index = 6;
            while (index + 1 <= params.length) {
                String val = params[index + 1];

                switch (params[index]) {
                    case "typ":
                        ctype = val;
                        break;
                    case "generation":
                        generation = val;
                        break;
                    case "username":
                        username = val;
                        break;
                    case "password":
                        password = val;
                        break;
                    case "raddr":
                        raddr = val;
                        break;
                    case "rport":
                        rport = val;
                        break;
                }
                index += 2;
            }
        }

        String toSdp(Ice iceObj) {
            Candidate c = this;
            String sdp = "candidate:" + c.foundation + " " +
                    c.component + " " +
                    c.protocol.toLowerCase() + " " +
                    c.priority + " " +
                    c.ip + " " +
                    c.port;
            if (c.ctype != null)
                sdp = sdp + " typ " + c.ctype;
            if (c.component.equalsIgnoreCase("1"))
                sdp = sdp + " name rtp";
            if (c.component.equalsIgnoreCase("2"))
                sdp = sdp + " name rtcp";
            sdp = sdp + " network_name en0";
            if ((c.username != null) && (c.password != null)) {
                sdp = sdp + " username " + c.username;
                sdp = sdp + " password " + c.password;
                if (iceObj != null) {
                    if (iceObj.ufrag == null)
                        iceObj.ufrag = c.username;
                    if (iceObj.pwd == null)
                        iceObj.pwd = c.username;
                }
            } else if (iceObj != null) {
                if (iceObj.ufrag != null)
                    sdp = sdp + " username " + iceObj.ufrag;
                if (iceObj.pwd != null)
                    sdp = sdp + " password " + iceObj.pwd;
            }
            if (c.generation != null)
                sdp = sdp + " generation " + c.generation;
            if (c.raddr != null)
                sdp = sdp + " raddr " + c.raddr;
            if (c.rport != null)
                sdp = sdp + " rport " + c.rport;
            sdp = sdp + "\r\n";
            return sdp;
        }
    }

    static class Rtcp {

        public String port, nettype, addrtype, address;

        Rtcp(String[] params) {
            port = params[0];
            nettype = params[1];
            addrtype = params[2];
            address = params[3];
        }
    }

    static class Crypto {

        public String tag;
        @SerializedName("crypto-suite")
        public String crypto_suite;
        @SerializedName("key-params")
        public String key_params;

        Crypto(String[] params) {
            tag = params[0];
            crypto_suite = params[1];
            key_params = params[2];
        }

        String toSdp() {
            return "a=crypto:" + this.tag + " " + this.crypto_suite + " " + this.key_params + "\r\n";
        }
    }

    class Fingerprint {

        public String hash;
        public String print;
        public String required;

        Fingerprint(String[] params) {
            hash = params[0];
            print = params[1];
            required = "1";
        }

        String toSdp() {
            return "a=fingerprint:" + this.hash + " " + this.print + "\r\n";
        }

    }

    static class Codec {
        String fmtp;
        ArrayList<String[]> rtcpfbs;

        @SerializedName("id")
        public String cid;
        @SerializedName("name")
        public String cname;
        public String clockrate;
        public String channels;
        public String ptime;

        Codec(String[] params) {
            String[] bits = params[1].split("/");
            cid = params[0];
            cname = bits[0];
            clockrate = bits[1];
            if (bits.length > 2) {
                channels = bits[2];
            }
        }

        String toSdp() {
            String sdp = "a=rtpmap:" + this.cid + " " + this.cname + "/" + this.clockrate;
            if (this.channels != null) {
                sdp += "/" + this.channels;
            }
            sdp += "\r\n";
            if (this.ptime != null) {
                sdp += "a=ptime:" + this.ptime;
                sdp += "\r\n";
            }
            if (this.fmtp != null) {
                sdp += "a=fmtp:" + this.cid + " " + this.fmtp + "\r\n";
            }
            if (this.rtcpfbs != null) {
                for (String[] fbs : rtcpfbs) {
                    sdp += "a=rtcp-fb:" + this.cid;
                    for (String fb : fbs) {
                        sdp += " " + fb;
                    }
                    sdp += "\r\n";
                }
            }
            return sdp;
        }
    }

    class Sctpmap {

        String port, app, count;

        Sctpmap(String[] params) {
            port = params[0];
            app = params[1];
            count = params[2];
        }

        String toSdp() {
            return "a=sctpmap:" + this.port + " " + this.app + " " + this.count + "\r\n";
        }
    }

    class Ssrc {
        String ssrc;
        String cname;
        String mslabel;
        String label;
        String msid;
        String msid3;

        Ssrc(String[] params) {
            ssrc = params[0];
            String[] kv = params[1].split(":");
            String k = kv[0];
            String v = kv[1];
            switch (k) {
                case "cname":
                    cname = v;
                    break;
                case "mslabel":
                    mslabel = v;
                    break;
                case "label":
                    label = v;
                    break;
                case "msid":
                    msid = v;
                    msid3 = params[2];
                    break;
            }
        }

        String toSdp() {
            String sdp = "";
            if (this.cname != null)
                sdp = "a=ssrc:" + this.ssrc + " " + "cname:" + this.cname + "\r\n";
            if (this.mslabel != null)
                sdp = "a=ssrc:" + this.ssrc + " " + "mslabel:" + this.mslabel + "\r\n";
            if (this.label != null)
                sdp = "a=ssrc:" + this.ssrc + " " + "label:" + this.label + "\r\n";
            if (this.msid != null)
                sdp = "a=ssrc:" + this.ssrc + " " + "msid:" + this.msid + " " + this.msid3 + "\r\n";
            return sdp;
        }
    }


    class Group {

        @SerializedName("type")
        public String gtype;
        public String[] contents;
        MidSem midSem;

        Group(String[] params) {
            gtype = params[0];
            contents = new String[params.length - 1];
            for (int i = 0; i < contents.length; i++) {
                contents[i] = params[i + 1];
            }
        }
    }

    /*
        class Mid {

            public String mid;

            Mid(String[] params) {
                mid = params[0];
            }
            String toSdp(){
                return "a=mid:"+mid+"\r\n";
            }
        }
    */
    class MidSem {

        public String[] sems;

        MidSem(String[] params) {
            sems = params;
        }
    }

    /*
        class Setup {

            public String setup;

            Setup(String[] params) {
                setup = params[0];
            }
        }
    */
    class ContentsHolder {
        Contents contents;

        ContentsHolder(Contents c) {
            contents = c;
        }
    }

    class Message {
        @SerializedName("to")
        String mto;
        @SerializedName("from")
        String mfrom;
        String nonsense;
        String session;
        Contents sdp;
        String retry;
        @SerializedName("type")
        String mtype;
        Candidate candidate;
        String sdpMLineIndex;
        Info info;
        Info vinfo;
        Info ainfo;
        String time;
    }

    class Sdp {
        PhonoSDPtoJson.Ice ice;
        ArrayList<PhonoSDPtoJson.Candidate> candidates;
        ArrayList<PhonoSDPtoJson.Codec> codecs;
        Media media;
        Cline connection;
        String setup;
        String mid;
        Rtcp rtcp;
        @SerializedName("rtcp-mux")
        Boolean rtcp_mux;
        String direction;
        String recvonly;
        String[] ssrcgroup;
        Fingerprint fingerprint;
        Crypto crypto;
        ArrayList<Sctpmap> sctpmap;
        ArrayList<Ssrc> ssrcs;

        Sdp() {
            candidates = new ArrayList();
            codecs = new ArrayList();
            sctpmap = new ArrayList();
            ssrcs = new ArrayList();
            ice = new PhonoSDPtoJson.Ice();
        }

        String toSdp() {
            String sdp = "";
            sdp += "m=" + this.media.mtype + " " + this.media.port + " " + this.media.proto;
            if (media.pts != null) {
                for (String pts : media.pts) {
                    sdp = sdp + " " + pts;
                }
            }
            sdp = sdp + "\r\n";
            if (this.fingerprint != null) {
                sdp = sdp + this.fingerprint.toSdp();
            }
            if (this.ice != null) {
                sdp = sdp + "a=ice-ufrag:" + ice.ufrag + "\r\n";
                sdp = sdp + "a=ice-pwd:" + ice.pwd + "\r\n";
                if (ice.options != null) {
                    sdp = sdp + "a=ice-options:" + ice.options + "\r\n";
                }
            }

            if (this.connection != null) {
                sdp = sdp + "c=" + this.connection.nettype + " " + this.connection.addrtype + " " +
                        "0.0.0.0\r\n";
                //this.connection.address + "\r\n";
            }

            if (this.mid != null) {
                sdp = sdp + "a=mid:" + mid + "\r\n";
            }

            if (this.setup != null) {
                sdp = sdp + "a=setup:" + this.setup + "\r\n";
            }

            if (this.rtcp != null) {
                sdp = sdp + "a=rtcp:" + this.rtcp.port + " " + this.rtcp.nettype + " " +
                        this.rtcp.addrtype + " " +
                        this.rtcp.address + "\r\n";
            }

            for (Candidate can : this.candidates) {
                sdp = sdp + "a=" + can.toSdp(this.ice);
            }


            if (this.direction != null) {
                if (this.direction == "recvonly") {
                    sdp = sdp + "a=recvonly\r\n";
                } else if (this.direction == "sendonly") {
                    sdp = sdp + "a=sendonly\r\n";
                } else if (this.direction == "none") {
                    sdp = sdp;
                } else {
                    sdp = sdp + "a=sendrecv\r\n";
                }
            }


            if ((this.rtcp_mux != null) && (this.rtcp_mux)) {
                sdp = sdp + "a=rtcp-mux" + "\r\n";
            }

            if (this.crypto != null) {
                sdp = sdp + this.crypto.toSdp();
            }

            for (Codec cdi : this.codecs) {
                sdp = sdp + cdi.toSdp();
            }

            for (Sctpmap sdi : this.sctpmap) {
                sdp += sdi.toSdp();
            }

            if (this.ssrcgroup != null) {
                String gline = "a=ssrc-group:";
                for (String p : this.ssrcgroup) {
                    gline += p + " ";
                }
                sdp += gline.trim() + "\r\n";
            }

            if (this.ssrcs != null) {
                for (Ssrc ssrc : this.ssrcs) {
                    sdp += ssrc.toSdp();
                }
            }
            return sdp;
        }
    }

    String makeMessage(Contents description, String to, String from, String mtype, String nonsense, String session) {
        String ret = null;
        Message mess = new Message();
        mess.sdp = description;
        mess.mtype = mtype;
        mess.session = session;
        mess.mto = to;
        mess.mfrom = from;
        if (nonsense != null) {
            mess.nonsense = nonsense;
        }
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        ret = gson.toJson(mess);
        return ret;
    }

    String makeCandidateMessage(String csdp, String to, String from, int mline, String nonsense, String session) {
        String ret = null;
        Message mess = new Message();
        mess.candidate = new Candidate(csdp.substring(csdp.indexOf(":") + 1).split(" "));
        mess.mtype = "candidate";
        mess.session = session;
        mess.mto = to;
        mess.mfrom = from;
        mess.sdpMLineIndex = "" + mline;
        if (nonsense != null) {
            mess.nonsense = nonsense;
        }
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        ret = gson.toJson(mess);
        return ret;
    }

    Contents parseSDP(String s) {
        return new Contents(s);
    }

    Contents makeContentsFromJson(String json) {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.fromJson(json, Contents.class);
    }

    Message makeMessageFromJson(String json) {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.fromJson(json, Message.class);
    }

    String parseCandidate(String candidateSDP) {
        String ret = "";
        Line line = new PhonoSDPtoJson.Line(candidateSDP);
        if (line.contents != null) {
            Candidate c = new Candidate(line.contents.substring(line.contents.indexOf(":") + 1).split(" "));
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            ret = gson.toJson(c);
        }
        return ret;
    }

    class Info {
        String msid;
        String datamid;
        String vtype;
        String atype;
        String codec;
        String appdata;
        String csrc;
    }

    String patch(String sdpString, ArrayList<Patch> patches) {
        ArrayList<String> sdpLines = new ArrayList();
        sdpLines.addAll(Arrays.asList(sdpString.split("\r\n")));
        for (Patch lpatch : patches) {
            if (lpatch.action.equals("append")) {
                sdpLines.addAll(lpatch.lines);
            } else {
                int where = sdpLines.size() - 1;

                for (int lno = 0; lno < sdpLines.size(); lno++) {
                    String sline = sdpLines.get(lno);
                    if (sline.startsWith(lpatch.at)) {
                        where = lno;
                        break;
                    }
                }
                if (lpatch.action.equals("prepend")) {
                    if (lpatch.line != null) {
                        sdpLines.add(where, lpatch.line);
                    }
                    if (lpatch.lines != null) {
                        sdpLines.addAll(where, lpatch.lines);
                    }
                }
                if (lpatch.action.equals("increment")) {
                    String[] bits = sdpLines.get(where).split(" ");
                    int v = Integer.parseInt(bits[lpatch.field]);
                    v = v + 1;
                    bits[lpatch.field] = "" + v;
                    String line = "";
                    for (String bit : bits) {
                        line += bit + " ";
                    }
                    line = line.trim();
                    sdpLines.set(where, line);
                }
                if (lpatch.action.equals("replace")) {
                    sdpLines.set(where, lpatch.line);
                }
                if (lpatch.action.equals("duplicate")) {
                    String withline = null;
                    for (String sline : sdpLines) {
                        if (sline.startsWith(lpatch.line)) {
                            withline = sline;
                            break;
                        }
                    }
                    if (withline != null) {
                        sdpLines.add(where, withline);
                    }
                }
            }
        }

        String sdp = "";
        for (String l : sdpLines) {
            if (l.length() > 0) {
                sdp += (l + "\r\n");
            }
        }
        return sdp;
    }

    // candidate: json representing the body
    // Return a text string in SDP format
    String buildCandidate(String candidateJson) {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        Candidate c = gson.fromJson(candidateJson, Candidate.class);
        return c.toSdp(null);
    }

    class Contents {
        ArrayList<Sdp> contents;
        Oline session;
        Cline connection;
        Group group;

        String toJson() {
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            return gson.toJson(this);
        }

        String toSDP() {
            // Write some constant stuff
            String sdp =
                    "v=0\r\n";
            if (this.session != null) {
                sdp = sdp + "o=" + session.username + " " + session.oid + " " + session.ver + " " +
                        session.nettype + " " + session.addrtype + " " + session.address + "\r\n";
            } else {
                long id = System.currentTimeMillis();
                int ver = 2;
                sdp = sdp + "o=-" + " 3" + id + " " + ver + " IN IP4 192.67.4.14" + "\r\n"; // does the IP here matter ?!?
            }

            sdp = sdp + "s=-\r\n" +
                    "t=0 0\r\n";

            if (this.connection != null) {
                sdp = sdp + "c=" + connection.nettype + " " + connection.addrtype +
                        " " + connection.address + "\r\n";
            }
            if (this.group != null) {
                sdp = sdp + "a=group:" + group.gtype;
                int ig = 0;
                while (ig + 1 <= group.contents.length) {
                    sdp = sdp + " " + group.contents[ig];
                    ig = ig + 1;
                }
                sdp = sdp + "\r\n";
                if ((group.midSem != null) && (group.midSem.sems != null)) {
                    sdp += "a=msid-semantic:";
                    for (String sem : group.midSem.sems) {
                        sdp = sdp + " " + sem;
                    }
                    sdp = sdp + "\r\n";
                }
            }
            for (Sdp sdpObj : contents) {
                sdp = sdp + sdpObj.toSdp();
            }
            return sdp;

        }

        Contents(String sdpString) {
            this.contents = new ArrayList();
            Sdp sdpObj = new Sdp();
            Sdp sessionSDP = sdpObj;
            // Iterate the lines
            String[] sdpLines = sdpString.split("\r\n");
            for (String sline : sdpLines) {
                PhonoSDPtoJson.Line line = new PhonoSDPtoJson.Line(sline);

                switch (line.ltype) {
                    case "o":
                        this.session = new PhonoSDPtoJson.Oline(line.contents);
                        break;
                    case "m":
                        // New m-line,
                        // create a new content
                        PhonoSDPtoJson.Media media = new PhonoSDPtoJson.Media(line.contents);
                        sdpObj = new Sdp();
                        sdpObj.ice = sessionSDP.ice;
                        if (sessionSDP.fingerprint != null) {
                            sdpObj.fingerprint = sessionSDP.fingerprint;
                        }
                        sdpObj.media = media;
                        this.contents.add(sdpObj);
                        break;
                    case "c":
                        if (sdpObj != null) {
                            sdpObj.connection = new PhonoSDPtoJson.Cline(line.contents);
                        } else {
                            this.connection = new PhonoSDPtoJson.Cline(line.contents);
                        }
                        break;
                    case "a":
                        PhonoSDPtoJson.Attribute a = new PhonoSDPtoJson.Attribute(line.contents);
                        switch (a.key) {
                            case "candidate":
                                PhonoSDPtoJson.Candidate candidate = new PhonoSDPtoJson.Candidate(a.params);
                                sdpObj.candidates.add(candidate);
                                break;
                            case "group":
                                Group group = new Group(a.params);
                                this.group = group;
                                break;
                            case "setup":
                                sdpObj.setup = a.params[0];
                                break;
                            case "mid":
                                sdpObj.mid = a.params[0];
                                break;
                            case "rtcp":
                                sdpObj.rtcp = new Rtcp(a.params);
                                break;
                            case "rtcp-mux":
                                sdpObj.rtcp_mux = true;
                                break;
                            case "fmtp":
                                for (Codec c : sdpObj.codecs) {
                                    if (c.cid == a.params[0]) {
                                        c.fmtp = sline.split(" ")[1];
                                        break;
                                    }
                                }
                                break;
                            case "rtcp-fb":
                                String[] rtcpfb = new String[a.params.length - 1];
                                for (int i = 0; i < rtcpfb.length; i++) {
                                    rtcpfb[i] = a.params[i + 1];
                                }
                                for (Codec codec : sdpObj.codecs) {
                                    if (codec.cid == a.params[0]) {
                                        if (codec.rtcpfbs == null) {
                                            codec.rtcpfbs = new ArrayList();
                                        }
                                        codec.rtcpfbs.add(rtcpfb);
                                        break;
                                    }
                                }
                                break;
                            case "rtpmap":
                                Codec codec = new Codec(a.params);
                                sdpObj.codecs.add(codec);
                                break;
                            case "sendrecv":
                                sdpObj.direction = "sendrecv";
                                break;
                            case "sendonly":
                                sdpObj.direction = "sendonly";
                                break;
                            case "recvonly":
                                sdpObj.recvonly = "recvonly";
                                break;
                            case "ssrc-group":
                                sdpObj.ssrcgroup = a.params;
                                break;
                            case "ssrc":
                                sdpObj.ssrcs.add(new Ssrc(a.params));
                                break;
                            case "fingerprint":
                                sdpObj.fingerprint = new Fingerprint(a.params);
                                break;
                            case "crypto":
                                sdpObj.crypto = new Crypto(a.params);
                                break;
                            case "ice-ufrag":
                                sdpObj.ice.ufrag = a.params[0];
                                break;
                            case "ice-pwd":
                                sdpObj.ice.pwd = a.params[0];
                                break;
                            case "ice-options":
                                sdpObj.ice.options = a.params[0];
                                break;
                            case "sctpmap":
                                sdpObj.sctpmap.add(new Sctpmap(a.params));
                                break;
                            case "msid-semantic":
                                if (this.group != null) {
                                    this.group.midSem = new MidSem(a.params);
                                }
                                break;
                        }
                        break;
                }
            }
        }
    }

}
