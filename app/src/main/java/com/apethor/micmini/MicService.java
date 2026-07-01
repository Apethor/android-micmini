package com.apethor.micmini;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * MicMini — captures the microphone and serves raw PCM over TCP (the phone is the server).
 * Wire format: PCM signed 16-bit LITTLE-ENDIAN, mono, {@link #SAMPLE_RATE} Hz — no header.
 * A client just does connect(ip, {@link #PORT}) and reads the byte stream.
 * Recording runs only while a client is connected (saves battery).
 *
 * NOTE: the stream is unauthenticated and unencrypted — intended for a trusted LAN.
 * Set {@link #BIND_LOOPBACK} to true to listen on 127.0.0.1 only (use with `adb forward`).
 */
public class MicService extends Service {
    public static final String TAG = "MicMini";
    public static final int PORT = 6000;
    public static final int SAMPLE_RATE = 16000;   // Hz. Fixed wire contract — if you change it,
                                                   // change the client too (raw stream has no header).
    private static final int CHUNK = 1280;         // 40 ms @16k mono s16le -> low latency

    // false = listen on all interfaces (LAN use). true = 127.0.0.1 only (use with `adb forward`).
    private static final boolean BIND_LOOPBACK = false;

    private volatile boolean running = false;
    private volatile ServerSocket server;
    private Thread worker;

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) {
            running = true;
            startForeground(1, buildNotification());
            worker = new Thread(this::serverLoop, "micmini-server");
            worker.start();
        }
        return START_STICKY;
    }

    private Notification buildNotification() {
        String chId = "micmini";
        String text = "PCM TCP :" + PORT + "  " + SAMPLE_RATE + "Hz mono s16le";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    chId, "MicMini", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(ch);
            return new Notification.Builder(this, chId)
                    .setContentTitle("MicMini").setContentText(text)
                    .setSmallIcon(android.R.drawable.ic_btn_speak_now).build();
        } else {
            return new Notification.Builder(this)
                    .setContentTitle("MicMini").setContentText(text)
                    .setSmallIcon(android.R.drawable.ic_btn_speak_now).build();
        }
    }

    private void serverLoop() {
        try {
            InetAddress bindAddr = BIND_LOOPBACK ? InetAddress.getLoopbackAddress() : null; // null = all
            server = new ServerSocket();
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress(bindAddr, PORT));
            Log.i(TAG, "listening on " + (BIND_LOOPBACK ? "127.0.0.1:" : "0.0.0.0:") + PORT
                    + "  " + SAMPLE_RATE + "Hz mono s16le");
            // One client at a time: while streaming to a client, further connections wait in the
            // OS accept backlog and are served after the current one disconnects.
            while (running) {
                Socket sock = server.accept();
                Log.i(TAG, "client connected: " + sock.getRemoteSocketAddress());
                try {
                    sock.setTcpNoDelay(true);
                    streamTo(sock);
                } catch (Exception e) {
                    Log.w(TAG, "client ended: " + e);
                } finally {
                    try { sock.close(); } catch (Exception ignore) {}
                }
            }
        } catch (Exception e) {
            if (running) Log.e(TAG, "server error", e);   // expected when closed on shutdown
        } finally {
            closeServer();
        }
    }

    private void streamTo(Socket sock) throws Exception {
        int minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (minBuf <= 0) minBuf = 4096;
        int recBuf = Math.max(minBuf, 8192);
        AudioRecord rec = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, recBuf);
        if (rec.getState() != AudioRecord.STATE_INITIALIZED) {
            rec.release();
            // Keep the wire format fixed: rather than silently changing the rate (which would
            // change pitch for the client), fail loudly. 16 kHz is widely supported; if a device
            // does not support it, change SAMPLE_RATE here and in the client.
            Log.e(TAG, "AudioRecord init failed @" + SAMPLE_RATE + "Hz (device may not support it)");
            throw new Exception("AudioRecord init failed @" + SAMPLE_RATE + "Hz");
        }
        byte[] buf = new byte[CHUNK];
        OutputStream out = sock.getOutputStream();
        rec.startRecording();
        try {
            while (running && !sock.isClosed()) {
                int n = rec.read(buf, 0, buf.length);
                if (n > 0) out.write(buf, 0, n);
                else if (n < 0) break;
            }
            out.flush();
        } finally {
            try { rec.stop(); } catch (Exception ignore) {}
            rec.release();
        }
    }

    private void closeServer() {
        ServerSocket s = server;
        if (s != null) try { s.close(); } catch (Exception ignore) {}
        server = null;
    }

    @Override public void onDestroy() {
        running = false;
        closeServer();
        if (worker != null) worker.interrupt();
        super.onDestroy();
    }
}
