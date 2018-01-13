/*
 * NewRaceActivity
 * Called by RaceListActivity when + button is pressed
 * Presented in 2 page popup (NewRaceFrag1, NewRaceFrag2)
 */
package com.marktreble.f3ftimer.dialog;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.pilot.PilotData;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;

import java.util.ArrayList;

public class NewRaceActivity extends FragmentActivity {
	
	public String name = "";
	public Integer offset = 0;
	public Integer rpf = 0;
	public ArrayList<Integer> pilots;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.race_new);

		NewRaceFrag1 f;
		if (savedInstanceState != null) {
            Log.i("ONCREATE (NRACTIVITY)","RESTORING FROM SAVEDINSTANCESTATE");
	    } else {
	    	f = new NewRaceFrag1();
            f.setRetainInstance(true);
	    	FragmentManager fm = getSupportFragmentManager();
	    	FragmentTransaction ft = fm.beginTransaction();
	    	ft.add(R.id.dialog1, f, "newracefrag1");
	    	ft.commit();
	    }
	}
	
	@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("name", this.name);
        outState.putInt("type", this.rpf);
        outState.putInt("offset", this.offset);
    }
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        this.name=savedInstanceState.getString("name");
		this.rpf=savedInstanceState.getInt("type");
		this.offset=savedInstanceState.getInt("offset");
	}
	
	public void saveNewRace(){
		Race r = new Race();
		r.name = this.name;
		r.rounds_per_flight = this.rpf;
		r.offset = this.offset;
		
  		RaceData datasource = new RaceData(NewRaceActivity.this);
  		datasource.open();
  		int race_id = (int)datasource.saveRace(r);
  		datasource.setGroups(race_id, 1, 1);
  		datasource.close();

  		RacePilotData racepilotsdatasource = new RacePilotData(NewRaceActivity.this);
  		racepilotsdatasource.open();
  		PilotData pilotsdatasource = new PilotData(NewRaceActivity.this);
  		pilotsdatasource.open();
  		for (int i=0; i<this.pilots.size(); i++){
  			Pilot p = pilotsdatasource.getPilot(this.pilots.get(i));
  			racepilotsdatasource.addPilot(p, race_id);
			Log.i("NEWRACE", p.toString());
  		}
		racepilotsdatasource.setStartPos(race_id, r.round, r.offset, r.start_number, true);
  		pilotsdatasource.close();
  		racepilotsdatasource.close();
	}
	
	public void getFragment(Fragment f, String tag){
		f.setRetainInstance(true);
		FragmentManager fm = getSupportFragmentManager();
    	FragmentTransaction ft = fm.beginTransaction();
    	ft.replace(R.id.dialog1, f, tag);
    	ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN );
    	ft.commit();
	}
	
	public void moveUp(View v) {
		FragmentManager fm = getSupportFragmentManager();
		NewRaceFrag3 f = (NewRaceFrag3)fm.findFragmentByTag("newracefrag3");
		f.moveUp(v);
	}

	public void moveDown(View v) {
		FragmentManager fm = getSupportFragmentManager();
		NewRaceFrag3 f = (NewRaceFrag3)fm.findFragmentByTag("newracefrag3");
		f.moveDown(v);
	}
    
    @Override
    public void onBackPressed(){
        FragmentManager fm = getSupportFragmentManager();
        NewRaceFrag2 f2 = (NewRaceFrag2)fm.findFragmentByTag("newracefrag2");
        NewRaceFrag3 f3 = (NewRaceFrag3)fm.findFragmentByTag("newracefrag3");
        if (f2!=null) {
            if (f2.onBackPressed())
                return;
        }

        if (f3!=null) {
            if (f3.onBackPressed())
                return;
        }

        super.onBackPressed();


    }
}
