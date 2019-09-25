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

public class RaceTimerFrag6 extends RaceTimerFrag {

    private boolean mClickedOnce = false;

    public RaceTimerFrag6() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.race_timer_frag2, container, false);

        Button zero = mView.findViewById(R.id.button_zero);
        zero.setVisibility(View.VISIBLE);
        zero.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickedOnce) return;
                mClickedOnce = true;
                score_zero();

            }
        });

        Button refly = mView.findViewById(R.id.button_refly);
        refly.setVisibility(View.VISIBLE);
        refly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickedOnce) return;
                mClickedOnce = true;
                reflight();

            }
        });

        super.setPilotName();

        if (((RaceTimerActivity) getActivity()).mWindowState == RaceTimerActivity.WINDOW_STATE_MINIMIZED) {
            setMinimized();
        }

        // Start Round Timeout now
        RaceTimerActivity a = (RaceTimerActivity) getActivity();
        a.sendCommand("begin_timeout");

        return mView;
    }

    public void reflight() {
        RaceTimerActivity a = (RaceTimerActivity) getActivity();
        a.reflight();

    }

    public void score_zero() {
        RaceTimerActivity a = (RaceTimerActivity) getActivity();
        a.scorePilotZero(a.mPilot);
        a.setResult(RaceActivity.RESULT_ABORTED, null);
        a.finish();

    }
}
