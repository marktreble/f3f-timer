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
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

public class RaceRoundTimeoutCompleteFrag extends Fragment {

    View mView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.race_round_timeout_complete_frag, container, false);


        Button scrub = mView.findViewById(R.id.button_scrub);

        RaceRoundTimeoutActivity a = (RaceRoundTimeoutActivity) getActivity();
        if (a.mGroupScored)
            scrub.setText(getResources().getString(R.string.scrub_group));

        scrub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Progress to the next UI
                RaceRoundTimeoutActivity a = (RaceRoundTimeoutActivity) getActivity();
                a.setResult(RaceActivity.ROUND_SCRUBBED, a.mIntent);
                a.finish();
            }
        });

        Button ignore = mView.findViewById(R.id.button_ignore);
        ignore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Progress to the next UI
                RaceRoundTimeoutActivity a = (RaceRoundTimeoutActivity) getActivity();
                a.setResult(RaceActivity.RESULT_ABORTED, a.mIntent);
                a.finish();
            }
        });

        TextView cd = mView.findViewById(R.id.countdown);
        String str_time = String.format("%d:%02d", 0, 0);
        cd.setText(str_time);

        return mView;
    }
}
