package com.marktreble.f3ftimer.helpers.locale;

import android.content.Context;
import android.os.Build;

import java.util.Locale;

@SuppressWarnings("deprecation")
public class LocaleHelper {

   /**
    * Compatibility wrapper for getting current Locale
    *
    * This provides a normalised function across ALL API levels.
    *
    * @param context Context
    * @return Locale
    */
   static public Locale getLocale(Context context) {
      Locale locale;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
         locale = context.getResources().getConfiguration().getLocales().get(0);
      } else {
         locale = context.getResources().getConfiguration().locale;
      }
      return locale;
   }

   /**
    * Compatibility wrapper for setting current Locale
    *
    * This provides a normalised function across ALL API levels.
    * (Older API now removed, but maintained for possible future use)
    *
    * @param context Context
    * @param locale Locale to set
    */
   static public void setLocale(Context context, Locale locale) {
      context.getResources().getConfiguration().setLocale(locale);
   }
}
