package com.nubia.micmini;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Auto-start no boot: sobe o serviço sozinho após reiniciar (diferencial vs WO Mic/IP Webcam). */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context ctx, Intent intent) {
        Intent i = new Intent(ctx, MicService.class);
        if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i); else ctx.startService(i);
    }
}
