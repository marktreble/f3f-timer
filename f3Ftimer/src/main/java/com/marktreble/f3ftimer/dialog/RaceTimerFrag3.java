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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.preference.PreferenceManager;

import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.constants.Pref;

/* Model Launched */
public class RaceTimerFrag3 extends RaceTimerFrag {

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private long mStart;

    public RaceTimerFrag3() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            mStart = System.currentTimeMillis();
            mHandler.postDelayed(updateClock, 10);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(updateClock);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.race_timer_frag2, container, false);

        Button ab = mView.findViewById(R.id.button_abort);
        ab.setVisibility(View.VISIBLE);
        ab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.removeCallbacks(updateClock);
                RaceTimerActivity a = (RaceTimerActivity) getActivity();
                a.sendCommand("abort");
                a.sendCommand("begin_timeout");
                a.getFragment(new RaceTimerFragAborted(getString(R.string.race_aborted)), 6); // Abort submenu (reflight or score 0)
            }
        });

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String soft_buttons = sharedPref.getString(Pref.INPUT_SRC, getString(R.string.Demo));
        if (soft_buttons.equals(getString(R.string.Demo))) {
            Button baseA = mView.findViewById(R.id.base_A);
            baseA.setVisibility(View.VISIBLE);

            baseA.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RaceTimerActivity a = (RaceTimerActivity) getActivity();
                    a.sendCommand("baseA");
                }
            });

            Button baseB = mView.findViewById(R.id.base_B);
            baseB.setVisibility(View.VISIBLE);

            baseB.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RaceTimerActivity a = (RaceTimerActivity) getActivity();
                    a.sendCommand("baseB");
                }
            });

        }

        super.setStatus(getString(R.string.model_launched));
        super.setPilotName();

        if (((RaceTimerActivity) getActivity()).mWindowState == RaceTimerActivity.WINDOW_STATE_MINIMIZED) {
            setMinimized();
        }

        return mView;
    }

    private Runnable updateClock = new Runnable() {
        public void run() {
            long elapsed = System.currentTimeMillis() - mStart;
            float seconds = (float) elapsed / 1000;
            if (seconds > 30) seconds = 30;

            TextView cd = mView.findViewById(R.id.time);
            String str_time = String.format("%.2f", 30 - seconds);
            cd.setText(str_time);

            TextView min = mView.findViewById(R.id.mintime);
            min.setText(str_time);

            int s = (int) Math.floor(seconds);
            RaceTimerActivity a = (RaceTimerActivity) getActivity();


            if (s == 4) a.sendCommand("25");
            if (s == 9) a.sendCommand("20");
            if (s == 14) a.sendCommand("15");
            if (s == 17) a.sendCommand("12");
            if (s == 19) a.sendCommand("10");
            if (s == 20) a.sendCommand("9");
            if (s == 21) a.sendCommand("8");
            if (s == 22) a.sendCommand("7");
            if (s == 23) a.sendCommand("6");
            if (s == 24) a.sendCommand("5");
            if (s == 25) a.sendCommand("4");
            if (s == 26) a.sendCommand("3");
            if (s == 27) a.sendCommand("2");
            if (s == 28) a.sendCommand("1");

            if (s == 30) {
                // Runout of climbout time
                // Force the server to start the clock
                a.sendCommand("0"); // Informs the driver that this was a late entry
                next();

            } else {
                mHandler.postDelayed(updateClock, 10);
            }

        }
    };

    public void setOffCourse() {
        super.setStatus(getString(R.string.off_course));
    }

    public void next() {
        mHandler.removeCallbacks(updateClock);
        RaceTimerActivity a = (RaceTimerActivity) getActivity();
        a.getFragment(new RaceTimerFrag4(), 4);
    }

    public void startPressed() {
        // Ignore
    }

}
