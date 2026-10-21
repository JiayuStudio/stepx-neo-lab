package com.amoo.backscreenfun;

import android.content.Context;
import android.content.SharedPreferences;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

/** Per-install 128-bit pairing secret; intentionally never logged. */
final class Pairing {
    private Pairing() { }
    static synchronized String token(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("bsf", Context.MODE_PRIVATE);
        String saved = prefs.getString("pairToken", "");
        if (saved != null && saved.matches("[A-F0-9]{32}")) return saved;
        byte[] random = new byte[16];
        new SecureRandom().nextBytes(random);
        char[] hex = "0123456789ABCDEF".toCharArray(), output = new char[32];
        for (int i = 0; i < random.length; i++) {
            output[i * 2] = hex[(random[i] & 255) >>> 4];
            output[i * 2 + 1] = hex[random[i] & 15];
        }
        String created = new String(output);
        // Synchronous durable rotation; never accept a token that failed to persist.
        if (!prefs.edit().putString("pairToken", created).commit()) throw new IllegalStateException("Pairing unavailable");
        return created;
    }
    static synchronized void rotate(Context context) {
        if (!context.getSharedPreferences("bsf", Context.MODE_PRIVATE).edit().remove("pairToken").commit())
            throw new IllegalStateException("Pairing unavailable");
        token(context);
    }
    static String payload(Context context) { return "BSF1|47321|" + token(context); }
    static synchronized boolean matches(Context context, String candidate) {
        if (candidate == null || !candidate.matches("[a-fA-F0-9]{32}")) return false;
        return MessageDigest.isEqual(token(context).getBytes(StandardCharsets.US_ASCII),
                candidate.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }
}
