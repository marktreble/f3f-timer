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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.marktreble.f3ftimer.BaseActivity;
import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.results.Results;
import com.marktreble.f3ftimer.dialog.AboutActivity;
import com.marktreble.f3ftimer.dialog.GenericAlert;
import com.marktreble.f3ftimer.dialog.GenericListPicker;
import com.marktreble.f3ftimer.dialog.HelpActivity;
import com.marktreble.f3ftimer.exportimport.F3ftimerApiExportRace;
import com.marktreble.f3ftimer.exportimport.F3xvaultApiExportRace;
import com.marktreble.f3ftimer.filesystem.F3XVaultExport;
import com.marktreble.f3ftimer.filesystem.SpreadsheetExport;
import com.marktreble.f3ftimer.pilotmanager.PilotsActivity;
import com.marktreble.f3ftimer.racemanager.RaceListActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class ResultsRaceActivity extends ResultsRaceBaseActivity
    implements ListView.OnClickListener {

    private Integer mRid;

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
        }
        if (intent != null) {
            intent.putExtra("race_id", mRid);
            startActivityForResult(intent, mRid);
        }
    }
}
