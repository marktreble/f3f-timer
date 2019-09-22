package com.marktreble.f3ftimer.dialog;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
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


        final NumberPicker numGroups = (NumberPicker) findViewById(R.id.numgroups);

        mIntent = getIntent(); // gets the previously created intent
        int max_groups = mIntent.getIntExtra("max_groups", 1);
        int current_groups = mIntent.getIntExtra("current_groups", 1);

        numGroups.setMinValue(1);
        numGroups.setMaxValue(max_groups);
        numGroups.setValue(current_groups);

        Button done = (Button) findViewById(R.id.button_done);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIntent.putExtra("num_groups", String.format("%d", numGroups.getValue()));
                setResult(RaceActivity.RESULT_OK, mIntent);
                finish();
            }
        });
        /*
        EditText groups = (EditText) findViewById(R.id.editText1);
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
        */

    }
}
