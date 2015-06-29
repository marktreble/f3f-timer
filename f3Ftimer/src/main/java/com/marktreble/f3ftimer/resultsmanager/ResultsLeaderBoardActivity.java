/*
 * ResultsRaceActivity
 * Shows List of contestants, along with total points and normalised score
 */
package com.marktreble.f3ftimer.resultsmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

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
import android.util.Log;
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

    private int mGroupScoring;

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
 
 		TextView tt = (TextView) findViewById(R.id.race_title);
  		tt.setText("Leader Board");

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
	
	private void getNamesArray(){
		
		RaceData datasource = new RaceData(ResultsLeaderBoardActivity.this);
  		datasource.open();
  		Race race = datasource.getRace(mRid);

		RacePilotData datasource2 = new RacePilotData(ResultsLeaderBoardActivity.this);
  		datasource2.open();
  		ArrayList<Pilot> allPilots = datasource2.getAllPilotsForRace(mRid, 0, 0);
  		ArrayList<String> p_names = new ArrayList<>();
  		ArrayList<String> p_nationalities = new ArrayList<>();
  		ArrayList<float[]> p_times = new ArrayList<>();
  		ArrayList<float[]> p_points = new ArrayList<>();
  		ArrayList<int[]> p_penalty = new ArrayList<>();
  		Float[] p_totals = new Float[allPilots.size()];
  		int[] p_positions = new int[allPilots.size()];
  		
  		mFTD = 9999;
  		
  		if (allPilots != null){
  			
  			// Get all times for pilots in all rounds
  			for (Pilot p : allPilots){
  				p_names.add(String.format("%s %s", p.firstname, p.lastname));
  				p_nationalities.add(p.nationality);
  				float[] sc = new float[race.round];
  				for (int rnd=0; rnd<race.round; rnd++){
  					sc[rnd] = datasource2.getPilotTimeInRound(mRid, p.id, rnd+1);
					Log.i("PILOT TIME", String.format("%s %s", p.firstname, p.lastname) +":" + Float.toString(sc[rnd]));
  				}
  				p_times.add(sc);
  			}

  			if (race.round>1){
	  			// Loop through each round to find the winner, then populate the scores
				for (int rnd=0; rnd<race.round-1; rnd++){
					ArrayList<Pilot> pilots_in_round = datasource2.getAllPilotsForRace(mRid, rnd+1, 0);

                    mGroupScoring = datasource.getGroups(mRid, rnd+1);

                    int g = 0; // Current group we are calculating

                    float[] ftg = new float[mGroupScoring+1]; // Fastest time in group (used for calculating normalised scores)
                    for (int i=0; i<mGroupScoring+1; i++)
                        ftg[i]= 9999;

                    int group_size = (int)Math.floor(p_names.size()/mGroupScoring);
                    int remainder = p_names.size() - (mGroupScoring * group_size);

			  		for (int i=0; i<p_names.size(); i++){
                        if (g<remainder){
                            if (i>= (group_size+1)*(g+1)) {
                                g++;
                            }
                        } else {
                            if (i>= ((group_size+1)*remainder) + (group_size*((g+1)-remainder))) {
                                g++;
                            }
                        }

                        String str_t = String.format("%.2f",p_times.get(i)[rnd]);
			 			float t = Float.parseFloat(str_t);
						if (t>0)
                            ftg[g] = Math.min( t, ftg[g]);
		  			
						// Update the FTD here too
						mFTD = Math.min(mFTD,  ftg[g]);
						if (mFTD == t){
							mFTDRound = rnd+1;
							mFTDName = p_names.get(i);
						}
			  		
			  		}

                    g = 0; // Current group we are calculating

			  		float[] points = new float[p_names.size()];
			  		int[] penalty = new int[p_names.size()];
			  		for (int i=0; i<p_names.size(); i++){
                        if (g<remainder){
                            if (i>= (group_size+1)*(g+1)) {
                                g++;
                            }
                        } else {
                            if (i>= ((group_size+1)*remainder) + (group_size*((g+1)-remainder))) {
                                g++;
                            }
                        }

			  			String str_t = String.format("%.2f",p_times.get(i)[rnd]);
			 			float time = Float.parseFloat(str_t);
			  			float pnts = 0;
			  			if (time>0)
							pnts = round2Fixed((ftg[g]/time) * 1000, 2);
	

						points[i] = pnts;
		  				penalty[i] = pilots_in_round.get(i).penalty;
			  		}
					p_points.add(points);
					p_penalty.add(penalty);
				}
	
	  			// Loop through each pilot to Find discards + calc totals
	  			int numdiscards = (race.round>4) ? ((race.round>15) ? 2 : 1) : 0;
				for (int i=0; i<p_names.size(); i++){
					Float[] totals = new Float[race.round-1];
						
					float penalties = 0;
					for (int rnd=0; rnd<race.round-1; rnd++){
						totals[rnd] = p_points.get(rnd)[i];
						penalties+=p_penalty.get(rnd)[i] * 100;
					}
					
					// sort totals in order then lose the lowest according to numdiscards
					Arrays.sort(totals);
					float tot = 0;
					for (int j=numdiscards; j<race.round-1; j++)
						tot += totals[j];
						
					// Now apply penalties
					p_totals[i] = tot - penalties;
				}

                // Now sort the pilots
				Float[] p_sorted_totals = p_totals.clone();
				Arrays.sort(p_sorted_totals, Collections.reverseOrder());

                // Set the positions according to the sorted order
				for (int i=0; i<p_names.size(); i++){
					for (int j=0; j<p_names.size(); j++){
						if (p_totals[i] == p_sorted_totals[j])
							p_positions[i] = (int)j + 1;
								
					}
				}  			
	  			
				int sz = p_names.size();
				mArrNames = new ArrayList<>(sz);
				mArrNumbers = new ArrayList<>(sz);
				mArrPilots = new ArrayList<>(sz);
				mArrScores = new ArrayList<>(sz);
				
				// Initialise
				for (int i = 0; i < sz; i++) {
					mArrNames.add("");
					mArrNumbers.add("");
					mArrPilots.add(new Pilot());
					mArrScores.add(1000f);
				}

	  			for (int i=0; i<sz; i++){
	  				int pos = p_positions[i]-1;
	  				mArrNames.set(pos, String.format("%s", p_names.get(i)));
	  				mArrNumbers.set(pos, String.format("%d.", p_positions[i]));
	  				Pilot p = new Pilot();
	  				p.points = round2Fixed(p_totals[i].floatValue(), 2);
	  				p.nationality = p_nationalities.get(i);
	  				mArrPilots.set(pos, p);
	  			}
	  			float top_score = mArrPilots.get(0).points;
                float previousscore = 1000.0f;
	  			for (int i=1; i<sz; i++){
	  				float pilot_points = mArrPilots.get(i).points;
	  				float normalised = round2Fixed(pilot_points/top_score * 1000, 2);

                    // Check for tied scores - use the same position qualifier
                    if (normalised == previousscore)
                        mArrNumbers.set(i, mArrNumbers.get(i-1));
                    previousscore = normalised;

	  				mArrScores.set(i, Float.valueOf(normalised));
	  			}
  			} else {
  	  			// No rounds complete yet
  				mArrNames = new ArrayList<String>(0);
  				mArrPilots = new ArrayList<Pilot>(0);
  				mArrScores = new ArrayList<Float>(0);
   	  		}
  			datasource2.close();
  		}
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
                
                Pilot p = (Pilot)mArrPilots.get(position);
                
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
        		
           		row.setBackgroundColor(getResources().getColor(R.color.background));
           		
                TextView time = (TextView) row.findViewById(R.id.time);
           		time.setText(Float.toString(p.points));

                TextView points = (TextView) row.findViewById(R.id.points);
           		points.setText(Float.toString(mArrScores.get(position)));

                return row;
            }
   	   	};

        TextView ftdView = new TextView(mContext);
   	   	if (mFTD<9999) {
            ftdView.setText(String.format("Fastest Time: %.2fs by %s in round %d", mFTD, mFTDName, mFTDRound));
        } else {
            ftdView.setText("No rounds completed yet");
        }

        Resources r = getResources();
        int px1 = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, r.getDisplayMetrics());
	   	ftdView.setTextColor(r.getColor(R.color.text2));
	   	ftdView.setTextSize(TypedValue.COMPLEX_UNIT_SP,24);
	   	ftdView.setPadding(px1, px1, px1, px1);
	   	   	
	   	getListView().addFooterView(ftdView);

   	   	getListView().invalidateViews();
	}
	
	private float round2Fixed(float value, double places){

		double multiplier = Math.pow(10, places);  
		//Log.i("MULTIPLIER", Double.toString(multiplier));
		double integer = Math.floor(value);
		//Log.i("INTEGER", Double.toString(integer));
		double precision = Math.floor((value-integer) * multiplier);
		//Log.i("PRECISION", Double.toString(precision));

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