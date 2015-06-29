package com.marktreble.f3ftimer.dialog;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

/**
 * Created by marktreble on 22/12/14.
 */
public class GroupScoreEditActivity extends Activity {
    Integer mPid = 0;
    private Intent mIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_score);

        EditText groups = (EditText) findViewById(R.id.editText1);

        mIntent= getIntent(); // gets the previously created intent
        groups.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    EditText groups = (EditText) findViewById(R.id.editText1);

                    mIntent.putExtra("num_groups", groups.getText().toString());
                    setResult(RaceActivity.RESULT_OK, mIntent);
                    finish();
                    return true;
                }
                return false;
            }
        });

    }
}
