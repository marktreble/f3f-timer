package com.marktreble.f3ftimer;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;

public class BaseActivity extends AppCompatActivity {

    protected Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((F3FtimerApplication) getApplication()).setBaseTheme(this);
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        getSupportActionBar().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.gradient_background));
        getSupportActionBar().setHomeAsUpIndicator(R.mipmap.ic_launcher);// set drawable icon
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        int t1 = F3FtimerApplication.themeAttributeToColor(
                R.attr.t1,
                this,
                R.color.light_grey);
        String hexColour = String.format("#%06X", (0xFFFFFF & Color.argb(0, Color.red(t1), Color.green(t1), Color.blue(t1))));
        getSupportActionBar().setTitle(Html.fromHtml("<font color=\"" + hexColour + "\">" + getString(R.string.app_name) + "</font>"));

        mContext = this;
    }
}
