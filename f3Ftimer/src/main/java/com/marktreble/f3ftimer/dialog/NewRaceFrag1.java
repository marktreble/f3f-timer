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

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.marktreble.f3ftimer.R;

public class NewRaceFrag1 extends Fragment {

    private View mView;

    public NewRaceFrag1() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.race_new_frag1, container, false);

        EditText name = mView.findViewById(R.id.new_race_name);
        EditText rpf = mView.findViewById(R.id.new_race_rounds_per_flight);

        EditText offset = mView.findViewById(R.id.new_race_offset);
        offset.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    next();

                }
                return false;
            }
        });

        Button ib = mView.findViewById(R.id.button1);
        ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next();

            }
        });

        NewRaceActivity a = (NewRaceActivity) getActivity();

        if (a.name != null) name.setText(a.name);
        if (a.rpf > 0) rpf.setText(String.format("%d", a.rpf));
        if (a.offset > 0) offset.setText(String.format("%d", a.offset));


        return mView;
    }

    public void next() {
        NewRaceActivity a = (NewRaceActivity) getActivity();

        EditText name = mView.findViewById(R.id.new_race_name);
        EditText rpf = mView.findViewById(R.id.new_race_rounds_per_flight);
        EditText offset = mView.findViewById(R.id.new_race_offset);

        // Hide the keyboard
        InputMethodManager imm = (InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(offset.getWindowToken(), 0);
        }

        a.name = name.getText().toString();
        a.rpf = Integer.parseInt("0" + rpf.getText().toString());
        a.offset = Integer.parseInt("0" + offset.getText().toString());

        a.getFragment(new NewRaceFrag2(), "newracefrag2");
    }


}
