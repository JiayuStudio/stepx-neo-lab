import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.UiAutomation;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.Display;
import android.view.accessibility.AccessibilityNodeInfo;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Fixed UI operations under shell UID; no network service or arbitrary commands. */
public final class PhoneUiHelper {
    private UiAutomation automation;
    private HandlerThread thread;
    private final List<AccessibilityNodeInfo> nodes = new ArrayList<>();
    private int screenWidth, screenHeight, imageWidth, imageHeight;

    private void geometry(int width, int height) {
        screenWidth = width;
        screenHeight = height;
        double scale = Math.min(1.0, Math.min(920.0 / Math.min(width, height),
                2048.0 / Math.max(width, height)));
        imageWidth = (int) Math.round(width * scale);
        imageHeight = (int) Math.round(height * scale);
    }

    private void geometry() throws Exception {
        Class<?> manager = Class.forName("android.hardware.display.DisplayManagerGlobal");
        Object instance = manager.getMethod("getInstance").invoke(null);
        Display display = (Display) manager.getMethod("getRealDisplay", int.class).invoke(instance, 0);
        Point size = new Point();
        display.getRealSize(size);
        geometry(size.x, size.y);
    }

    private JSONObject dimensions() throws Exception {
        return new JSONObject().put("width", imageWidth).put("height", imageHeight)
                .put("screen_width", screenWidth).put("screen_height", screenHeight)
                .put("coordinate_units", "image_pixels")
                .put("instruction", "Use image pixel coordinates from this image or phone_ui bounds. "
                        + "phone_tap and phone_swipe automatically map image pixels to the physical display. "
                        + "Do not manually scale. Use phone_input for text, not the soft keyboard.");
    }

    private void connect() throws Exception {
        if (Looper.getMainLooper() == null) Looper.prepareMainLooper();
        thread = new HandlerThread("StepneoPhoneUi");
        thread.start();
        Object connection = Class.forName("android.app.UiAutomationConnection").getConstructor().newInstance();
        automation = (UiAutomation) UiAutomation.class.getConstructor(Looper.class,
                Class.forName("android.app.IUiAutomationConnection")).newInstance(thread.getLooper(), connection);
        // Preserve the owner's existing accessibility services. No settings or permissions change.
        int flags = UiAutomation.class.getField("FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES").getInt(null);
        UiAutomation.class.getMethod("connectWithTimeout", int.class, long.class)
                .invoke(automation, flags, 3000L);
        AccessibilityServiceInfo info = automation.getServiceInfo();
        info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        automation.setServiceInfo(info);
    }

    private static String string(CharSequence value) { return value == null ? "" : value.toString(); }

