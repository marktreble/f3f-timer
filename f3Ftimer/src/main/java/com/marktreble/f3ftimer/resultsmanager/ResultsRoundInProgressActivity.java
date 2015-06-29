/*
 * ResultsRaceActivity
 * Shows List of contestants, along with total points and normalised score
 */
package com.marktreble.f3ftimer.resultsmanager;

import java.util.ArrayList;

import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;

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

public class ResultsRoundInProgressActivity extends ListActivity {
	
	static boolean DEBUG = true;
	static int RESULT_ABORTED = 1;
	
	private ArrayAdapter<String> mArrAdapter;
	private ArrayList<String> mArrNames;
    private ArrayList<String> mArrNumbers;
	private ArrayList<Pilot> mArrPilots;
    private ArrayList<Integer> mArrGroups;
    private ArrayList<Boolean> mFirstInGroup;

	private Integer mRid;
	private Race mRace;

    private Context mContext;

    private int mGroupScoring;

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
		
		RaceData datasource = new RaceData(this);
  		datasource.open();
  		Race race = datasource.getRace(mRid);

  		RacePilotData datasource2 = new RacePilotData(this);
		datasource2.open();
		ArrayList<Pilot> allPilots = datasource2.getAllPilotsForRace(mRid, race.round, mRace.offset);
		
		mArrNames = new ArrayList<>();
		mArrPilots = new ArrayList<>();
        mArrNumbers = new ArrayList<>();
        mArrGroups = new ArrayList<>();
        mFirstInGroup = new ArrayList<>();

        int g = 0; // Current group we are calculating

        mGroupScoring = datasource.getGroups(mRid, race.round);

        float[] ftg = new float[mGroupScoring+1]; // Fastest time in group (used for calculating normalised scores)
        for (int i=0; i<mGroupScoring+1; i++)
            ftg[i]= 9999;

        boolean first = true;
        int group_size = (int)Math.floor(allPilots.size()/mGroupScoring);
        int remainder = allPilots.size() - (mGroupScoring * group_size);

        // Find ftr
        for (int i=0;i<allPilots.size();i++){
            if (g<remainder){
                if (i>= (group_size+1)*(g+1)) {
                    g++;
                    first = true;
                }
            } else {
                if (i>= ((group_size+1)*remainder) + (group_size*((g+1)-remainder))) {
                    g++;
                    first = true;
                }
            }
            Pilot p = allPilots.get(i);
			mArrNames.add(String.format("%s %s", p.firstname, p.lastname));
            mArrNumbers.add(String.format("%d.", i+1));
            mArrGroups.add(g);
            mFirstInGroup.add(first);
			mArrPilots.add(p);
			first = false;

			String t_str = String.format("%.2f", p.time);
			float time = Float.parseFloat(t_str);

            ftg[g] = (time>0) ? Math.min(ftg[g], time) : ftg[g];
		}
		
		// Set points for each pilot
        g=0;

        // Set points for each pilot
        for (int i=0;i<allPilots.size();i++){
            if (g<remainder){
                if (i>= (group_size+1)*(g+1)) {
                    g++;
                }
            } else {
                if (i>= ((group_size+1)*remainder) + (group_size*((g+1)-remainder))) {
                    g++;
                }
            }
            Pilot p = allPilots.get(i);
			String t_str = String.format("%.2f", p.time);
			float time = Float.parseFloat(t_str);
			
			if (time>0)
				p.points = round2Fixed((ftg[g]/time) * 1000, 2);
			
			if (time==0 && p.flown) // Avoid division by 0
				p.points = 0f;
			
			p.points-= p.penalty * 100;
			
			if (time==0 && p.status==Pilot.STATUS_RETIRED) // Avoid division by 0
				p.points = 0f;
			
		}
		
		datasource2.close();
        datasource.close();
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
                p_number.setText(mArrNumbers.get(position));

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
                if (mGroupScoring>1 && mFirstInGroup.get(position)){
                    group_header.setVisibility(View.VISIBLE);
                    group_header_label.setText("Group "+(mArrGroups.get(position)+1));
                } else {
                    group_header.setVisibility(View.GONE);
                }

                TextView time = (TextView) row.findViewById(R.id.time);
                if (p.time==0 && !p.flown){
                	time.setText(getResources().getString(R.string.notime));
                } else {
                	time.setText(String.format("%.2f", p.time));
                }

                TextView points = (TextView) row.findViewById(R.id.points);
                if (p.flown || p.status==Pilot.STATUS_RETIRED){
            		points.setText(Float.toString(p.points));
                } else {
            		points.setText("");
                }

                return row;
            }
   	   	};
   	   	
   	   	getListView().invalidateViews();
	}
	
	private float round2Fixed(float value, double places){

		double multiplier = Math.pow(10, places);  
		double integer = Math.floor(value);
		double precision = Math.floor((value-integer) * multiplier);

		return (float)(integer + (precision/multiplier));
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
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
			
	public void share(){
		/*Intent intent = new Intent(mContext, SettingsActivity.class);
    	startActivityForResult(intent, 1);
    	*/
	}

}