package com.marktreble.f3ftimer.dialog;

import android.content.Intent;
import android.os.Bundle;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.pilot.PilotData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;

import java.util.ArrayList;

/**
 * Created by marktreble on 22/12/14.
 */
public class FlyingOrderEditActivity extends FragmentActivity {

    private Integer mRid;
    public ArrayList<Integer> pilots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.race_new);

        Intent i = getIntent();
        Integer race_id;
        mRid = i.getIntExtra("race_id", 0);

        NewRaceFrag3 f;
        if (savedInstanceState != null) {
            Log.i("ONCREATE (FOEACTIVITY)", "RESTORING FROM SAVEDINSTANCESTATE");
        } else {

            pilots = new ArrayList<>();
            RacePilotData datasource = new RacePilotData(this);
            datasource.open();
            ArrayList<Pilot> allPilots = datasource.getAllPilotsForRace(mRid, 0, 0, 0);
            datasource.close();
            for (Pilot p : allPilots) pilots.add(p.pilot_id);

            f = new NewRaceFrag3();
            f.setRetainInstance(true);
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.dialog1, f, "newracefrag3");
            ft.commit();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("pigs", "pigs");

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
    }

    public void updateFlyingOrder(){
        RacePilotData racepilotsdatasource = new RacePilotData(this);
        racepilotsdatasource.open();
        racepilotsdatasource.deleteAllPilots(mRid);
        PilotData pilotsdatasource = new PilotData(this);
        pilotsdatasource.open();
        for (int i=0; i<this.pilots.size(); i++){
            Pilot p = pilotsdatasource.getPilot(this.pilots.get(i));
            racepilotsdatasource.addPilot(p, mRid);
        }
        pilotsdatasource.close();
        racepilotsdatasource.close();
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
}
