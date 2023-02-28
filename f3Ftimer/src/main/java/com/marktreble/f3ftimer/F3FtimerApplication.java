/*
 *     ___________ ______   _______
 *    / ____/__  // ____/  /_  __(_)___ ___  ___  _____
 *   / /_    /_ </ /_       / / / / __ `__ \/ _ \/ ___/
 *  / __/  ___/ / __/      / / / / / / / / /  __/ /
 * /_/    /____/_/        /_/ /_/_/ /_/ /_/\___/_/
 *
 * Open Source F3F timer UI and scores database
 *
 */

package com.marktreble.f3ftimer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import androidx.multidex.MultiDexApplication;
import androidx.preference.PreferenceManager;
import androidx.core.content.ContextCompat;
import android.util.TypedValue;

import com.jakewharton.processphoenix.ProcessPhoenix;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(formKey = "", // will not be used
        mailTo = "mark.treble@marktreble.co.uk",
        mode = ReportingInteractionMode.SILENT)

public class F3FtimerApplication extends MultiDexApplication {


    @Override
    public void onCreate() {
        super.onCreate();

        // The following line triggers the initialization of ACRA
        ACRA.init(this);

    }

    public static int themeAttributeToColor(int themeAttributeId,
                                            Context context,
                                            int fallbackColorId) {
        TypedValue outValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        boolean wasResolved =
                theme.resolveAttribute(
                        themeAttributeId, outValue, true);
        if (wasResolved) {
            return outValue.resourceId == 0
                    ? outValue.data
                    : ContextCompat.getColor(
                    context, outValue.resourceId);
        } else {
            // fallback colour handling
            return fallbackColorId;
        }
    }

    public void setBaseTheme(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String t = sharedPref.getString("pref_app_theme", "AppTheme");

        if (t.equals("")) t = "AppTheme";

        int s = getResources().getIdentifier(t, "style", getPackageName());
        context.setTheme(s);
    }

    public void setOverlayTheme(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String t = sharedPref.getString("pref_app_theme", "AppTheme.Overlay");

        if (t.equals("")) t = "AppTheme.Overlay";

        int s = getResources().getIdentifier(t + ".Overlay", "style", getPackageName());
        context.setTheme(s);
    }

    public void setTransparentTheme(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String t = sharedPref.getString("pref_app_theme", "AppTheme.Transparent");

        if (t.equals("")) t = "AppTheme.Transparent";

        int s = getResources().getIdentifier(t + ".Transparent", "style", getPackageName());
        context.setTheme(s);
    }

    public void restartApp() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String newtheme = sharedPref.getString("pref_app_theme", "");
        String oldtheme = sharedPref.getString("pref_prev_app_theme", "AppTheme");
        if (!newtheme.equals(oldtheme)) {
            sharedPref.edit().putString("pref_prev_app_theme", newtheme).apply();
            ProcessPhoenix.triggerRebirth(this);
        }
    }

}