    private void collect(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 60 || nodes.size() >= 2000) return;
        nodes.add(node);
        for (int i = 0; i < node.getChildCount(); i++) collect(node.getChild(i), depth + 1);
    }

    private void readNodes() throws Exception {
        for (int i = 0; i < 6; i++) {
            AccessibilityNodeInfo root = automation.getRootInActiveWindow();
            if (root != null) { collect(root, 0); return; }
            Thread.sleep(100);
        }
        throw new IllegalStateException("No active UI root. Unlock the phone and inspect a screenshot; do not repeat the task.");
    }

    private JSONArray bounds(AccessibilityNodeInfo node) {
        Rect r = new Rect();
        node.getBoundsInScreen(r);
        return new JSONArray().put(Math.round((float) r.left * imageWidth / screenWidth))
                .put(Math.round((float) r.top * imageHeight / screenHeight))
                .put(Math.round((float) r.right * imageWidth / screenWidth))
                .put(Math.round((float) r.bottom * imageHeight / screenHeight));
    }

    private JSONObject describe(AccessibilityNodeInfo node) throws Exception {
        return new JSONObject().put("package", string(node.getPackageName()))
                .put("resource_id", node.getViewIdResourceName() == null ? "" : node.getViewIdResourceName())
                .put("class", string(node.getClassName())).put("text", node.isPassword() ? "[password]" : string(node.getText()))
                .put("description", string(node.getContentDescription())).put("hint", string(node.getHintText()))
                .put("bounds", bounds(node)).put("clickable", node.isClickable()).put("editable", node.isEditable())
                .put("focused", node.isFocused()).put("enabled", node.isEnabled()).put("scrollable", node.isScrollable());
    }

    private JSONObject snapshot() throws Exception {
        readNodes();
        JSONArray result = new JSONArray();
        for (AccessibilityNodeInfo node : nodes) {
            if (!node.isVisibleToUser()) continue;
            if (node.isEditable() || node.isClickable() || node.isScrollable()
                    || node.getText() != null || node.getContentDescription() != null) result.put(describe(node));
            if (result.length() >= 300) break;
        }
        return dimensions().put("nodes", result).put("node_limit_reached", nodes.size() >= 2000 || result.length() >= 300)
                .put("snapshot_method", "accessibility_without_global_idle_wait");
    }

    private AccessibilityNodeInfo select(JSONObject args, boolean editable) throws Exception {
        readNodes();
        List<AccessibilityNodeInfo> found = new ArrayList<>();
        for (AccessibilityNodeInfo node : nodes) {
            if (!node.isVisibleToUser() || !node.isEnabled() || (editable && !node.isEditable())) continue;
            if (args.has("resource_id") && !args.getString("resource_id").equals(node.getViewIdResourceName())) continue;
            if (args.has("target_text") && !args.getString("target_text").equals(string(node.getText()))
                    && !args.getString("target_text").equals(string(node.getContentDescription()))
                    && !args.getString("target_text").equals(string(node.getHintText()))) continue;
            if (args.has("package") && !args.getString("package").equals(string(node.getPackageName()))) continue;
            found.add(node);
        }
        if (found.size() != 1) throw new IllegalStateException("Expected exactly one matching "
                + (editable ? "editable field" : "UI element") + "; found " + found.size()
                + ". Read phone_ui and refine the selector; no action performed.");
        return found.get(0);
    }

    private JSONObject click(JSONObject args) throws Exception {
        AccessibilityNodeInfo node = select(args, false);
        AccessibilityNodeInfo target = node;
        for (int i = 0; i < 5 && !target.isClickable() && !target.isEditable(); i++) {
            AccessibilityNodeInfo parent = target.getParent();
            if (parent == null) break;
            target = parent;
        }
        boolean ok = target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        if (!ok) throw new IllegalStateException("UI element rejected ACTION_CLICK; inspect bounds and use phone_tap.");
        return new JSONObject().put("action_accepted", true).put("target", describe(node))
                .put("instruction", "Read phone_ui to verify navigation before entering text.");
    }

    private JSONObject input(JSONObject args) throws Exception {
        AccessibilityNodeInfo node;
        if (args.has("resource_id") || args.has("target_text")) {
            node = select(args, true);
            if (!node.isFocused()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                Thread.sleep(100);
                node.refresh();
            }
        } else {
            // The browser's global focus query can report the WebView container
            // while its editable child is focused. Use the active UI tree,
            // exactly as phone_ui does, and refuse ambiguous input focus.
            readNodes();
            node = null;
            for (AccessibilityNodeInfo candidate : nodes) {
                if (!candidate.isFocused() || !candidate.isEditable()
                        || !candidate.isVisibleToUser() || !candidate.isEnabled()) continue;
                if (node != null) throw new IllegalStateException(
                        "Multiple focused editable fields. Specify the intended field; no text entered.");
                node = candidate;
            }
        }
        if (node == null || !node.isEditable() || !node.isVisibleToUser() || !node.isEnabled() || !node.isFocused())
            throw new IllegalStateException("No focused editable field. Click the intended input or specify a unique field selector first; no text entered.");
        if (node.isPassword()) throw new IllegalStateException("Password fields are excluded from automated text verification.");
        if (args.has("package") && !args.getString("package").equals(string(node.getPackageName())))
            throw new IllegalStateException("Focused field belongs to a different package; no text entered.");
        String text = args.getString("text");
        String mode = args.optString("mode", "replace");
        String current = node.isShowingHintText() ? "" : string(node.getText());
        String expected = mode.equals("append") ? current + text : text;
        Bundle bundle = new Bundle();
        bundle.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, expected);
        if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle))
            throw new IllegalStateException("Field rejected Unicode ACTION_SET_TEXT; no keyboard or clipboard fallback was attempted.");
        boolean verified = false;
        for (int i = 0; i < 10; i++) {
            Thread.sleep(80);
            if (!node.refresh()) break;
            if (expected.equals(node.isShowingHintText() ? "" : string(node.getText()))) { verified = true; break; }
        }
        return new JSONObject().put("verified", verified).put("isError", !verified)
                .put("input_method", "accessibility_set_text").put("mode", mode)
                .put("characters", expected.codePointCount(0, expected.length()))
                .put("package", string(node.getPackageName())).put("resource_id", node.getViewIdResourceName())
                .put("submitted", false).put("message", verified ? "Text read back and matched exactly. No Enter or Send was pressed."
                        : "Field changed but text could not be verified. Inspect phone_ui; do not blindly append or repeat.");
    }

    private JSONObject resize(String source, String target) throws Exception {
        Bitmap raw = BitmapFactory.decodeFile(source);
        if (raw == null) throw new IllegalStateException("Cannot decode screen PNG.");
        geometry(raw.getWidth(), raw.getHeight());
        Bitmap scaled = Bitmap.createScaledBitmap(raw, imageWidth, imageHeight, true);
        try (FileOutputStream stream = new FileOutputStream(target)) {
            if (!scaled.compress(Bitmap.CompressFormat.PNG, 100, stream)) throw new IllegalStateException("Cannot encode PNG.");
        }
        return dimensions();
    }

    public static void main(String[] args) {
        PhoneUiHelper helper = new PhoneUiHelper();
        int code = 0;
        try {
            JSONObject result;
            if (args.length == 1 && args[0].equals("geometry")) {
                helper.geometry(); result = helper.dimensions();
            } else if (args.length == 3 && args[0].equals("resize")) {
                result = helper.resize(args[1], args[2]);
            } else {
                helper.geometry();
                helper.connect();
                if (args.length == 1 && args[0].equals("ui")) result = helper.snapshot();
                else if (args.length == 2 && (args[0].equals("input") || args[0].equals("click"))) {
                    JSONObject request = new JSONObject(new String(Files.readAllBytes(Paths.get(args[1])), StandardCharsets.UTF_8));
                    result = args[0].equals("input") ? helper.input(request) : helper.click(request);
                    if (result.optBoolean("isError")) code = 1;
                } else throw new IllegalArgumentException("Unsupported fixed UI operation.");
            }
            System.out.println(result.toString());
        } catch (Throwable failure) {
            Throwable cause = failure;
            while (cause.getCause() != null) cause = cause.getCause();
            try { System.out.println(new JSONObject().put("isError", true).put("message",
                    cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage()).toString()); }
            catch (Exception ignored) { System.out.println("{\"isError\":true,\"message\":\"UI helper failed\"}"); }
            code = 1;
        } finally {
            if (helper.automation != null) {
                try { UiAutomation.class.getMethod("disconnect").invoke(helper.automation); } catch (Throwable ignored) {}
            }
            if (helper.thread != null) helper.thread.quitSafely();
        }
        System.exit(code);
    }
}
