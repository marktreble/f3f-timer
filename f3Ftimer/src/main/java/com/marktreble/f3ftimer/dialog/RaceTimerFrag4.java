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
import com.marktreble.f3ftimer.constants.IComm;
import com.marktreble.f3ftimer.constants.Pref;

/* On Course */
public class RaceTimerFrag4 extends RaceTimerFrag {

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private long mStart;
    private int mLap = 0;
    private long mEstimate = 0;
    private Float mFinalTime = -1.0f;

    private boolean mClickedOnce = false;
    private boolean mStartPressed = false;

    public RaceTimerFrag4() {

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

        Button refly = mView.findViewById(R.id.button_refly);
        refly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickedOnce) return;
                mClickedOnce = true;
                mStartPressed = true;
                reflight();

            }
        });

        Button fin = mView.findViewById(R.id.button_finish);
        fin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickedOnce) return;
                mClickedOnce = true;
                mStartPressed = true;
                next();

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

        super.setStatus(getString(R.string.on_course));

        setLeg(mLap, mEstimate, 0, 0, 0, "");
        if (mFinalTime >= 0)
            setFinal(mFinalTime, 0, "");

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

            TextView cd = mView.findViewById(R.id.time);
            String str_time = String.format("%.2f", seconds);
            if (mLap > 0) {
                str_time += String.format(" T%d", mLap);
            }
            cd.setText(str_time);

            TextView min = mView.findViewById(R.id.mintime);
            min.setText(str_time);

            mHandler.postDelayed(updateClock, 10);

        }
    };

    public void setLeg(int number, long estimated, long fastestLegTime, long legTime, long deltaTime, String fastestFlightPilot) {
        RaceTimerActivity a = (RaceTimerActivity) getActivity();

        // Stop the clock here
        if (number == 10 && mFinalTime < 0) {
            long elapsed = System.currentTimeMillis() - mStart;
            mHandler.removeCallbacks(updateClock);
            TextView cd = mView.findViewById(R.id.time);
            cd.setText("");

            TextView min = mView.findViewById(R.id.mintime);
            min.setText("");

            a.sendCommand(String.format("::%.2f", (float) elapsed / 1000));
        }

        mLap = number;
        mEstimate = estimated;

        if (number > 0) {
            Intent i = new Intent(IComm.RCV_LIVE_UPDATE);
            i.putExtra("com.marktreble.f3ftimer.value.turnNumber", number);
            i.putExtra("com.marktreble.f3ftimer.value.legTime", legTime / 1000.0f);
            i.putExtra("com.marktreble.f3ftimer.value.fastestLegTime", fastestLegTime / 1000.0f);
            i.putExtra("com.marktreble.f3ftimer.value.fastestLegPilot", fastestFlightPilot);
            i.putExtra("com.marktreble.f3ftimer.value.deltaTime", deltaTime / 1000.0f);
            i.putExtra("com.marktreble.f3ftimer.value.estimatedTime", estimated / 1000.0f);
            a.sendBroadcast(i);

            TextView lap = mView.findViewById(R.id.lap);
            String str_lap = String.format("Turn: %d", number);
            lap.setText(str_lap);
        }

        if (estimated > 0) {
            TextView est = mView.findViewById(R.id.estimated);
            String str_est = String.format("Est: %.2f", (float) estimated / 1000);
            est.setText(str_est);
        }

    }

    public void setFinal(Float time, float fastestFlightTime, String fastestFlightPilot) {
        mHandler.removeCallbacks(updateClock);
        TextView cd = mView.findViewById(R.id.time);
        String str_time = String.format("%.2f", time);
        cd.setText(str_time);

        TextView min = mView.findViewById(R.id.mintime);
        min.setText(str_time);

        TextView lap = mView.findViewById(R.id.lap);
        lap.setText("");

        TextView est = mView.findViewById(R.id.estimated);
        est.setText("");

        TextView status = mView.findViewById(R.id.status);
        status.setText(getString(R.string.run_complete));

        Button abort = mView.findViewById(R.id.button_abort);
        abort.setVisibility(View.GONE);

        Button baseA = mView.findViewById(R.id.base_A);
        Button baseB = mView.findViewById(R.id.base_B);
        baseA.setVisibility(View.GONE);
        baseB.setVisibility(View.GONE);

        Button f = mView.findViewById(R.id.button_finish);
        f.setVisibility(View.VISIBLE);

        Button r = mView.findViewById(R.id.button_refly);
        r.setVisibility(View.VISIBLE);

        mFinalTime = time;

        // Start Round Timeout now
        RaceTimerActivity a = (RaceTimerActivity) getActivity();
        a.sendCommand("begin_timeout");

        /* send to ResultsServer Live Listener */
        Intent i = new Intent(IComm.RCV_LIVE_UPDATE);
        i.putExtra("com.marktreble.f3ftimer.value.fastestFlightTime", fastestFlightTime);
        i.putExtra("com.marktreble.f3ftimer.value.fastestFlightPilot", fastestFlightPilot);
        // TODO
        // State of 6? - what does that mean?
        // Should be declared as a semantically named constant to make the code readable
        i.putExtra("com.marktreble.f3ftimer.value.state", 6);
        a.sendBroadcast(i);

    }

    public void next() {
        RaceTimerActivity a = (RaceTimerActivity) getActivity();
        // Tell Driver to finalise the score
        // Driver will post back run_finalised when finished
        a.sendOrderedCommand("finalise");

    }

    public void cont() {
        RaceTimerActivity a = (RaceTimerActivity) getActivity();
        RaceTimerFragRunComplete f = new RaceTimerFragRunComplete();
        f.mFinalTime = mFinalTime;
        a.getFragment(f, 5);
    }

    public void reflight() {
        RaceTimerActivity a = (RaceTimerActivity) getActivity();
        a.reflight();

    }

    public void startPressed() {
        if (mFinalTime < 0) return; // Ignore if the race is still in progress
        mClickedOnce = true;
        if (!mStartPressed) {
            mStartPressed = true;
            next();
        }
    }
}
