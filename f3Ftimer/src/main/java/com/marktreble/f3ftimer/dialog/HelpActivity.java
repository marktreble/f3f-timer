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

package com.marktreble.f3ftimer.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.marktreble.f3ftimer.F3FtimerApplication;
import com.marktreble.f3ftimer.R;

public class HelpActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((F3FtimerApplication)getApplication()).setOverlayTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help);

        PackageInfo pInfo;
        String v = "";
        String b = "";
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            v = pInfo.versionName;
            b = String.format("%d", pInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        TextView version = findViewById(R.id.version);
        version.setText(String.format("%s %s (Build %s)", getString(R.string.version), v, b));


    }
}
