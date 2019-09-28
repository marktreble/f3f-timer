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

package com.marktreble.f3ftimer.dialog;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;

import com.marktreble.f3ftimer.F3FtimerApplication;
import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;

import java.util.ArrayList;

public class FlyingOrderEditActivity extends FragmentActivity {

    private Integer mRid;
    public ArrayList<Integer> pilots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((F3FtimerApplication) getApplication()).setTransparentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.race_new);

        Intent i = getIntent();
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
            for (Pilot p : allPilots) pilots.add(p.id);

            f = new NewRaceFrag3();
            f.mRid = mRid;
            f.setRetainInstance(true);
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.dialog1, f, "newracefrag3");
            ft.commit();

            Log.i("HEYHEYHEY", pilots.toString());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    public void updateFlyingOrder() {
        ArrayList<Pilot> allPilots;
        RacePilotData datasource = new RacePilotData(this);
        datasource.open();
        allPilots = datasource.getAllPilotsForRace(mRid, 0, 0, 0);

        datasource.deleteAllPilots(mRid);
        for (int i = 0; i < this.pilots.size(); i++) {
            for (int j = 0; j < allPilots.size(); j++) {
                Pilot p = allPilots.get(j);
                if (p.id == this.pilots.get(i)) {
                    if (p.pilot_id == 0) p.id = 0;
                    datasource.addPilot(p, mRid);
                    Log.i("EDITRACE", p.toString());

                }
            }
        }
        datasource.close();
    }

    public void moveUp(View v) {
        FragmentManager fm = getSupportFragmentManager();
        NewRaceFrag3 f = (NewRaceFrag3) fm.findFragmentByTag("newracefrag3");
        f.moveUp(v);
    }

    public void moveDown(View v) {
        FragmentManager fm = getSupportFragmentManager();
        NewRaceFrag3 f = (NewRaceFrag3) fm.findFragmentByTag("newracefrag3");
        f.moveDown(v);
    }
}
