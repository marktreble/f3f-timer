package com.marktreble.f3ftimer.resultsmanager;

import java.util.ArrayList;

import android.os.Bundle;
import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.marktreble.f3ftimer.data.race.*;
import com.marktreble.f3ftimer.R;


public class ResultsRaceActivity extends ListActivity {

	private ArrayAdapter<String> mArrAdapter;

	private Integer mRid;

	static final boolean DEBUG = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ImageView view = (ImageView)findViewById(android.R.id.home);
		Resources r = getResources();
		int px = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, r.getDisplayMetrics());
		view.setPadding(0, 0, px, 0);
		
		setContentView(R.layout.race);
	    
		Intent intent = getIntent();
		if (intent.hasExtra("race_id")){
			Bundle extras = intent.getExtras();
			mRid = extras.getInt("race_id");
		}
		
		RaceData datasource = new RaceData(this);
  		datasource.open();
  		Race race = datasource.getRace(mRid);
  		datasource.close();	
  		
 		TextView tt = (TextView) findViewById(R.id.race_title);
  		tt.setText(race.name);
  		
		ArrayList<String> arrOptions = new ArrayList<String>();
		arrOptions.add(String.format("Round in Progress (R%d)", race.round));
		arrOptions.add("Completed Rounds");
		arrOptions.add("Leader Board");
		
   	   	mArrAdapter = new ArrayAdapter<String>(this, R.layout.listrow , arrOptions);
        setListAdapter(mArrAdapter);        
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = null;
		switch (position){
			case 0:
				 intent = new Intent(this, ResultsRoundInProgressActivity.class);
				 break;
			case 1:
				 intent = new Intent(this, ResultsCompletedRoundsActivity.class);				
				break;
			case 2:
				 intent = new Intent(this, ResultsLeaderBoardActivity.class);				
				break;
		}
        intent.putExtra("race_id", mRid);
    	startActivityForResult(intent, mRid);
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
