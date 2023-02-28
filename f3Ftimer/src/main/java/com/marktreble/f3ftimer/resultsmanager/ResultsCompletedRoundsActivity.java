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
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.marktreble.f3ftimer.F3FtimerApplication;
import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;

import java.util.ArrayList;

public class ResultsCompletedRoundsActivity extends ResultsRaceBaseActivity
    implements ListView.OnClickListener {

    private ArrayAdapter<String> mArrAdapter;

    ArrayList<String> mOptions;

    private int mNumRounds = 0;

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

        if (mNumRounds <= 1) {
            TextView noneView = new TextView(this);
            noneView.setText(getString(R.string.no_rounds));

            noneView.setTextColor(F3FtimerApplication.themeAttributeToColor(
                    R.attr.t2,
                    this,
                    R.color.light_grey));
            int px1 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
            noneView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            noneView.setPadding(px1, px1, px1, px1);

            lv.addFooterView(noneView);
        }
    }

    private void getNamesArray() {
        RaceData datasource = new RaceData(this);
        datasource.open();
        Race race = datasource.getRace(mRid);
        datasource.close();

        TextView tt = findViewById(R.id.race_title);
        tt.setText(race.name);

        mOptions = new ArrayList<>();
        for (int i = 1; i < race.round; i++) {
           mOptions.add(String.format(getString(R.string.ttl_round_number), i));
        }

        mNumRounds = race.round;
    }

    private void setList() {
        mArrAdapter = new ArrayAdapter<String>(this, R.layout.listrow, mOptions) {
            @Override
            public @NonNull
            View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row;

                if (null == convertView) {
                    row = getLayoutInflater().inflate(R.layout.listrow, parent, false);
                    row.setOnClickListener(ResultsCompletedRoundsActivity.this);
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
        Intent intent = new Intent(this, ResultsCompletedRoundActivity.class);
        intent.putExtra("race_id", mRid);
        intent.putExtra("round_id", position + 1);
        startActivity(intent);
    }
}
