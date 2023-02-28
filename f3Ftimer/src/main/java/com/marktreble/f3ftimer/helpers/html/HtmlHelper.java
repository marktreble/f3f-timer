package com.marktreble.f3ftimer.helpers.html;

import android.os.Build;
import android.text.Html;
import android.text.Spanned;

public class HtmlHelper {

   /**
    * Compatibility wrapper for parsing html into attributed text (Spanned)
    *
    * This provides a normalised function across ALL API levels.
    *
    * @param html a string of html
    * @return Spanned
    */
   @SuppressWarnings("deprecation")
   static public Spanned fromHtml(String html) {
      Spanned string;
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
         string = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
      } else {
         string = Html.fromHtml(html);
      }
      return string;
   }
}
