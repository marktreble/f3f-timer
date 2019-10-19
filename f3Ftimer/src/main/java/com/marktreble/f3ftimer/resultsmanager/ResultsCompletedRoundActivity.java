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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.marktreble.f3ftimer.BaseActivity;
import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.results.Results;
import com.marktreble.f3ftimer.dialog.AboutActivity;
import com.marktreble.f3ftimer.dialog.HelpActivity;
import com.marktreble.f3ftimer.pilotmanager.PilotsActivity;
import com.marktreble.f3ftimer.racemanager.RaceListActivity;

import java.util.ArrayList;

public class ResultsCompletedRoundActivity extends ResultsRaceBaseActivity {

    private ArrayAdapter<String> mArrAdapter;
    private ArrayList<String> mArrNames;
    private ArrayList<String> mArrNumbers;
    private ArrayList<Pilot> mArrPilots;
    private ArrayList<Integer> mArrGroups;

    private Integer mNumGroups;
    private Integer mRound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.race);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (intent.hasExtra("race_id")) {
                mRid = extras.getInt("race_id");
            }
            if (intent.hasExtra("round_id")) {
                mRound = extras.getInt("round_id");
            }
        }

        RaceData datasource = new RaceData(this);
        datasource.open();
        RaceData.Group groups = datasource.getGroup(mRid, mRound);
        datasource.close();

        mNumGroups = groups.num_groups;

        String group_scored = "";
        if (mNumGroups > 1) {
            group_scored = String.format(getString(R.string.ttl_round_groups), groups.num_groups);
        }
        TextView tt = findViewById(R.id.race_title);
        tt.setText(String.format(getString(R.string.ttl_round_number_groups), mRound, group_scored));

        getNamesArray();
        setList();

        ListView lv = findViewById(android.R.id.list);
        lv.setAdapter(mArrAdapter);
    }

    /*
     * Get Pilots from database to populate the listview
     */

    private void getNamesArray() {
        Results r = new Results();
        r.getResultsForCompletedRound(this, mRid, mRound);

        mArrNames = r.mArrNames;
        mArrPilots = r.mArrPilots;
        mArrNumbers = r.mArrNumbers;
        mArrGroups = r.mArrGroups;
    }

    private void setList() {
        mArrAdapter = new ArrayAdapter<String>(this, R.layout.listrow_resultspilots, R.id.text1, mArrNames) {
            @Override
            public @NonNull
            View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row;

                if (null == convertView) {
                    row = getLayoutInflater().inflate(R.layout.listrow_resultspilots, parent, false);
                } else {
                    row = convertView;
                }

                Pilot p = mArrPilots.get(position);

                TextView p_number = row.findViewById(R.id.number);
                p_number.setText(mArrNumbers.get(position));

                TextView p_group = row.findViewById(R.id.group);
                if (mNumGroups > 1) {
                    p_group.setVisibility(View.VISIBLE);
                    p_group.setText(String.format("%d", mArrGroups.get(position) + 1));
                } else {
                    p_group.setVisibility(View.GONE);
                }
                //p_number.setText(p.position);

                TextView p_name = row.findViewById(R.id.text1);
                p_name.setText(mArrNames.get(position));

                Drawable flag = p.getFlag(mContext);
                if (flag != null) {
                    p_name.setCompoundDrawablesWithIntrinsicBounds(flag, null, null, null);
                    int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
                    p_name.setCompoundDrawablePadding(padding);
                }


                TextView time = row.findViewById(R.id.time);
                if (p.time == 0 && !p.flown) {
                    time.setText(getResources().getString(R.string.notime));
                } else {
                    time.setText(String.format("%.2f", p.time));
                }

                TextView points = row.findViewById(R.id.points);
                if (p.flown || p.status == Pilot.STATUS_RETIRED) {
                    points.setText(String.format("%.2f", p.points));
                } else {
                    points.setText("");
                }

                TextView penalty = row.findViewById(R.id.penalty);
                if (p.penalty > 0) {
                    penalty.setText(String.format(getResources().getString(R.string.penalty), p.penalty));
                } else {
                    penalty.setText(getResources().getString(R.string.empty));
                }

                return row;
            }
        };
    }
}