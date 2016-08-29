package com.marktreble.f3ftimer.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

/**
 * Created by marktreble on 28/08/2016.
 */
public class RaceTimerFrag6 extends RaceTimerFrag {

    private boolean mClickedOnce = false;

    public RaceTimerFrag6(){

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.race_timer_frag6, container, false);

        Button zero = (Button) mView.findViewById(R.id.button_zero);
        zero.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickedOnce) return;
                mClickedOnce = true;
                score_zero();

            }
        });

        Button refly = (Button) mView.findViewById(R.id.button_refly);
        refly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickedOnce) return;
                mClickedOnce = true;
                reflight();

            }
        });

        super.setPilotName();

        // Start Round Timeout now
        RaceTimerActivity a = (RaceTimerActivity) getActivity();
        a.sendCommand("begin_timeout");

        return mView;
    }

    public void reflight(){
        RaceTimerActivity a = (RaceTimerActivity)getActivity();
        a.reflight();

    }

    public void score_zero(){
        RaceTimerActivity a = (RaceTimerActivity)getActivity();
        a.scorePilotZero(a.mPilot);
        a.setResult(RaceActivity.RESULT_ABORTED, null);
        a.finish();

    }
}
