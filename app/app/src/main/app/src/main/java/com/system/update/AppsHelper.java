package com.system.update;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import com.google.firebase.database.DatabaseReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppsHelper {

    public static void pushApps(Context ctx, DatabaseReference dataRef) {
        PackageManager pm = ctx.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<Map<String, Object>> list = new ArrayList<>();

        for (ApplicationInfo app : apps) {
            Map<String, Object> m = new HashMap<>();
            m.put("appName", pm.getApplicationLabel(app).toString());
            m.put("packageName", app.packageName);
            try {
                m.put("versionName", pm.getPackageInfo(app.packageName, 0).versionName);
            } catch (Exception e) {
                m.put("versionName", "?");
            }
            list.add(m);
        }
        dataRef.child("apps").setValue(list);
    }
}
