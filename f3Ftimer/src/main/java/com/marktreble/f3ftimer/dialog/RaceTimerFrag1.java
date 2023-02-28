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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

/* Start Screen */
public class RaceTimerFrag1 extends RaceTimerFrag {


    public RaceTimerFrag1() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.race_timer_frag2, container, false);

        Button swt = mView.findViewById(R.id.button_start_working_time);
        swt.setVisibility(View.VISIBLE);
        swt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Progress to the next UI
                next();

            }
        });

        Button ml = mView.findViewById(R.id.button_model_launched);
        ml.setVisibility(View.VISIBLE);
        ml.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                model_launched();

            }
        });

        Button ab = mView.findViewById(R.id.button_abort);
        ab.setVisibility(View.VISIBLE);
        ab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RaceTimerActivity a = (RaceTimerActivity) getActivity();
                a.sendCommand("abort");

                a.setResult(RaceActivity.RESULT_ABORTED, null);
                a.finish();

            }
        });

        super.hideStatus();
        super.setPilotName();

        return mView;
    }

    public void next() {
        RaceTimerActivity a = (RaceTimerActivity) getActivity();

        a.sendCommand("working_time");
        a.sendWind();
        a.getFragment(new RaceTimerFrag2(), 2);
    }

    public void model_launched() {
        RaceTimerActivity a = (RaceTimerActivity) getActivity();

        // Send model launched to server
        a.sendCommand("launch");

        a.getFragment(new RaceTimerFrag3(), 3);

    }

    public void startPressed() {
        next();
    }

}
