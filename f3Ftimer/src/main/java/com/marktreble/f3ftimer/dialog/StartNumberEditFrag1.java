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
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

public class StartNumberEditFrag1 extends Fragment {

    View mView;
    private Intent mIntent;
    private String mStartNumber;
    private Handler mHandler;
    private int mSz;
    private float mAnimationDelay;

    public StartNumberEditFrag1() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIntent = getActivity().getIntent(); // gets the previously created intent
        mHandler = new Handler();
        mSz = ((StartNumberEditActivity) getActivity()).mArrPilots.size();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.start_number_frag1, container, false);

        Button generate = mView.findViewById(R.id.generate_random);

        generate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAnimation();
            }
        });

        Button done = mView.findViewById(R.id.button_done);
        done.setEnabled(false);

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                done();
            }
        });

        return mView;
    }

    private void startAnimation() {
        Button done = mView.findViewById(R.id.button_done);
        done.setEnabled(false);

        mAnimationDelay = 5;
        mHandler.post(animate);
    }

    private Runnable animate = new Runnable() {
        @Override
        public void run() {
            int random = 0;
            while (!setPilotPreview(String.format("%d", random))) {
                random = (int) Math.floor(Math.random() * mSz) + 1;
            }
            mAnimationDelay *= 1.1;
            if (mAnimationDelay < 500) {
                mHandler.postDelayed(animate, (long) mAnimationDelay);
            } else {
                Button done = mView.findViewById(R.id.button_done);
                done.setEnabled(true);
            }
        }
    };

    private boolean setPilotPreview(String start) {
        ViewGroup cnt = mView.findViewById(R.id.preview);
        cnt.removeAllViews();
        if (start.equals("")) return false;

        String name = "";
        int n = Integer.parseInt(start, 10);
        if (n == 0) return false;

        for (Pilot p : ((StartNumberEditActivity) getActivity()).mArrPilots) {
            if (p.number.equals(start)) {
                name = String.format("%s %s", p.firstname, p.lastname);
            }
        }
        if (name.equals("")) return false;

        mStartNumber = start;

        View pilotView = getLayoutInflater().inflate(R.layout.listrow_pickpilots, cnt);

        TextView tv = pilotView.findViewById(R.id.text1);
        TextView nm = pilotView.findViewById(R.id.number);


        nm.setText(start);
        nm.setVisibility(View.VISIBLE);
        tv.setText(name);

        return true;
    }

    private void done() {
        mIntent.putExtra("start_number", mStartNumber);
        getActivity().setResult(RaceActivity.RESULT_OK, mIntent);
        getActivity().finish();
    }

}
