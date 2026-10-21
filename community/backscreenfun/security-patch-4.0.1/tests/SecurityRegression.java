package com.amoo.backscreenfun;

import android.content.Context;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class SecurityRegression {
    private static int passed;
    private static final Context CONTEXT = new Context();
    private static String token;
    private static final SecureHttp HTTP = new SecureHttp(new SecureHttp.Backend() {
        public boolean authorized(String candidate) { return Pairing.matches(CONTEXT, candidate); }
        public String status() { return PlaybackService.statusLine(); }
        public String alive() { return "ticks=12 playing=true kind=status"; }
        public void update(String tool, String state, String message, boolean show) {
            PlaybackService.pushStatus(CONTEXT, tool, state, message);
        }
    });
    private static void check(String label, boolean result) {
        if (!result) throw new AssertionError(label);
        passed++;
        System.out.println("PASS " + label);
    }
    private static String repeat(String value, int times) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < times; i++) b.append(value);
        return b.toString();
    }
    private static String request(String method, String path, String credential, String body) {
        return method + " " + path + " HTTP/1.1\r\nHost: localhost\r\n"
                + (credential == null ? "" : "Authorization: Bearer " + credential + "\r\n")
                + (body == null ? "" : "Content-Type: application/x-www-form-urlencoded\r\nContent-Length: "
                    + body.getBytes(StandardCharsets.UTF_8).length + "\r\n")
                + "\r\n" + (body == null ? "" : body);
    }
    private static SecureHttp.Response raw(String text) {
        return HTTP.handle(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static void expect(String label, int code, String req) {
        SecureHttp.Response response = raw(req);
        check(label, response.code == code);
    }
    private static String socketRequest(String text) throws Exception {
        try (Socket s = direct()) {
            s.setSoTimeout(4000);
            s.getOutputStream().write(text.getBytes(StandardCharsets.US_ASCII));
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int n; (n = s.getInputStream().read(buffer)) >= 0;) {
                b.write(buffer, 0, n);
                String response = b.toString("UTF-8");
                int end = response.indexOf("\r\n\r\n");
                if (end >= 0) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("Content-Length: ([0-9]+)").matcher(response);
                    if (m.find() && b.size() >= end + 4 + Integer.parseInt(m.group(1))) break;
                }
            }
            return b.toString("UTF-8");
        }
    }

    private static Socket direct() throws Exception {
        // The status channel is loopback-only; never consult a system proxy for it.
        Socket socket = new Socket(java.net.Proxy.NO_PROXY);
        socket.connect(new java.net.InetSocketAddress("127.0.0.1", StatusServer.PORT), 1500);
        return socket;
    }

    public static void main(String[] args) throws Exception {
        CONTEXT.getSharedPreferences("bsf", 0).edit().putString("pairToken", "11223344").commit();
        token = Pairing.token(CONTEXT);
        check("old short token migrated to 128 bits", token.matches("[A-F0-9]{32}"));
        check("token stable across reads", token.equals(Pairing.token(CONTEXT)));
        check("QR payload fits version-3 encoder", Pairing.payload(CONTEXT).getBytes("UTF-8").length <= 55);
        check("short token rejected", !Pairing.matches(CONTEXT, "11223344"));
        check("lowercase equivalent accepted", Pairing.matches(CONTEXT, token.toLowerCase(java.util.Locale.ROOT)));
        for (String path : new String[]{"/", "/status", "/feed", "/alive", "/event", "/quit", "/unknown"}) {
            expect("no credential " + path, 401, request("GET", path, null, null));
            expect("wrong credential " + path, 401, request("POST", path, repeat("0", 32), null));
        }
        check("unauthorized cannot mutate", PlaybackService.writes == 0);
        expect("authorized status read", 200, request("GET", "/status", token, null));
        SecureHttp.Response minimized = raw(request("GET", "/feed", token, null));
        check("feed diagnostic minimized", minimized.code == 200 && minimized.body.equals("feed=available\n"));
        expect("alive allowed", 200, request("GET", "/alive", token, null));
        expect("authorized unknown route", 404, request("GET", "/unknown", token, null));
        expect("GET status writes prohibited", 405, request("GET", "/status?tool=fixture&state=done", token, null));
        expect("GET quit prohibited", 405, request("GET", "/quit", token, null));
        expect("GET event prohibited", 405, request("GET", "/event", token, null));
        expect("POST feed prohibited", 405, request("POST", "/feed", token, null));
        expect("POST status valid", 200, request("POST", "/status", token, "tool=fixture&state=done&msg=%E5%AE%8C%E6%88%90"));
        check("UTF-8 form decoded correctly", PlaybackService.line.equals("fixture done 完成"));
        expect("POST event valid", 200, request("POST", "/event", token, "source=trade&state=alert"));
        check("event alert maps to error", PlaybackService.line.equals("trade error "));
        expect("POST quit valid", 200, request("POST", "/quit", token, null));
        check("quit resets status", PlaybackService.line.equals(" idle "));
        expect("legacy query token still authenticated", 200, request("GET", "/status?token=" + token, null, null));
        expect("header token alternative", 200, request("GET", "/status", null, null)
                .replace("Host: localhost", "Host: localhost\r\nX-BSF-Token: " + token));
        expect("duplicate credentials rejected", 400, request("GET", "/status?token=" + token, token, null));
        expect("duplicate form fields rejected", 400, request("POST", "/status?tool=fixture", token, "tool=other&state=done"));
        expect("case duplicate headers rejected", 400, request("GET", "/status", token, null)
                .replace("Host: localhost", "Host: localhost\r\nhOsT: localhost"));
        expect("invalid host rejected", 400, request("GET", "/status", token, null).replace("Host: localhost", "Host: invalid.example"));
        expect("browser origin rejected", 403, request("GET", "/status", token, null)
                .replace("Host: localhost", "Host: localhost\r\nOrigin: https://invalid.example"));
        int validWrites = PlaybackService.writes;
        expect("tool length bounded", 400, request("POST", "/status", token, "tool=" + repeat("x", 33) + "&state=done"));
        expect("message length bounded", 400, request("POST", "/status", token, "tool=fixture&state=done&msg=" + repeat("x", 41)));
        expect("control characters rejected", 400, request("POST", "/status", token, "tool=fixture&state=done&msg=%0aheader"));
        expect("bidi overrides rejected", 400, request("POST", "/status", token, "tool=fixture&state=done&msg=%E2%80%AE"));
        expect("unknown state rejected", 400, request("POST", "/status", token, "tool=fixture&state=untrusted"));
        expect("extra field rejected", 400, request("POST", "/status", token, "tool=fixture&state=done&other=x"));
        expect("missing field rejected", 400, request("POST", "/status", token, "tool=fixture"));
        expect("malformed percent rejected", 400, request("GET", "/status?token=%GG", null, null));
        expect("invalid UTF-8 rejected", 400, request("POST", "/status", token, "tool=fixture&state=done&msg=%c0%af"));
        expect("request line bounded", 414, request("GET", "/status?msg=" + repeat("x", 2100), token, null));
        expect("header line bounded", 431, request("GET", "/status", token, null)
                .replace("Host: localhost", "Host: localhost\r\nX-Pad: " + repeat("x", 4100)));
        StringBuilder manyHeaders = new StringBuilder("Host: localhost");
        for (int n = 0; n < 25; n++) manyHeaders.append("\r\nX-").append(n).append(": x");
        expect("header count bounded", 431, request("GET", "/status", token, null).replace("Host: localhost", manyHeaders));
        expect("body bounded", 413, request("POST", "/status", token, repeat("x", 2049)));
        expect("negative body length rejected", 400, request("POST", "/status", token, "x").replace("Content-Length: 1", "Content-Length: -1"));
        expect("duplicate body length rejected", 400, request("POST", "/status", token, "x").replace("Content-Length: 1", "Content-Length: 1\r\nContent-Length: 1"));
        expect("chunked body rejected", 400, request("POST", "/status", token, null).replace("Host: localhost", "Host: localhost\r\nTransfer-Encoding: chunked"));
        expect("unsupported type rejected", 415, request("POST", "/status", token, "x").replace("application/x-www-form-urlencoded", "application/json"));
        expect("truncated body rejected", 400, request("POST", "/status", token, "x").replace("Content-Length: 1", "Content-Length: 2"));
        check("invalid input has no mutation", PlaybackService.writes == validWrites);
        int beforeBad = PlaybackService.writes;
        new StatusReceiver().onReceive(CONTEXT, new android.content.Intent());
        check("disabled receiver has no side effect", PlaybackService.writes == beforeBad);
        String oldToken = token;
        Pairing.rotate(CONTEXT);
        token = Pairing.token(CONTEXT);
        check("rotation creates new token", !token.equals(oldToken));
        expect("rotated old token rejected", 401, request("GET", "/status", oldToken, null));
        expect("new token accepted", 200, request("GET", "/status", token, null));
        long start = System.nanoTime();
        InputStream trickle = new InputStream() {
            final byte[] data = request("GET", "/status", token, null).getBytes(StandardCharsets.US_ASCII);
            int n;
            public int read() {
                try { Thread.sleep(45); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return n < data.length ? data[n++] : -1;
            }
        };
        check("absolute request deadline", HTTP.handle(trickle).code == 408 && System.nanoTime() - start < 3800000000L);
        if (args.length > 0 && args[0].equals("offline")) {
            System.out.println("RESULT " + passed + " passed; socket tests reserved for device");
            return;
        }
        StatusServer.start(CONTEXT);
        String firstWire = socketRequest(request("GET", "/status", null, null));
        System.out.println("WIRE " + firstWire.split("\r\n")[0]);
        check("real socket unauthorized blocked", firstWire.startsWith("HTTP/1.1 401 "));
        check("real socket authorized allowed", socketRequest(request("GET", "/status", token, null)).startsWith("HTTP/1.1 200 "));
        try (Socket slow = direct()) {
            slow.setSoTimeout(3000);
            slow.getOutputStream().write('G');
            byte[] buffer = new byte[1024];
            int n = slow.getInputStream().read(buffer);
            check("socket inactivity timeout", new String(buffer, 0, n, StandardCharsets.US_ASCII).startsWith("HTTP/1.1 408 "));
        }
        List<Socket> held = new ArrayList<>();
        boolean saturated = false;
        for (int i = 0; i < 8; i++) { Socket s = direct(); s.setSoTimeout(3000); held.add(s); }
        for (Socket s : held) {
            byte[] b = new byte[1024];
            int n = s.getInputStream().read(b);
            if (n > 0 && new String(b, 0, n, StandardCharsets.US_ASCII).contains("503")) saturated = true;
            s.close();
        }
        check("worker queue saturation bounded", saturated);
        Thread.sleep(1100);
        boolean limited = false;
        for (int i = 0; i < 28; i++) {
            if (socketRequest(request("GET", "/alive", null, null)).startsWith("HTTP/1.1 429 ")) limited = true;
        }
        check("connection rate limited", limited);
        Thread.sleep(1100);
        check("recovers after invalid and saturated requests", socketRequest(request("GET", "/alive", token, null)).startsWith("HTTP/1.1 200 "));
        StatusServer.stop();
        boolean closed = false;
        try { direct().close(); } catch (java.io.IOException expected) { closed = true; }
        check("stop closes listening port", closed);
        System.out.println("RESULT " + passed + " passed");
    }
}
