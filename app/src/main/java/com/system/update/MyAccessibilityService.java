package com.system.update;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public class MyAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // لا نفعل شيئاً هنا، الهدف فقط إبقاء الخدمة حية
    }

    @Override
    public void onInterrupt() {
        // لا شي
    }
}
