package com.amoo.backscreenfun;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Small, bounded HTTP protocol for the local paired status channel. */
final class SecureHttp {
    static final int MAX_LINE = 2048, MAX_HEADERS = 4096, MAX_BODY = 2048;
    static final long DEADLINE_MS = 3000;

    interface Backend {
        boolean authorized(String token);
        String status();
        String alive();
        void update(String tool, String state, String message, boolean show);
    }

    static final class Response {
        final int code;
        final String body;
        Response(int code, String body) { this.code = code; this.body = body + "\n"; }
        byte[] bytes() {
            byte[] data = body.getBytes(StandardCharsets.UTF_8);
            String head = "HTTP/1.1 " + code + " " + reason(code) + "\r\n"
                    + "Content-Type: text/plain; charset=utf-8\r\n"
                    + "Content-Length: " + data.length + "\r\n"
                    + "Cache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\n"
                    + (code == 401 ? "WWW-Authenticate: Bearer realm=\"backscreen\"\r\n" : "")
                    + "Connection: close\r\n\r\n";
            byte[] headers = head.getBytes(StandardCharsets.US_ASCII);
            byte[] out = new byte[headers.length + data.length];
            System.arraycopy(headers, 0, out, 0, headers.length);
            System.arraycopy(data, 0, out, headers.length, data.length);
            return out;
        }
    }

    private static final class Rejected extends Exception {
        final int code;
        Rejected(int code) { this.code = code; }
    }

    private final Backend backend;
    SecureHttp(Backend backend) { this.backend = backend; }

    Response handle(InputStream input) {
        try {
            return parse(input, System.nanoTime() + DEADLINE_MS * 1000000L);
        } catch (Rejected e) {
            return new Response(e.code, reason(e.code));
        } catch (SocketTimeoutException e) {
            return new Response(408, "Request Timeout");
        } catch (IOException | IllegalArgumentException e) {
            return new Response(400, "Bad Request");
        } catch (RuntimeException e) {
            // No exception text, request data, addresses or credentials in responses/logs.
            return new Response(503, "Service Unavailable");
        }
    }

    private Response parse(InputStream in, long deadline) throws IOException, Rejected {
        String first = line(in, MAX_LINE, 414, deadline);
        String[] parts = first.split(" ", -1);
        require(parts.length == 3 && parts[0].matches("[A-Z]{3,8}")
                && (parts[2].equals("HTTP/1.1") || parts[2].equals("HTTP/1.0")), 400);
        String method = parts[0], target = parts[1];
        require(target.startsWith("/") && !target.startsWith("//")
                && !target.contains("#"), 400);
        Map<String, String> headers = new LinkedHashMap<>();
        int total = 0;
        while (true) {
            String text = line(in, MAX_HEADERS, 431, deadline);
            total += text.length() + 2;
            require(total <= MAX_HEADERS, 431);
            if (text.isEmpty()) break;
            require(headers.size() < 24, 431);
            int colon = text.indexOf(':');
            require(colon > 0, 400);
            String name = text.substring(0, colon);
            require(name.matches("[A-Za-z0-9-]{1,40}"), 400);
            name = name.toLowerCase(Locale.ROOT);
            require(!headers.containsKey(name), 400);
            String value = text.substring(colon + 1).trim();
            require(value.indexOf('\t') < 0, 400);
            headers.put(name, value);
        }
        String host = headers.get("host");
        require((host != null || parts[2].equals("HTTP/1.0"))
                && (host == null || host.matches("(?i)(localhost|127\\.0\\.0\\.1)(:[0-9]{1,5})?")), 400);
        require(!headers.containsKey("transfer-encoding") && !headers.containsKey("expect"), 400);
        int length = 0;
        if (headers.containsKey("content-length")) {
            String value = headers.get("content-length");
            require(value.matches("[0-9]{1,8}"), 400);
            long size = Long.parseLong(value);
            require(size <= MAX_BODY, 413);
            length = (int) size;
        }
        require(!method.equals("GET") || length == 0, 400);
        if (length > 0) {
            String type = headers.get("content-type");
            require(type != null && type.toLowerCase(Locale.ROOT)
                    .matches("application/x-www-form-urlencoded( *; *charset=utf-8)?"), 415);
        }
        byte[] body = new byte[length];
        for (int n = 0; n < length; n++) body[n] = (byte) read(in, deadline);
        Map<String, String> params = new LinkedHashMap<>();
        int question = target.indexOf('?');
        String path = question < 0 ? target : target.substring(0, question);
        require(path.matches("/[a-z]*"), 400);
        if (question >= 0) form(params, target.substring(question + 1));
        if (length > 0) form(params, utf8(body));

        String token = params.remove("token");
        String bearer = headers.get("authorization"), direct = headers.get("x-bsf-token");
        int credentialCount = (token == null ? 0 : 1) + (bearer == null ? 0 : 1) + (direct == null ? 0 : 1);
        require(credentialCount <= 1, 400);
        if (bearer != null) {
            require(bearer.startsWith("Bearer "), 401);
            token = bearer.substring(7);
        } else if (direct != null) token = direct;
        require(backend.authorized(token), 401);
        // This CLI/USB channel has no browser integration or CORS grant.
        require(!headers.containsKey("origin"), 403);
        switch (path) {
            case "/status":
                if (method.equals("GET")) {
                    require(params.isEmpty(), 405);
                    return new Response(200, bounded(backend.status(), 160));
                }
                require(method.equals("POST"), 405);
                allowed(params, "tool", "state", "msg");
                String tool = params.get("tool"), state = params.get("state");
                String message = params.containsKey("msg") ? params.get("msg") : "";
                require(tool != null && tool.matches("[a-zA-Z0-9_.-]{1,32}"), 400);
                require(state != null && state.matches("thinking|running|waiting|done|error|idle|ok|buy|sell"), 400);
                require(message.codePointCount(0, message.length()) <= 40 && clean(message), 400);
                backend.update(tool, state, message, false);
                return new Response(200, "ok");
            case "/event":
                require(method.equals("POST"), 405);
                allowed(params, "source", "state");
                String source = params.get("source"), event = params.get("state");
                require(source != null && source.matches("server|trade|crawler")
                        && event != null && event.matches("running|ok|alert|done|error|buy|sell"), 400);
                backend.update(source, event.equals("alert") ? "error" : event, "", true);
                return new Response(200, "ok");
            case "/quit":
                require(method.equals("POST"), 405);
                require(params.isEmpty(), 400);
                backend.update("", "idle", "", false);
                return new Response(200, "idle");
            case "/feed":
                require(method.equals("GET"), 405);
                require(params.isEmpty(), 400);
                // Raw ServiceFeed diagnostics contain account fields; never expose them.
                return new Response(200, "feed=available");
            case "/alive":
                require(method.equals("GET"), 405);
                require(params.isEmpty(), 400);
                return new Response(200, bounded(backend.alive(), 120));
            default:
                return new Response(404, "Not Found");
        }
    }

