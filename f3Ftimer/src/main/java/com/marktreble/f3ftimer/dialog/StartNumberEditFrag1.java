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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.constants.IComm;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

public class StartNumberEditFrag1 extends Fragment {

    View mView;
    private Intent mIntent;
    private Activity mActivity;

    public StartNumberEditFrag1() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIntent = getActivity().getIntent(); // gets the previously created intent
        mActivity = getActivity();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (((StartNumberEditActivity) getActivity()).mArrPilots != null) {
            setPilotPreview();
        }
        mActivity.registerReceiver(onBroadcast, new IntentFilter(IComm.RCV_UPDATE));
    }

    @Override
    public void onPause() {
        super.onPause();

        mActivity.unregisterReceiver(onBroadcast);
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
                ((StartNumberEditActivity) getActivity()).startRandomAnimation();
                Button done = mView.findViewById(R.id.button_done);
                done.setEnabled(false);

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

    private void setPilotPreview() {
        if (getActivity() == null) return;

        String start = ((StartNumberEditActivity) getActivity()).mStartNumber;
        String name = ((StartNumberEditActivity) getActivity()).mStartName;
        Button done = mView.findViewById(R.id.button_done);

        ViewGroup cnt = mView.findViewById(R.id.preview);
        cnt.removeAllViews();

        if (start == null) return;

        done.setEnabled(!start.equals(""));

        View pilotView = getLayoutInflater().inflate(R.layout.listrow_pickpilots, cnt);

        TextView tv = pilotView.findViewById(R.id.text1);
        TextView nm = pilotView.findViewById(R.id.number);


        nm.setText(start);
        nm.setVisibility(View.VISIBLE);
        tv.setText(name);
    }

    private void done() {
        mIntent.putExtra("start_number", ((StartNumberEditActivity) getActivity()).mStartNumber);
        getActivity().setResult(RaceActivity.RESULT_OK, mIntent);
        getActivity().finish();
    }

    private BroadcastReceiver onBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(IComm.MSG_UI_UPDATE)) {
                Bundle extras = intent.getExtras();
                if (extras == null) {
                    return;
                }
                String data = extras.getString(IComm.MSG_UI_UPDATE, "");


                if (data.equals("random_start_number")) {
                    setPilotPreview();
                }
            }
        }
    };

}
