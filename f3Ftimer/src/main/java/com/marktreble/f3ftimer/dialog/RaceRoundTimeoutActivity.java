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

import com.marktreble.f3ftimer.R;

public class RaceRoundTimeoutActivity extends FragmentActivity {

    private Fragment mCurrentFragment;
    private int mCurrentFragmentId;
    private Context mContext;
    public Intent mIntent;

    public long mStart;
    public boolean mGroupScored;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.race_timer);

        mContext = this;

        mIntent = getIntent();
        Bundle extras = mIntent.getExtras();
        mStart = extras.getLong("start");
        mGroupScored = extras.getBoolean("group_scored");


        Fragment f;
        if (savedInstanceState != null) {
            mCurrentFragmentId = savedInstanceState.getInt("mCurrentFragmentId");

            FragmentManager fm = getSupportFragmentManager();
            String tag = "raceroundtimeoutfrag" + Integer.toString(mCurrentFragmentId);

            mCurrentFragment = fm.findFragmentByTag(tag);
        } else {
            if (mStart == 0) {
                f = new RaceRoundTimeoutCompleteFrag();
            } else {
                f = new RaceRoundTimeoutFrag();
            }
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.dialog1, f, "raceroundtimeoutfrag");
            ft.commit();
            mCurrentFragment = f;
        }

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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("start", mStart);

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        mStart = savedInstanceState.getLong("start");
    }

    public void onResume() {
        super.onResume();
        registerReceiver(onBroadcast, new IntentFilter("com.marktreble.f3ftimer.onUpdate"));
    }

    public void onPause() {
        super.onPause();

        unregisterReceiver(onBroadcast);

    }

    public void onDestroy() {
        super.onDestroy();
    }

    public void getFragment(Fragment f, int id) {
        f.setRetainInstance(true);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        String tag = "raceroundtimeoutfrag" + Integer.toString(id);
        ft.replace(R.id.dialog1, f, tag);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();
        mCurrentFragment = f;
        mCurrentFragmentId = id;
    }

    // Binding for Service->UI Communication
    private BroadcastReceiver onBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("com.marktreble.f3ftimer.service_callback")) {
                Bundle extras = intent.getExtras();
                String data = extras.getString("com.marktreble.f3ftimer.service_callback");
                if (data == null) {
                    return;
                }

                if (mCurrentFragment == null) {
                    return;
                }

                if (data.equals("start_pressed")) {
                    //((RaceTimerFrag)mCurrentFragment).startPressed();
                    // TODO
                }
            }
        }
    };
}

