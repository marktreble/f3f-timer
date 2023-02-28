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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.marktreble.f3ftimer.BaseActivity;
import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.dialog.AboutActivity;
import com.marktreble.f3ftimer.dialog.HelpActivity;
import com.marktreble.f3ftimer.pilotmanager.PilotsActivity;
import com.marktreble.f3ftimer.racemanager.RaceListActivity;

import java.util.ArrayList;

public class ResultsActivity extends BaseActivity
    implements ListView.OnClickListener {

    private ArrayAdapter<String> mArrAdapter;
    private ArrayList<String> mArrNames;
    private ArrayList<Integer> mArrIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mPageTitle = getString(R.string.app_results);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.results_manager);

        mArrNames = new ArrayList<>();
        mArrIds = new ArrayList<>();

        getNamesArray();
        setList();

        ListView lv = findViewById(android.R.id.list);
        lv.setAdapter(mArrAdapter);
    }

    public void onBackPressed() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
    }

    @Override
    public void onResume() {
        super.onResume();

        this.getNamesArray();
        mArrAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        int position = (int)v.getTag();
        Intent intent = new Intent(this, ResultsRaceActivity.class);
        Integer pid = mArrIds.get(position);
        intent.putExtra("race_id", pid);
        startActivity(intent);
    }

    private void getNamesArray() {

        RaceData datasource = new RaceData(this);
        datasource.open();
        ArrayList<Race> allRaces = datasource.getAllRaces();
        datasource.close();

        @SuppressWarnings("unchecked")
        ArrayList<String> clonedNames = (ArrayList<String>) mArrNames.clone();
        mArrNames.removeAll(clonedNames);

        @SuppressWarnings("unchecked")
        ArrayList<Integer> clonedIds = (ArrayList<Integer>) mArrIds.clone();
        mArrIds.removeAll(clonedIds);


        for (Race r : allRaces) {
            mArrNames.add(String.format("%s", r.name));
            mArrIds.add(r.id);

        }
    }

    private void setList(){
        mArrAdapter = new ArrayAdapter<String>(this, R.layout.listrow, mArrNames) {
            @Override
            public @NonNull
            View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row;

                if (null == convertView) {
                    row = getLayoutInflater().inflate(R.layout.listrow, parent, false);
                    row.setOnClickListener(ResultsActivity.this);
                    row.setOnCreateContextMenuListener(ResultsActivity.this);
                } else {
                    row = convertView;
                }

                row.setTag(position);

                TextView tv = row.findViewById(R.id.text1);
                tv.setText(mArrNames.get(position));

                return row;
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.results_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        int itemId = item.getItemId();
        if (itemId == R.id.menu_pilot_manager) {
            pilotManager();
            return true;
        } else if (itemId == R.id.menu_race_manager) {
            raceManager();
            return true;
        } else if (itemId == R.id.menu_help) {
            help();
            return true;
        } else if (itemId == R.id.menu_about) {
            about();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
