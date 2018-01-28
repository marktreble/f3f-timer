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
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.results.Results;
import com.marktreble.f3ftimer.dialog.AboutActivity;
import com.marktreble.f3ftimer.dialog.HelpActivity;
import com.marktreble.f3ftimer.pilotmanager.PilotsActivity;
import com.marktreble.f3ftimer.racemanager.RaceListActivity;

import java.util.ArrayList;

public class ResultsRoundInProgressActivity extends ListActivity {
	
	static boolean DEBUG = true;
	static int RESULT_ABORTED = 1;
	
	private ArrayAdapter<String> mArrAdapter;
	private ArrayList<String> mArrNames;
    private ArrayList<String> mArrBibNumbers;
	private ArrayList<Pilot> mArrPilots;
    private ArrayList<Integer> mArrGroups;
    private ArrayList<Boolean> mFirstInGroup;

	private Integer mRid;
	private Race mRace;

    private Context mContext;

    private RaceData.Group mGroupScoring;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		
		ImageView view = (ImageView)findViewById(android.R.id.home);
		Resources r = getResources();
		int px = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, r.getDisplayMetrics());
		view.setPadding(0, 0, px, 0);
				
		setContentView(R.layout.race);

        mContext = this;

		Intent intent = getIntent();
		if (intent.hasExtra("race_id")){
			Bundle extras = intent.getExtras();
			mRid = extras.getInt("race_id");
		}
		
		RaceData datasource = new RaceData(this);
  		datasource.open();
  		Race race = datasource.getRace(mRid);
  		datasource.close();
  		mRace = race;  		
 
 		TextView tt = (TextView) findViewById(R.id.race_title);
  		tt.setText(mRace.name);

	    setList();
	    setListAdapter(mArrAdapter);
	}

	/*
	 * Get Pilots from database to populate the listview
	 */
	
	private void getNamesArray(){
		Results r = new Results();
		r.getRoundInProgress(this, mRid);

		mArrNames = r.mArrNames;
		mArrPilots = r.mArrPilots;
		mArrBibNumbers = r.mArrBibNumbers;
		mArrGroups = r.mArrGroups;
		mFirstInGroup = r.mFirstInGroup;
		mGroupScoring = r.mGroupScoring;

    }
	
	private void setList(){
	    this.getNamesArray(); 

	    mArrAdapter = new ArrayAdapter<String>(this, R.layout.listrow_racepilots , R.id.text1, mArrNames){
   	   		@Override
   	   		public View getView(int position, View convertView, ViewGroup parent) {
                View row;
                
                if (mArrNames.get(position) == null) return null;
                
                if (null == convertView) {
                row = getLayoutInflater().inflate(R.layout.listrow_racepilots, parent, false);
                } else {
                row = convertView;
                }
                
                Pilot p = mArrPilots.get(position);

                TextView p_number = (TextView) row.findViewById(R.id.number);
                p_number.setText(mArrBibNumbers.get(position));

                TextView p_name = (TextView) row.findViewById(R.id.text1);
                p_name.setText(mArrNames.get(position));
                p_name.setTextColor(getResources().getColor(R.color.text3 ));

                Drawable flag = p.getFlag(mContext);
                if (flag != null){
                    p_name.setCompoundDrawablesWithIntrinsicBounds(flag, null, null, null);
                    int padding = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
                    p_name.setCompoundDrawablePadding(padding);
                }

                View group_header = row.findViewById(R.id.group_header);
                TextView group_header_label = (TextView) row.findViewById(R.id.group_header_label);
                if (mGroupScoring.num_groups>1 && mFirstInGroup.get(position)){
                    group_header.setVisibility(View.VISIBLE);
                    group_header_label.setText("Group "+(mArrGroups.get(position)+1));
                } else {
                    group_header.setVisibility(View.GONE);
                }

                TextView time = (TextView) row.findViewById(R.id.time);
				if ((p.time==0 || Float.isNaN(p.time)) && !p.flown){
                	time.setText(getResources().getString(R.string.notime));
                } else {
                	time.setText(String.format("%.2f", p.time).replace(",", "."));
                }

                TextView points = (TextView) row.findViewById(R.id.points);
                if (p.flown || p.status==Pilot.STATUS_RETIRED){
            		points.setText(String.format("%.2f", p.points).replace(",", "."));
                } else {
            		points.setText("");
                }

				TextView penalty = (TextView) row.findViewById(R.id.penalty);
				if (p.penalty >0){
					penalty.setText(String.format("%s%d", getResources().getString(R.string.penalty), p.penalty));
				} else {
					penalty.setText(getResources().getString(R.string.empty));
				}
                return row;
            }
   	   	};
   	   	
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

	public void share(){

	}

	public void pilotManager(){
		Intent intent = new Intent(mContext,PilotsActivity.class);
		startActivity(intent);
	}

	public void raceManager(){
		Intent intent = new Intent(mContext, RaceListActivity.class);
		startActivity(intent);
	}

	public void help(){
		Intent intent = new Intent(mContext, HelpActivity.class);
		startActivity(intent);
	}

	public void about(){
		Intent intent = new Intent(mContext, AboutActivity.class);
		startActivity(intent);
	}

}