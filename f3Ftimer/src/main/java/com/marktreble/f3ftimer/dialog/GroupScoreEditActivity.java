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
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

import com.marktreble.f3ftimer.F3FtimerApplication;
import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

public class GroupScoreEditActivity extends AppCompatActivity {
    private Intent mIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((F3FtimerApplication) getApplication()).setTransparentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_score);


        final NumberPicker numGroups = findViewById(R.id.numgroups);
        setDividerColor(numGroups, F3FtimerApplication.themeAttributeToColor(
                R.attr.div,
                this,
                R.color.light_grey));

        mIntent = getIntent(); // gets the previously created intent
        int max_groups = mIntent.getIntExtra("max_groups", 1);
        int current_groups = mIntent.getIntExtra("current_groups", 1);

        numGroups.setMinValue(1);
        numGroups.setMaxValue(max_groups);
        numGroups.setValue(current_groups);

        Button done = findViewById(R.id.button_done);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIntent.putExtra("num_groups", String.format("%d", numGroups.getValue()));
                setResult(RaceActivity.RESULT_OK, mIntent);
                finish();
            }
        });
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
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (Resources.NotFoundException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }
}
