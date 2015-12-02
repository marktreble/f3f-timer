/*
 * RaceTimerFrag5
 * Finalised
 */
package com.marktreble.f3ftimer.dialog;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.os.Bundle;
import android.widget.TextView;

public class RaceTimerFrag5 extends RaceTimerFrag {

    public Float mFinalTime;

    private boolean mClickedOnce = false;
    private boolean mStartPressed = false;
    public RaceTimerFrag5(){
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.race_timer_frag5, container, false);

        Button next = (Button) mView.findViewById(R.id.button_next_pilot);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickedOnce) return;
                mClickedOnce = true;
                mStartPressed = true;
                next();

            }
        });

        TextView cd = (TextView) mView.findViewById(R.id.time);
        String str_time = String.format("%.2f", mFinalTime);
        cd.setText(str_time);

        TextView status = (TextView) mView.findViewById(R.id.status);
        status.setText("Run Complete");
        super.setPilotName();

        return mView;
    }

    public void next(){
        RaceTimerActivity a = (RaceTimerActivity)getActivity();
        a.sendCommand("abort");
        a.setResult(RaceActivity.RESULT_OK);
        a.finish();
    }

    public void startPressed(){
        mClickedOnce = true;
        if (!mStartPressed) {
            mStartPressed = true;
            next();
        }
    }

}
