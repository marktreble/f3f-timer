package com.marktreble.f3ftimer.resultsmanager;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.marktreble.f3ftimer.F3FtimerApplication;
import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.results.Results;

import java.util.ArrayList;

public class ResultsIndividualPilotActivity extends ResultsRaceBaseActivity {

    private int mPilotPos;
    private ArrayAdapter<String> mArrAdapter;
    private ArrayList<String> mArrTimes;
    private ArrayList<Integer> mArrPenalties;
    private ArrayList<Boolean> mArrDiscards;
    private ArrayList<String> mArrScores;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.race);

        Intent intent = getIntent();
        if (intent.hasExtra("race_id")) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                mRid = extras.getInt("race_id");
                mPilotPos = extras.getInt("pilot_pos");
            }
        }

        Results r = new Results();
        r.getResultsForRace(ResultsIndividualPilotActivity.this, mRid, true);

        Pilot p = r.mArrPilots.get(mPilotPos);

        TextView tt = findViewById(R.id.race_title);
        tt.setText(String.format("%s %s", p.firstname, p.lastname));

        getTimesArray(r);
        setList();

        ListView lv = findViewById(android.R.id.list);
        lv.setAdapter(mArrAdapter);
    }

    private void getTimesArray(Results r) {
        mArrTimes = new ArrayList<>();
        mArrScores = new ArrayList<>();
        mArrDiscards = new ArrayList<>();
        mArrPenalties = new ArrayList<>();
        for (int rnd = 0; rnd < r.mArrGroupings.size(); rnd++) {
            int numgroups;
            RaceData.Group grouping = r.mArrGroupings.get(rnd);
            numgroups = grouping.num_groups;

            for (int g = 0; g < numgroups; g++) {
                ArrayList<RaceData.Time> times = r.mArrTimes.get(mPilotPos);
                if (g == times.get(rnd).group) {

                    String s_time = String.format("%.2f", times.get(rnd).time);
                    mArrTimes.add(s_time);

                    int pen = times.get(rnd).penalty;
                    mArrPenalties.add(pen);

                    // Results class removes penalties from the score.
                    // We don't want to show that here, so add them back on
                    String s_points = String.format("%.2f", (times.get(rnd).points >= 0) ? times.get(rnd).points + (pen * 100) : 0);
                    mArrScores.add(s_points);

                    mArrDiscards.add(times.get(rnd).discarded);
                }
            }
        }
    }

    private void setList() {
        final int normal = F3FtimerApplication.themeAttributeToColor(
                R.attr.t3,
                this,
                R.color.text1);

        mArrAdapter = new ArrayAdapter<String>(this, R.layout.listrow, mArrTimes) {
            @Override
            public @NonNull
            View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row;

                if (null == convertView) {
                    row = getLayoutInflater().inflate(R.layout.listrow_resultspilots, parent, false);
                } else {
                    row = convertView;
                }

                row.setTag(position);

                TextView tv = row.findViewById(R.id.text1);
                tv.setText(String.format("Round %d", position + 1));

                TextView penalty = row.findViewById(R.id.penalty);
                int penalties = mArrPenalties.get(position);
                penalty.setText(penalties > 0 ? String.format("P%d", penalties) : "");

                TextView time = row.findViewById(R.id.time);
                time.setText(mArrTimes.get(position));

                TextView points = row.findViewById(R.id.points);
                points.setText(mArrScores.get(position));

                if (mArrDiscards.get(position)) {
                    tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    tv.setTextColor(ContextCompat.getColor(mContext, R.color.red));
                    time.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    time.setTextColor(ContextCompat.getColor(mContext, R.color.red));
                    points.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    points.setTextColor(ContextCompat.getColor(mContext, R.color.red));
                } else {
                    tv.setPaintFlags(tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    tv.setTextColor(normal);
                    time.setPaintFlags(tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    time.setTextColor(normal);
                    points.setPaintFlags(tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    points.setTextColor(normal);
                }

                return row;
            }
        };
    }
}
