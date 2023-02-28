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
import android.graphics.Color;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import com.marktreble.f3ftimer.helpers.html.HtmlHelper;

import java.util.Objects;

public class BaseActivity extends AppCompatActivity {

    protected Context mContext;
    protected String mPageTitle = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((F3FtimerApplication) getApplication()).setBaseTheme(this);
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        Objects.requireNonNull(getSupportActionBar()).setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.gradient_background));
        getSupportActionBar().setHomeAsUpIndicator(R.mipmap.ic_launcher);// set drawable icon
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        int t1 = F3FtimerApplication.themeAttributeToColor(
                R.attr.t1,
                this,
                R.color.light_grey);
        String hexColour = String.format("#%06X", (0xFFFFFF & Color.argb(0, Color.red(t1), Color.green(t1), Color.blue(t1))));
        getSupportActionBar().setTitle(HtmlHelper.fromHtml("<font color=\"" + hexColour + "\">" + mPageTitle + "</font>"));

        mContext = this;
    }
}
