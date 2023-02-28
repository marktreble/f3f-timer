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

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.util.Log;
import android.view.View;

import com.marktreble.f3ftimer.F3FtimerApplication;
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
        ((F3FtimerApplication) getApplication()).setTransparentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.race_new);

        NewRaceFrag1 f;
        if (savedInstanceState == null) {
            f = new NewRaceFrag1();
            //f.setRetainInstance(true);
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.dialog1, f, "newracefrag1");
            ft.commit();
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fm = getSupportFragmentManager();
                NewRaceFrag2 f2 = (NewRaceFrag2) fm.findFragmentByTag("newracefrag2");
                NewRaceFrag3 f3 = (NewRaceFrag3) fm.findFragmentByTag("newracefrag3");
                if (f2 != null) {
                    if (f2.onBackPressed())
                        return;
                }

                if (f3 != null) {
                    if (f3.onBackPressed())
                        return;
                }

                finish();
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("name", this.name);
        outState.putInt("type", this.rpf);
        outState.putInt("offset", this.offset);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.name = savedInstanceState.getString("name");
        this.rpf = savedInstanceState.getInt("type");
        this.offset = savedInstanceState.getInt("offset");
    }

    public void saveNewRace() {
        Race r = new Race();
        r.name = this.name;
        r.rounds_per_flight = this.rpf;
        r.offset = this.offset;

        RaceData datasource = new RaceData(NewRaceActivity.this);
        datasource.open();
        int race_id = (int) datasource.saveRace(r);
        datasource.close();

        RacePilotData racepilotsdatasource = new RacePilotData(NewRaceActivity.this);
        racepilotsdatasource.open();
        PilotData pilotsdatasource = new PilotData(NewRaceActivity.this);
        pilotsdatasource.open();
        for (int i = 0; i < this.pilots.size(); i++) {
            Pilot p = pilotsdatasource.getPilot(this.pilots.get(i));
            racepilotsdatasource.addPilot(p, race_id);
            Log.i("NEWRACE", p.toString());
        }
        pilotsdatasource.close();
        racepilotsdatasource.close();
    }

    public void getFragment(Fragment f, String tag) {
        //f.setRetainInstance(true);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.dialog1, f, tag);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();
    }

    public void moveUp(View v) {
        FragmentManager fm = getSupportFragmentManager();
        NewRaceFrag3 f = (NewRaceFrag3) fm.findFragmentByTag("newracefrag3");
        if (f != null) {
            f.moveUp(v);
        }
    }

    public void moveDown(View v) {
        FragmentManager fm = getSupportFragmentManager();
        NewRaceFrag3 f = (NewRaceFrag3) fm.findFragmentByTag("newracefrag3");
        if (f != null) {
            f.moveDown(v);
        }
    }

    /*
    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        NewRaceFrag2 f2 = (NewRaceFrag2) fm.findFragmentByTag("newracefrag2");
        NewRaceFrag3 f3 = (NewRaceFrag3) fm.findFragmentByTag("newracefrag3");
        if (f2 != null) {
            if (f2.onBackPressed())
                return;
        }

        if (f3 != null) {
            if (f3.onBackPressed())
                return;
        }

        super.onBackPressed();


    }

     */
}
