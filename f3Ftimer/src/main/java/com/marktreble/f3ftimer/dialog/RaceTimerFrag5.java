/*
 * RaceTimerFrag5
 * Finalised
 */
package com.marktreble.f3ftimer.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

public class RaceTimerFrag5 extends RaceTimerFrag {

    public Float mFinalTime;

    private boolean mClickedOnce = false;
    private boolean mStartPressed = false;

    public RaceTimerFrag5() {
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

        Button cancel = mView.findViewById(R.id.button_cancel);
        cancel.setVisibility(View.VISIBLE);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                quit();

            }
        });

        Button next = mView.findViewById(R.id.button_next_pilot);
        next.setVisibility(View.VISIBLE);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickedOnce) return;
                mClickedOnce = true;
                mStartPressed = true;
                next();

            }
        });

        TextView cd = mView.findViewById(R.id.time);
        String str_time = String.format("%.2f", mFinalTime);
        cd.setText(str_time);

        TextView min = mView.findViewById(R.id.mintime);
        min.setText(str_time);

        TextView status = mView.findViewById(R.id.status);
        status.setText(getString(R.string.run_complete));

        super.setPilotName();

        if (((RaceTimerActivity) getActivity()).mWindowState == RaceTimerActivity.WINDOW_STATE_MINIMIZED) {
            setMinimized();
        }


        return mView;
    }

    private void quit() {
        RaceTimerActivity a = (RaceTimerActivity) getActivity();
        a.finish();
    }

    private void next() {
        RaceTimerActivity a = (RaceTimerActivity) getActivity();
        a.sendCommand("abort");
        a.setResult(RaceActivity.RESULT_OK);
        a.finish();
    }

    public void startPressed() {
        mClickedOnce = true;
        if (!mStartPressed) {
            mStartPressed = true;
            next();
        }
    }

}
