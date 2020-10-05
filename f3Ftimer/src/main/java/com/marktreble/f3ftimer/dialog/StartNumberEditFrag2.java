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
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

public class StartNumberEditFrag2 extends Fragment {

    View mView;
    private Intent mIntent;

    public StartNumberEditFrag2() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIntent = getActivity().getIntent(); // gets the previously created intent
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.start_number_frag2, container, false);

        EditText startnumber = mView.findViewById(R.id.editText1);

        startnumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Button done = mView.findViewById(R.id.button_done);
                if (setPilotPreview(s.toString().trim())) {
                    done.setEnabled(true);
                } else {
                    done.setEnabled(false);
                }
            }
        });

        startnumber.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    done();
                    return true;
                }
                return false;
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

        View pilotView = getLayoutInflater().inflate(R.layout.listrow_pickpilots, cnt);

        TextView tv = pilotView.findViewById(R.id.text1);
        TextView nm = pilotView.findViewById(R.id.number);


        nm.setText(start);
        nm.setVisibility(View.VISIBLE);
        tv.setText(name);

        return true;
    }

    private void done() {
        EditText startnumber = mView.findViewById(R.id.editText1);

        mIntent.putExtra("start_number", startnumber.getText().toString());
        getActivity().setResult(RaceActivity.RESULT_OK, mIntent);
        getActivity().finish();
    }

}

