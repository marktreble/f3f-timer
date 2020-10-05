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

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.marktreble.f3ftimer.F3FtimerApplication;
import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

public class TimeEntryActivity extends AppCompatActivity {

    Integer mPid = 0;
    Integer mRound = 0;
    private Intent mIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((F3FtimerApplication) getApplication()).setTransparentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.time_entry);

        mIntent = getIntent(); // gets the previously created intent
        if (mIntent.hasExtra("pilot_id")) {
            Bundle extras = mIntent.getExtras();
            if (extras == null) {
                return;
            }

            mPid = extras.getInt("pilot_id");
            mRound = extras.getInt("round");

            EditText time = findViewById(R.id.editText1);


            time.setOnEditorActionListener(new OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        // Get entered data, and save to/update database
                        EditText time = findViewById(R.id.editText1);

                        mIntent.putExtra("time", time.getText().toString());
                        mIntent.putExtra("pilot", mPid);
                        mIntent.putExtra("round", mRound);
                        setResult(RaceActivity.RESULT_OK, mIntent);
                        finish();
                        return true;
                    }
                    return false;
                }
            });
        }
    }

}
