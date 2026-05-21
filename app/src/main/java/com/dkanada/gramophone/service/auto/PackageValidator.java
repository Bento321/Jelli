package com.dkanada.gramophone.service.auto;

import android.content.Context;
import android.os.Process;

public final class PackageValidator {
    private static final String ANDROID_AUTO_PACKAGE = "com.google.android.projection.gearhead";
    private static final String ANDROID_AUTOMOTIVE_PACKAGE = "com.google.android.car.media";
    private static final String WEAR_PACKAGE = "com.google.android.wearable.app";
    private static final String ASSISTANT_PACKAGE = "com.google.android.googlequicksearchbox";

    private PackageValidator() {
    }

    public static boolean isCallerAllowed(Context context, String callerPackage, int callerUid) {
        if (callerUid == Process.SYSTEM_UID || callerUid == Process.myUid()) {
            return true;
        }
        if (callerPackage == null) return false;
        switch (callerPackage) {
            case ANDROID_AUTO_PACKAGE:
            case ANDROID_AUTOMOTIVE_PACKAGE:
            case WEAR_PACKAGE:
            case ASSISTANT_PACKAGE:
                return true;
            default:
                return callerPackage.equals(context.getPackageName());
        }
    }
}
