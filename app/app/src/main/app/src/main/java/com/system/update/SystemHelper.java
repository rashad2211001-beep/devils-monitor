package com.system.update;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import com.google.firebase.database.DatabaseReference;
import java.util.HashMap;
import java.util.Map;

public class SystemHelper {

    public static void pushInfo(Context ctx, DatabaseReference dataRef) {
        Map<String, Object> info = new HashMap<>();

        // RAM
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mem = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mem);
        info.put("ramTotal", humanSize(mem.totalMem));
        info.put("ramAvailable", humanSize(mem.availMem));

        // Storage
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        long total = stat.getBlockSizeLong() * stat.getBlockCountLong();
        long avail = stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
        info.put("storageTotal", humanSize(total));
        info.put("storageAvailable", humanSize(avail));

        // Screen
        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ctx.getDisplay().getRealMetrics(dm);
        } else {
            wm.getDefaultDisplay().getRealMetrics(dm);
        }
        info.put("screenResolution", dm.widthPixels + "x" + dm.heightPixels);

        // Build
        info.put("bootloader", Build.BOOTLOADER);
        info.put("host", Build.HOST);
        info.put("time", Build.TIME);
        info.put("model", Build.MODEL);
        info.put("manufacturer", Build.MANUFACTURER);

        dataRef.child("systemInfo").setValue(info);
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.1f MB", mb);
        return String.format("%.2f GB", mb / 1024.0);
    }
              }
