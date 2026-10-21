package studio.jiayu.bsfprobe;

import android.app.Activity;
import android.os.Bundle;
import android.content.ComponentName;
import android.content.Intent;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Disposable independent UID. No pairing secret is passed to this application. */
public final class AndroidProbe extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        TextView label = new TextView(this);
        label.setText("背屏安全回归检测\n仅发送未授权的虚构状态。测试完成后自动移除本工具。");
        setContentView(label);
        new Thread(new Runnable() { public void run() { probe(); } }, "bsf-probe").start();
    }
    private void probe() {
        JSONObject result = new JSONObject();
        JSONArray requests = new JSONArray();
        try {
            for (String path : new String[]{"/status", "/feed", "/alive", "/quit", "/event", "/"})
                requests.put(request("GET", path, null));
            requests.put(request("POST", "/status", "tool=untrusted&state=error&msg=probe-unauthorized"));
            requests.put(request("POST", "/quit", null));
            requests.put(request("POST", "/event", "source=trade&state=buy"));
            requests.put(request("GET", "/status?tool=untrusted&state=error", null));
            Thread.sleep(1200);
            try (Socket slow = connect()) {
                slow.getOutputStream().write('G');
                result.put("inactivity_timeout_status", responseStatus(slow));
            }
            List<Socket> held = new ArrayList<>();
            JSONArray saturation = new JSONArray();
            for (int i = 0; i < 8; i++) held.add(connect());
            for (Socket s : held) {
                try { saturation.put(responseStatus(s)); } finally { s.close(); }
            }
            result.put("saturation_statuses", saturation);
            Thread.sleep(1200);
            int limited = 0;
            for (int i = 0; i < 64; i++)
                if (request("GET", "/alive", null).getInt("status") == 429) limited++;
            result.put("rate_limited_requests", limited);
            Thread.sleep(1200);
            result.put("recovery_unauthorized_status", request("GET", "/alive", null).getInt("status"));
            Intent legacy = new Intent("com.amoo.backscreenfun.STATUS");
            legacy.setComponent(new ComponentName("com.jiayu.backscreenfun.secure", "com.amoo.backscreenfun.StatusReceiver"));
            legacy.putExtra("tool", "untrusted");
            legacy.putExtra("state", "error");
            legacy.putExtra("msg", "probe-unauthorized");
            try { sendBroadcast(legacy); result.put("broadcast_attempt", "sent_without_privilege"); }
            catch (SecurityException expected) { result.put("broadcast_attempt", "permission_denied"); }
            result.put("requests", requests);
            result.put("complete", true);
        } catch (Exception e) {
            try { result.put("complete", false); result.put("error_class", e.getClass().getSimpleName()); }
            catch (Exception ignored) { }
        }
        try (FileOutputStream output = openFileOutput("probe-result.json", MODE_PRIVATE)) {
            output.write(result.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) { }
    }
    private JSONObject request(String method, String path, String body) throws Exception {
        String text = method + " " + path + " HTTP/1.1\r\nHost: localhost\r\n"
                + (body == null ? "" : "Content-Type: application/x-www-form-urlencoded\r\nContent-Length: "
                    + body.getBytes(StandardCharsets.UTF_8).length + "\r\n")
                + "\r\n" + (body == null ? "" : body);
        try (Socket socket = connect()) {
            socket.getOutputStream().write(text.getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int n; (n = socket.getInputStream().read(buffer)) >= 0;) {
                data.write(buffer, 0, n);
                if (data.size() > 4096) throw new IllegalStateException("Response too large");
                if (completeResponse(data)) break;
            }
            String response = data.toString("UTF-8");
            JSONObject item = new JSONObject();
            item.put("method", method);
            item.put("path", path);
            item.put("status", Integer.parseInt(response.split(" ", 3)[1]));
            item.put("contains_synthetic_message", response.contains("synthetic-message"));
            item.put("contains_unauthorized_marker", response.contains("probe-unauthorized"));
            return item;
        }
    }
    private Socket connect() throws Exception {
        Socket socket = new Socket(java.net.Proxy.NO_PROXY);
        socket.connect(new java.net.InetSocketAddress("127.0.0.1", 47321), 2000);
        socket.setSoTimeout(6000);
        return socket;
    }
    private boolean completeResponse(ByteArrayOutputStream data) throws Exception {
        String response = data.toString("UTF-8");
        int end = response.indexOf("\r\n\r\n");
        java.util.regex.Matcher length = java.util.regex.Pattern.compile("Content-Length: ([0-9]+)").matcher(response);
        return end >= 0 && length.find() && data.size() >= end + 4 + Integer.parseInt(length.group(1));
    }
    private int responseStatus(Socket socket) throws Exception {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int n; (n = socket.getInputStream().read(buffer)) >= 0;) {
            data.write(buffer, 0, n);
            if (data.size() > 4096) throw new IllegalStateException("Response too large");
            if (completeResponse(data)) break;
        }
        return Integer.parseInt(data.toString("UTF-8").split(" ", 3)[1]);
    }
}