    private static void allowed(Map<String, String> params, String... names) throws Rejected {
        for (String key : params.keySet()) {
            boolean found = false;
            for (String name : names) if (name.equals(key)) found = true;
            require(found, 400);
        }
    }

    private static boolean clean(String value) {
        for (int n = 0; n < value.length(); n++) {
            char c = value.charAt(n);
            if (Character.isISOControl(c) || c == '\u2028' || c == '\u2029'
                    || (c >= '\u202a' && c <= '\u202e') || (c >= '\u2066' && c <= '\u2069')) return false;
        }
        return true;
    }

    private static String bounded(String value, int max) {
        if (value == null) return "";
        if (value.length() > max) value = value.substring(0, max);
        return clean(value) ? value : "status unavailable";
    }

    private static void form(Map<String, String> params, String text) throws Rejected {
        if (text.isEmpty()) return;
        String[] fields = text.split("&", -1);
        require(fields.length <= 8, 400);
        for (String field : fields) {
            int equal = field.indexOf('=');
            require(equal > 0, 400);
            String key = decode(field.substring(0, equal)), value = decode(field.substring(equal + 1));
            require(key.matches("[a-z]{1,12}") && !params.containsKey(key) && params.size() < 8, 400);
            require(value.length() <= (key.equals("token") ? 32 : 160), 400);
            params.put(key, value);
        }
    }

    private static String decode(String text) throws Rejected {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int n = 0; n < text.length(); n++) {
            char c = text.charAt(n);
            if (c == '%') {
                require(n + 2 < text.length(), 400);
                int a = Character.digit(text.charAt(++n), 16), b = Character.digit(text.charAt(++n), 16);
                require(a >= 0 && b >= 0, 400);
                out.write(a * 16 + b);
            } else {
                require(c >= 32 && c < 127, 400);
                out.write(c == '+' ? ' ' : c);
            }
        }
        return utf8(out.toByteArray());
    }

    private static String utf8(byte[] data) throws Rejected {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(data)).toString();
        } catch (CharacterCodingException e) { throw new Rejected(400); }
    }

    private static String line(InputStream in, int max, int code, long deadline) throws IOException, Rejected {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (true) {
            int b = read(in, deadline);
            if (b == '\r') {
                require(read(in, deadline) == '\n', 400);
                return out.toString("US-ASCII");
            }
            require(b >= 32 && b < 127, 400);
            require(out.size() < max, code);
            out.write(b);
        }
    }

    private static int read(InputStream in, long deadline) throws IOException, Rejected {
        require(System.nanoTime() < deadline, 408);
        int b = in.read();
        require(b >= 0, 400);
        require(System.nanoTime() < deadline, 408);
        return b;
    }

    private static void require(boolean value, int code) throws Rejected {
        if (!value) throw new Rejected(code);
    }

    private static String reason(int code) {
        switch (code) {
            case 200: return "OK";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 408: return "Request Timeout";
            case 413: return "Content Too Large";
            case 414: return "URI Too Long";
            case 415: return "Unsupported Media Type";
            case 429: return "Too Many Requests";
            case 431: return "Request Header Fields Too Large";
            default: return "Service Unavailable";
        }
    }
}
