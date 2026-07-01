package com.nubia.micmini;

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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * MicMini — captura o microfone e serve PCM cru por TCP (celular = servidor).
 * Formato do stream: PCM signed 16-bit LITTLE-ENDIAN, mono, {@link #SAMPLE_RATE} Hz.
 * O consumidor (ex.: no Orange Pi/Batocera) faz connect(ip, PORT) e lê os bytes direto.
 * Grava só enquanto há um cliente conectado (economiza bateria).
 */
public class MicService extends Service {
    public static final String TAG = "MicMini";
    public static final int PORT = 6000;
    public static final int SAMPLE_RATE = 16000;   // Hz  (mude p/ 48000 se quiser fidelidade)
    private static final int CHUNK = 1280;         // 40 ms @16k mono s16le -> baixa latência

    private volatile boolean running = false;
    private Thread worker;
    private ServerSocket server;

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
            server = new ServerSocket();
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress(PORT));
            Log.i(TAG, "listening :" + PORT + "  " + SAMPLE_RATE + "Hz mono s16le");
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
            Log.e(TAG, "server error", e);
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
        if (server != null) try { server.close(); } catch (Exception ignore) {}
        server = null;
    }

    @Override public void onDestroy() {
        running = false;
        closeServer();
        if (worker != null) worker.interrupt();
        super.onDestroy();
    }
}
