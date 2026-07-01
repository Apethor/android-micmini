package com.apethor.micmini;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

/** Minimal screen: requests the microphone permission and starts the TCP service. */
public class MainActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        TextView tv = new TextView(this);
        tv.setText("MicMini\n\nPCM over TCP, port " + MicService.PORT
                + "\n" + MicService.SAMPLE_RATE + " Hz mono s16le\n\nServer started.");
        tv.setTextSize(20);
        tv.setPadding(40, 80, 40, 40);
        setContentView(tv);

        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        } else {
            startSvc();
        }
    }

    @Override public void onRequestPermissionsResult(int rc, String[] p, int[] r) {
        startSvc();
    }

    private void startSvc() {
        Intent i = new Intent(this, MicService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
    }
}
