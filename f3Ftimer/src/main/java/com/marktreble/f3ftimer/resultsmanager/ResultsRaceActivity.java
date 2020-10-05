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
import android.os.Bundle;

import androidx.annotation.NonNull;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;

import java.util.ArrayList;

public class ResultsRaceActivity extends ResultsRaceBaseActivity
    implements ListView.OnClickListener {

    ArrayAdapter<String> mArrAdapter;
    ArrayList<String> mOptions;



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

        getNamesArray();
        setList();

        ListView lv = findViewById(android.R.id.list);
        lv.setAdapter(mArrAdapter);
    }

    private void getNamesArray() {
        RaceData datasource = new RaceData(this);
        datasource.open();
        Race race = datasource.getRace(mRid);
        datasource.close();

        TextView tt = findViewById(R.id.race_title);
        tt.setText(race.name);

        mOptions = new ArrayList<>();
        mOptions.add(String.format(getString(R.string.ttl_round_in_progress), race.round));
        mOptions.add(getString(R.string.ttl_completed_rounds));
        mOptions.add(getString(R.string.ttl_leader_board));
        mOptions.add(getString(R.string.ttl_team_results));
        mOptions.add(getString(R.string.ttl_auto_read));

    }

    private void setList() {
        mArrAdapter = new ArrayAdapter<String>(this, R.layout.listrow, mOptions) {
            @Override
            public @NonNull
            View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row;

                if (null == convertView) {
                    row = getLayoutInflater().inflate(R.layout.listrow, parent, false);
                    row.setOnClickListener(ResultsRaceActivity.this);
                    row.setOnCreateContextMenuListener(ResultsRaceActivity.this);
                } else {
                    row = convertView;
                }

                row.setTag(position);

                TextView tv = row.findViewById(R.id.text1);
                tv.setText(mOptions.get(position));

                return row;
            }
        };
    }

    @Override
    public void onClick(View v) {
        int position = (int)v.getTag();
        Intent intent = null;
        switch (position) {
            case 0:
                intent = new Intent(this, ResultsRoundInProgressActivity.class);
                break;
            case 1:
                intent = new Intent(this, ResultsCompletedRoundsActivity.class);
                break;
            case 2:
                intent = new Intent(this, ResultsLeaderBoardActivity.class);
                break;
            case 3:
                intent = new Intent(this, ResultsTeamsActivity.class);
                break;
            case 4:
                intent = new Intent(this, ResultsReadActivity.class);
                break;
        }
        if (intent != null) {
            intent.putExtra("race_id", mRid);
            startActivityForResult(intent, mRid);
        }
    }
}
