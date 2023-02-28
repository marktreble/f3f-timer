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
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

import com.marktreble.f3ftimer.F3FtimerApplication;
import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

public class GroupScoreEditActivity extends AppCompatActivity {
    private Intent mIntent;
    private NumberPicker mNumGroups;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((F3FtimerApplication) getApplication()).setTransparentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_score);


        mNumGroups = findViewById(R.id.numgroups);
        setDividerColor(mNumGroups, F3FtimerApplication.themeAttributeToColor(
                R.attr.div,
                this,
                R.color.light_grey));

        mIntent = getIntent(); // gets the previously created intent
        int max_groups = mIntent.getIntExtra("max_groups", 1);
        if (max_groups<1) max_groups = 1;

        int current_groups;
        if (savedInstanceState == null) {
            current_groups = mIntent.getIntExtra("current_groups", 1);
            if (current_groups < 1) current_groups = 1;

        } else {
            current_groups = savedInstanceState.getInt("current_groups");
        }

        mNumGroups.setMinValue(1);
        mNumGroups.setMaxValue(max_groups);
        mNumGroups.setValue(current_groups);

        Button done = findViewById(R.id.button_done);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIntent.putExtra("num_groups", String.format("%d", mNumGroups.getValue()));
                setResult(RaceActivity.RESULT_OK, mIntent);
                finish();
            }
        });
    }

    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("current_groups", mNumGroups.getValue());
    }

    // https://stackoverflow.com/questions/24233556/changing-numberpicker-divider-color
    private void setDividerColor(NumberPicker picker, int color) {

        java.lang.reflect.Field[] pickerFields = NumberPicker.class.getDeclaredFields();
        for (java.lang.reflect.Field pf : pickerFields) {
            if (pf.getName().equals("mSelectionDivider")) {
                pf.setAccessible(true);
                try {
                    ColorDrawable colorDrawable = new ColorDrawable(color);
                    pf.set(picker, colorDrawable);
                } catch (IllegalArgumentException | Resources.NotFoundException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }
}
