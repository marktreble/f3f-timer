package com.marktreble.f3ftimer.helpers.resources;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

@SuppressWarnings("deprecation")
public class ResourcesHelper {
   public static Resources updateConfiguration(Context context, Configuration config) {
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
           context = context.createConfigurationContext(config);
       } else {
           context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
       }
       return context.getResources();
   }

   public static Configuration getConfiguration(Context context) {
       return context.getResources().getConfiguration();
   }
}
