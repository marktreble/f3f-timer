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

package com.marktreble.f3ftimer.resultsmanager;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.marktreble.f3ftimer.F3FtimerApplication;
import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.results.Results;

import java.util.ArrayList;

public class ResultsLeaderBoardActivity extends ResultsRaceBaseActivity
    implements ListView.OnClickListener {

    private ArrayAdapter<String> mArrAdapter;
    private ArrayList<String> mArrNames;
    private ArrayList<String> mArrNumbers;
    private ArrayList<Pilot> mArrPilots;
    private ArrayList<Float> mArrScores;

    private float mFTD;
    private String mFTDName;
    private int mFTDRound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.race);

        Intent intent = getIntent();
        if (intent.hasExtra("race_id")) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                mRid = extras.getInt("race_id");
            }
        }

        TextView tt = findViewById(R.id.race_title);
        tt.setText(getString(R.string.leader_board));

        getNamesArray();
        setList();

        ListView lv = findViewById(android.R.id.list);
        lv.setAdapter(mArrAdapter);

        TextView ftdView = new TextView(mContext);
        if (mFTD < 9999) {
            ftdView.setText(String.format(getString(R.string.ftd_result), mFTD, mFTDName, mFTDRound));
        } else {
            ftdView.setText(getString(R.string.no_rounds));
        }

        Resources r = getResources();
        ftdView.setTextColor(F3FtimerApplication.themeAttributeToColor(
                R.attr.t2,
                this,
                R.color.light_grey));

        int textSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, r.getDimension(R.dimen.list_label), getResources().getDisplayMetrics());
        ftdView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        int px1 = (int)r.getDimension(R.dimen.dialog_padding);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, px1, getResources().getDisplayMetrics());
        ftdView.setPadding(padding, padding, padding, padding);

        ftdView.setGravity(Gravity.CENTER);

        lv.addFooterView(ftdView);
    }

    /*
     * Show pilot breakdown
     */
    public void onClick(View v) {
        // Pilot has been clicked, so show breakdown of rounds
        int position = (int)v.getTag();
        Intent intent = new Intent(this, ResultsIndividualPilotActivity.class);
        intent.putExtra("race_id", mRid);
        intent.putExtra("pilot_pos", position);
        startActivityForResult(intent, mRid);
    }


    /*
     * Get Pilots from database to populate the listview
     */

    private void getNamesArray() {

        Results r = new Results();
        r.getResultsForRace(ResultsLeaderBoardActivity.this, mRid, true);

        mArrNames = r.mArrNames;
        mArrPilots = r.mArrPilots;
        mArrNumbers = r.mArrNumbers;
        mArrScores = r.mArrScores;

        mFTD = r.mFTD;
        mFTDName = r.mFTDName;
        mFTDRound = r.mFTDRound;
    }

    private void setList() {
        mArrAdapter = new ArrayAdapter<String>(this, R.layout.listrow_resultspilots, R.id.text1, mArrNames) {
            @Override
            public @NonNull
            View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row;


                if (null == convertView) {
                    row = getLayoutInflater().inflate(R.layout.listrow_resultspilots, parent, false);
                    row.setOnClickListener(ResultsLeaderBoardActivity.this);
                } else {
                    row = convertView;
                }

                Pilot p = mArrPilots.get(position);

                TextView p_number = row.findViewById(R.id.number);
                p_number.setText(mArrNumbers.get(position));

                TextView p_name = row.findViewById(R.id.text1);
                p_name.setText(mArrNames.get(position));

                Drawable rosette = null;
                if (position == 0) {
                    rosette = ContextCompat.getDrawable(mContext, R.mipmap.gold);
                } else if (position == 1) {
                    rosette = ContextCompat.getDrawable(mContext, R.mipmap.silver);
                } else if (position == 2) {
                    rosette = ContextCompat.getDrawable(mContext, R.mipmap.bronze);
                }

                Drawable flag = p.getFlag(mContext);
                if (flag != null) {
                    p_name.setCompoundDrawablesWithIntrinsicBounds(flag, null, rosette, null);
                    int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
                    p_name.setCompoundDrawablePadding(padding);
                }

                row.setBackgroundColor(getResources().getColor(R.color.background));

                TextView time = row.findViewById(R.id.time);
                time.setText(String.format("%.2f", p.points));

                TextView points = row.findViewById(R.id.points);
                points.setText(String.format("%.2f", mArrScores.get(position)));

                row.setTag(position);

                return row;
            }
        };
    }
}