package com.apethor.micmini;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Starts the service on boot so the phone serves audio without any manual step after a restart.
 *  NOTE: some ROMs block auto-start by default — whitelist the app in the vendor's autostart
 *  manager (e.g. Settings -> Apps -> menu -> Autostart management). */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context ctx, Intent intent) {
        Intent i = new Intent(ctx, MicService.class);
        if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i); else ctx.startService(i);
    }
}
