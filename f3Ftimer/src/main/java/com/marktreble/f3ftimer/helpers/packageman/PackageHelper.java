package com.marktreble.f3ftimer.helpers.packageman;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

@SuppressWarnings("deprecation")
public class PackageHelper {
    public static PackageInfo getPackageInfo(Context context, Integer flags) throws PackageManager.NameNotFoundException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.getPackageManager()
                    .getPackageInfo(
                            context.getPackageName(),
                            PackageManager.PackageInfoFlags.of(flags)
                    );
        } else {
            return context.getPackageManager()
                    .getPackageInfo(
                            context.getPackageName(),
                            flags
                    );
        }
    }
}
