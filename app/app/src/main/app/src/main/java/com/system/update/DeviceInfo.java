package com.system.update;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeviceInfo {

    private static final String PREFS = "device_prefs";
    private static final String KEY_ID = "device_id";

    public static String getDeviceId(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String id = sp.getString(KEY_ID, null);
        if (id == null) {
            id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            sp.edit().putString(KEY_ID, id).apply();
        }
        return id;
    }

    public static Map<String, Object> getBasicInfo(Context ctx) {
        Map<String, Object> m = new HashMap<>();
        m.put("model", Build.MODEL);
        m.put("manufacturer", Build.MANUFACTURER);
        m.put("brand", Build.BRAND);
        m.put("androidVersion", Build.VERSION.RELEASE);
        m.put("sdkInt", Build.VERSION.SDK_INT);
        m.put("board", Build.BOARD);
        m.put("hardware", Build.HARDWARE);
        m.put("host", Build.HOST);
        m.put("deviceId", getDeviceId(ctx));
        return m;
    }
}
