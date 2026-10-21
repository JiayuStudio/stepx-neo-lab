package com.amoo.backscreenfun;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Legacy broadcasts are disabled; paired HTTP is the supported external interface. */
public final class StatusReceiver extends BroadcastReceiver {
    public static final String ACTION = "com.amoo.backscreenfun.STATUS";
    @Override public void onReceive(Context context, Intent intent) { }
}
