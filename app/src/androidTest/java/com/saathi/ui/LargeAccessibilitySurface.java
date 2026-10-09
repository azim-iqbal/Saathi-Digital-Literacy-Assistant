package com.saathi.ui;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

/** Standalone test-APK surface: deliberately exceeds the observation budget. */
public final class LargeAccessibilitySurface extends View {
    public LargeAccessibilitySurface(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    private final AccessibilityNodeProvider provider = new AccessibilityNodeProvider() {
        @Override
        @SuppressWarnings("deprecation")
        public AccessibilityNodeInfo createAccessibilityNodeInfo(int id) {
            if (id < HOST_VIEW_ID || id > 650) return null;
            AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
            info.setPackageName(getContext().getPackageName());
            info.setClassName("android.view.View");
            info.setVisibleToUser(true);
            info.setEnabled(true);
            info.setBoundsInScreen(new Rect(30, 100, 800, 1000));
            if (id == HOST_VIEW_ID) {
                info.setSource(LargeAccessibilitySurface.this);
                for (int index = 0; index <= 650; index++) {
                    info.addChild(LargeAccessibilitySurface.this, index);
                }
            } else {
                info.setSource(LargeAccessibilitySurface.this, id);
                info.setParent(LargeAccessibilitySurface.this);
                if (id == 0) {
                    info.setText("Help");
                    info.setClickable(true);
                }
                if (id == 650) {
                    info.setEditable(true);
                    info.setPassword(true);
                    info.setHintText("OTP");
                }
            }
            return info;
        }
    };

    @Override public AccessibilityNodeProvider getAccessibilityNodeProvider() { return provider; }
}
