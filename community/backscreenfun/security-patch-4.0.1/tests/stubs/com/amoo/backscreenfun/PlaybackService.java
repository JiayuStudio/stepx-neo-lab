package com.amoo.backscreenfun;
import android.content.Context;
final class PlaybackService {
    static String line = "fixture running synthetic-message";
    static int writes;
    static void pushStatus(Context context, String tool, String state, String message) {
        line = tool + " " + state + " " + message;
        writes++;
    }
    static void showStatus(Context context) { }
    static String statusLine() { return line; }
    static long ticks() { return 12; }
    static boolean isPlaying() { return true; }
    static String kind() { return "status"; }
}
