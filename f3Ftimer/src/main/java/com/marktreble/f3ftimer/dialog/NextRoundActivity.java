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


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.WindowManager.LayoutParams;

import com.marktreble.f3ftimer.F3FtimerApplication;
import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.constants.IComm;

public class NextRoundActivity extends FragmentActivity {

    private Fragment mCurrentFragment;
    public int round_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((F3FtimerApplication) getApplication()).setTransparentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.race_timer);


        Intent intent = getIntent();
        if (intent.hasExtra("round_id")) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                round_id = extras.getInt("round_id");
            }
        }

        RaceTimerFragNextRound f = new RaceTimerFragNextRound();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.dialog1, f, "racetimerfragnextround");
        ft.commit();
        mCurrentFragment = f;

        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            // disable back button
            return false;
        }

        return super.onKeyDown(keyCode, event);
    }

    public void onResume() {
        super.onResume();
        registerReceiver(onBroadcast, new IntentFilter(IComm.RCV_UPDATE));
    }

    public void onPause() {
        super.onPause();

        unregisterReceiver(onBroadcast);

    }

    // Binding for Service->UI Communication
    private BroadcastReceiver onBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(IComm.MSG_SERVICE_CALLBACK)) {
                Bundle extras = intent.getExtras();
                if (extras == null) {
                    return;
                }

                String data = extras.getString(IComm.MSG_SERVICE_CALLBACK);

                if (data == null) {
                    return;
                }

                if (mCurrentFragment == null) {
                    return;
                }

                if (data.equals("start_pressed")) {
                    ((RaceTimerFrag) mCurrentFragment).startPressed();
                }


            }
        }
    };

}
