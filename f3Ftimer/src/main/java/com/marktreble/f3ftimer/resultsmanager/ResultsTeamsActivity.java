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

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.marktreble.f3ftimer.F3FtimerApplication;
import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.results.Results;
import com.marktreble.f3ftimer.dialog.AboutActivity;
import com.marktreble.f3ftimer.dialog.HelpActivity;
import com.marktreble.f3ftimer.pilotmanager.PilotsActivity;
import com.marktreble.f3ftimer.racemanager.RaceListActivity;

import java.util.ArrayList;

public class ResultsTeamsActivity extends ListActivity {

    private ArrayAdapter<String> mArrAdapter;
    private ArrayList<String> mArrNames;
    private ArrayList<String> mArrNumbers;
    private ArrayList<Float> mArrScores;
    private ArrayList<ArrayList<String>> mArrPilotNames;

    private Integer mRid;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((F3FtimerApplication) getApplication()).setBaseTheme(this);
        super.onCreate(savedInstanceState);

        ImageView view = findViewById(android.R.id.home);
        Resources r = getResources();
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, r.getDisplayMetrics());
        view.setPadding(0, 0, px, 0);

        mContext = this;

        setContentView(R.layout.race);

        Intent intent = getIntent();
        if (intent.hasExtra("race_id")) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                mRid = extras.getInt("race_id");
            }
        }

        TextView tt = findViewById(R.id.race_title);
        tt.setText(getString(R.string.team_results));

        setList();
        setListAdapter(mArrAdapter);
    }

    /*
     * Get Pilots from database to populate the listview
     */

    private void getNamesArray() {

        Results r = new Results();
        r.getTeamResultsForRace(ResultsTeamsActivity.this, mRid);

        mArrNames = r.mArrNames;
        mArrNumbers = r.mArrNumbers;
        mArrScores = r.mArrScores;
        mArrPilotNames = r.mArrPilotNames;

    }

    private void setList() {
        this.getNamesArray();

        mArrAdapter = new ArrayAdapter<String>(this, R.layout.listrow_resultsteams, R.id.text1, mArrNames) {
            @Override
            public @NonNull
            View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row;

                if (null == convertView) {
                    row = getLayoutInflater().inflate(R.layout.listrow_resultsteams, parent, false);
                } else {
                    row = convertView;
                }

                TextView p_number = row.findViewById(R.id.number);
                p_number.setText(mArrNumbers.get(position));

                TextView p_name = row.findViewById(R.id.text1);
                p_name.setText(mArrNames.get(position));

                TextView points = row.findViewById(R.id.points);
                points.setText(String.format("%.2f", mArrScores.get(position)));

                TextView pilot_names = row.findViewById(R.id.pilot_names);
                StringBuilder pilots = new StringBuilder();

                for (String s : mArrPilotNames.get(position)) {
                    pilots.append(s);
                    pilots.append("\n");
                }
                pilot_names.setText(pilots.toString());

                return row;
            }
        };

        if (mArrNames.size() == 0) {
            TextView noneView = new TextView(mContext);
            noneView.setText(getString(R.string.no_teams));

            Resources r = getResources();
            int px1 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, r.getDisplayMetrics());
            noneView.setTextColor(F3FtimerApplication.themeAttributeToColor(
                    R.attr.t2,
                    this,
                    R.color.light_grey));
            noneView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            noneView.setPadding(px1, px1, px1, px1);

            getListView().addFooterView(noneView);

            getListView().invalidateViews();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.results, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.menu_share:
                share();
                return true;
            case R.id.menu_pilot_manager:
                pilotManager();
                return true;
            case R.id.menu_race_manager:
                raceManager();
                return true;
            case R.id.menu_help:
                help();
                return true;
            case R.id.menu_about:
                about();
                return true;


            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void share() {

    }

    public void pilotManager() {
        Intent intent = new Intent(mContext, PilotsActivity.class);
        startActivity(intent);
    }

    public void raceManager() {
        Intent intent = new Intent(mContext, RaceListActivity.class);
        startActivity(intent);
    }

    public void help() {
        Intent intent = new Intent(mContext, HelpActivity.class);
        startActivity(intent);
    }

    public void about() {
        Intent intent = new Intent(mContext, AboutActivity.class);
        startActivity(intent);
    }
}
