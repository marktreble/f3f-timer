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

public class ResultsCompletedRoundActivity extends ListActivity {
	
	static boolean DEBUG = true;
	static int RESULT_ABORTED = 1;
	
	private ArrayAdapter<String> mArrAdapter;
	private ArrayList<String> mArrNames;
    private ArrayList<String> mArrNumbers;
    private ArrayList<Pilot> mArrPilots;

	private Integer mRid;
	private Integer mRound;

    private Context mContext;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		
		ImageView view = (ImageView)findViewById(android.R.id.home);
		Resources r = getResources();
		int px = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, r.getDisplayMetrics());
		view.setPadding(0, 0, px, 0);

        mContext = this;

		setContentView(R.layout.race);
			
		Intent intent = getIntent();
		if (intent.hasExtra("race_id")){
			Bundle extras = intent.getExtras();
			mRid = extras.getInt("race_id");
		}
		if (intent.hasExtra("round_id")){
			Bundle extras = intent.getExtras();
			mRound = extras.getInt("round_id");
		}
		
		RaceData datasource = new RaceData(this);
  		datasource.open();
  		Race race = datasource.getRace(mRid);
		RaceData.Group groups = datasource.getGroups(mRid, mRound);
  		datasource.close();

		String group_scored = "";
		if (groups.num_groups>1){
			group_scored = String.format(" - Group Scored (%d)", groups.num_groups);
		}
 		TextView tt = (TextView) findViewById(R.id.race_title);
  		tt.setText(String.format("Round %d%s", mRound, group_scored));

	    setList();
	    setListAdapter(mArrAdapter);
	}

	/*
	 * Get Pilots from database to populate the listview
	 */

	private void getNamesArray(){
		Results r = new Results();
		r.getResultsForCompletedRound(this, mRid, mRound);

		mArrNames = r.mArrNames;
		mArrPilots = r.mArrPilots;
		mArrNumbers = r.mArrNumbers;
	}
	
	private void setList(){
	    this.getNamesArray(); 

	    mArrAdapter = new ArrayAdapter<String>(this, R.layout.listrow_resultspilots , R.id.text1, mArrNames){
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
				//p_number.setText(p.position);

                TextView p_group = (TextView) row.findViewById(R.id.group);
                p_group.setText(Integer.toString(p.group));

                TextView p_name = (TextView) row.findViewById(R.id.text1);
                p_name.setText(mArrNames.get(position));
                p_name.setTextColor(getResources().getColor(R.color.text3 ));

                Drawable flag = p.getFlag(mContext);
                if (flag != null){
                    p_name.setCompoundDrawablesWithIntrinsicBounds(flag, null, null, null);
                    int padding = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
                    p_name.setCompoundDrawablePadding(padding);
                }


                TextView time = (TextView) row.findViewById(R.id.time);
				if ((p.time==0 || Float.isNaN(p.time)) && !p.flown){
                	time.setText(getResources().getString(R.string.notime));
                } else {
                	time.setText(String.format("%.2f", p.time));
                }

                TextView points = (TextView) row.findViewById(R.id.points);
                if (p.flown || p.status==Pilot.STATUS_RETIRED){
            		points.setText(String.format("%.2f", p.points));
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