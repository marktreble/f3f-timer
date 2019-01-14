/*
 * ResultsRaceActivity
 * Shows List of contestants, along with total points and normalised score
 */
package com.marktreble.f3ftimer.resultsmanager;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.results.Results;
import com.marktreble.f3ftimer.dialog.AboutActivity;
import com.marktreble.f3ftimer.dialog.HelpActivity;
import com.marktreble.f3ftimer.pilotmanager.PilotsActivity;
import com.marktreble.f3ftimer.racemanager.RaceListActivity;

import java.util.ArrayList;

public class ResultsLeaderBoardActivity extends ListActivity {

    static boolean DEBUG = true;
    static int RESULT_ABORTED = 1;

    private ArrayAdapter<String> mArrAdapter;
    private ArrayList<String> mArrNames;
    private ArrayList<String> mArrNumbers;
    private ArrayList<Pilot> mArrPilots;
    private ArrayList<Float> mArrScores;

    private float mFTD;
    private String mFTDName;
    private int mFTDRound;

    private Integer mRid;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ImageView view = (ImageView) findViewById(android.R.id.home);
        Resources r = getResources();
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, r.getDisplayMetrics());
        view.setPadding(0, 0, px, 0);

        mContext = this;

        setContentView(R.layout.race);

        Intent intent = getIntent();
        if (intent.hasExtra("race_id")) {
            Bundle extras = intent.getExtras();
            mRid = extras.getInt("race_id");
        }

        TextView tt = (TextView) findViewById(R.id.race_title);
        tt.setText(getString(R.string.leader_board));

        setList();
        setListAdapter(mArrAdapter);
    }

    /*
     * Show pilot breakdown
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Pilot has been clicked, so show breakdown of rounds

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
        this.getNamesArray();

        mArrAdapter = new ArrayAdapter<String>(this, R.layout.listrow_resultspilots, R.id.text1, mArrNames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row;

                if (mArrNames.get(position) == null) return null;

                if (null == convertView) {
                    row = getLayoutInflater().inflate(R.layout.listrow_resultspilots, parent, false);
                } else {
                    row = convertView;
                }

                Pilot p = mArrPilots.get(position);

                TextView p_number = (TextView) row.findViewById(R.id.number);
                p_number.setText(mArrNumbers.get(position));

                TextView p_name = (TextView) row.findViewById(R.id.text1);
                p_name.setText(mArrNames.get(position));
                p_name.setTextColor(getResources().getColor(R.color.text3));

                Drawable rosette = null;
                if (position == 0) {
                    rosette = ContextCompat.getDrawable(mContext, R.drawable.gold);
                } else if (position == 1) {
                    rosette = ContextCompat.getDrawable(mContext, R.drawable.silver);
                } else if (position == 2) {
                    rosette = ContextCompat.getDrawable(mContext, R.drawable.bronze);
                }

                Drawable flag = p.getFlag(mContext);
                if (flag != null) {
                    p_name.setCompoundDrawablesWithIntrinsicBounds(flag, null, rosette, null);
                    int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
                    p_name.setCompoundDrawablePadding(padding);
                }

                row.setBackgroundColor(getResources().getColor(R.color.background));

                TextView time = (TextView) row.findViewById(R.id.time);
                time.setText(String.format("%.2f", p.points));

                TextView points = (TextView) row.findViewById(R.id.points);
                points.setText(String.format("%.2f", mArrScores.get(position)));

                return row;
            }
        };

        TextView ftdView = new TextView(mContext);
        if (mFTD < 9999) {
            ftdView.setText(String.format(getString(R.string.ftd_result), mFTD, mFTDName, mFTDRound));
        } else {
            ftdView.setText(getString(R.string.no_rounds));
        }

        Resources r = getResources();
        int px1 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, r.getDisplayMetrics());
        ftdView.setTextColor(r.getColor(R.color.text2));
        ftdView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        ftdView.setPadding(px1, px1, px1, px1);

        getListView().addFooterView(ftdView);

        getListView().invalidateViews();
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