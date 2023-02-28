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
import android.os.Handler;
import androidx.fragment.app.Fragment;

import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

public class RaceRoundTimeoutFrag extends Fragment {

    View mView;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mHandler.post(updateClock);
        } else {
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
        mView = inflater.inflate(R.layout.race_round_timeout_frag, container, false);


        Button resume = mView.findViewById(R.id.button_resume);
        resume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.removeCallbacks(updateClock);
                // Progress to the next UI
                RaceRoundTimeoutActivity a = (RaceRoundTimeoutActivity) getActivity();
                a.setResult(RaceActivity.RESULT_OK, null);
                a.finish();
            }
        });


        return mView;
    }

    private Runnable updateClock = new Runnable() {
        public void run() {
            RaceRoundTimeoutActivity a = (RaceRoundTimeoutActivity) getActivity();

            long elapsed = Math.min(System.currentTimeMillis() - a.mStart, 30 * 60 * 1000);

            float fseconds = (float) ((60 * 30) - (elapsed / 1000));

            int minutes = (int) Math.floor(fseconds / 60);
            int seconds = (int) (fseconds - (minutes * 60));

            TextView cd = mView.findViewById(R.id.countdown);
            String str_time = String.format("%d:%02d", minutes, seconds);
            cd.setText(str_time);

            if (fseconds <= 0) {
                // Round Timeout
                a.getFragment(new RaceRoundTimeoutCompleteFrag(), 2);

            } else {
                mHandler.postDelayed(updateClock, 10);
            }

        }
    };
}
