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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.marktreble.f3ftimer.F3FtimerApplication;
import com.marktreble.f3ftimer.R;

/**
 * Created by marktreble on 27/08/2017.
 */

public class F3xvaultAPILoginActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((F3FtimerApplication)getApplication()).setTransparentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.f3xvault_api_login);

        EditText username = findViewById(R.id.username);
        EditText password = findViewById(R.id.password);
        CheckBox remember = findViewById(R.id.remember);

        Button login = findViewById(R.id.login);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });


        password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    return login();
                }
                return false;
            }
        });

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean api_remember = sharedPref.getBoolean("pref_f3xv_api_details_remember", true);
        remember.setChecked(api_remember);
        if (api_remember) {
            String str_username = sharedPref.getString("pref_f3xv_api_username", "");
            String str_password = sharedPref.getString("pref_f3xv_api_password", "");

            username.setText(str_username);
            password.setText(str_password);
        }

    }

    private boolean login() {
        // Get entered data, and save to/update database
        EditText username = findViewById(R.id.username);
        EditText password = findViewById(R.id.password);
        CheckBox remember = findViewById(R.id.remember);

        String str_username = username.getText().toString();
        String str_password = password.getText().toString();

        boolean api_remember = remember.isChecked();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("pref_f3xv_api_details_remember", api_remember);

        if (api_remember) {
            // Save details to user Prefs
            editor.putString("pref_f3xv_api_username", str_username);
            editor.putString("pref_f3xv_api_password", str_password);

        } else {
            editor.putString("pref_f3xv_api_username", "");
            editor.putString("pref_f3xv_api_password", "");

        }
        editor.apply();

        if (str_username.equals("")
                || str_password.equals("")) {
            return false;
        }

        Intent intent = new Intent();
        intent.putExtra("username", str_username);
        intent.putExtra("password", str_password);
        setResult(RESULT_OK, intent);
        finish();

        return true;
    }
}
