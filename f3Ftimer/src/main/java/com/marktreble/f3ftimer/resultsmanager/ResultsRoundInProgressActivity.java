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
import androidx.annotation.NonNull;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.results.Results;

import java.util.ArrayList;

public class ResultsRoundInProgressActivity extends ResultsRaceBaseActivity {

    private ArrayAdapter<String> mArrAdapter;
    private ArrayList<String> mArrNames;
    private ArrayList<String> mArrBibNumbers;
    private ArrayList<Pilot> mArrPilots;
    private ArrayList<Integer> mArrGroups;
    private ArrayList<Boolean> mFirstInGroup;

    private RaceData.Group mGroupScoring;

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

        RaceData datasource = new RaceData(this);
        datasource.open();
        Race race = datasource.getRace(mRid);
        datasource.close();

        TextView tt = findViewById(R.id.race_title);
        tt.setText(race.name);

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
        r.getRoundInProgress(this, mRid);

        mArrNames = r.mArrNames;
        mArrPilots = r.mArrPilots;
        mArrBibNumbers = r.mArrBibNumbers;
        mArrGroups = r.mArrGroups;
        mFirstInGroup = r.mFirstInGroup;
        mGroupScoring = r.mGroupScoring;

    }

    private void setList() {
        mArrAdapter = new ArrayAdapter<String>(this, R.layout.listrow_racepilots, R.id.text1, mArrNames) {
            @Override
            public @NonNull
            View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row;

                if (null == convertView) {
                    row = getLayoutInflater().inflate(R.layout.listrow_racepilots, parent, false);
                } else {
                    row = convertView;
                }

                Pilot p = mArrPilots.get(position);

                TextView p_number = row.findViewById(R.id.number);
                p_number.setText(mArrBibNumbers.get(position));

                TextView p_name = row.findViewById(R.id.text1);
                p_name.setText(mArrNames.get(position));

                Drawable flag = p.getFlag(mContext);
                if (flag != null) {
                    p_name.setCompoundDrawablesWithIntrinsicBounds(flag, null, null, null);
                    int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
                    p_name.setCompoundDrawablePadding(padding);
                }

                View group_header = row.findViewById(R.id.group_header);
                TextView group_header_label = row.findViewById(R.id.group_header_label);
                if (mGroupScoring.num_groups > 1 && mFirstInGroup.get(position)) {
                    group_header.setVisibility(View.VISIBLE);
                    group_header_label.setText(String.format(getString(R.string.group_heading), mArrGroups.get(position) + 1));
                } else {
                    group_header.setVisibility(View.GONE);
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