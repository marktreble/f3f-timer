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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.WindowManager.LayoutParams;

import com.marktreble.f3ftimer.F3FtimerApplication;
import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.constants.IComm;

public class RaceRoundTimeoutActivity extends FragmentActivity {

    private Fragment mCurrentFragment;
    private int mCurrentFragmentId;
    public Intent mIntent;

    public long mStart;
    public boolean mGroupScored;

    final private static String RACE_ROUND_TIMEOUT_FRAG_TAG = "raceroundtimeoutfrag";

    final private static String K_START = "start";
    final private static String K_CUR_FRAG_ID = "current_fragment_id";
    final private static String K_GROUP_SCORED = "group_scored";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((F3FtimerApplication) getApplication()).setTransparentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.race_timer);

        mIntent = getIntent();
        Bundle extras = mIntent.getExtras();
        if (extras != null) {
            mStart = extras.getLong(K_START);
            mGroupScored = extras.getBoolean(K_GROUP_SCORED);
        }


        if (savedInstanceState != null) {
            mCurrentFragmentId = savedInstanceState.getInt(K_CUR_FRAG_ID);
            mStart = savedInstanceState.getLong(K_START);

            FragmentManager fm = getSupportFragmentManager();
            String tag = RACE_ROUND_TIMEOUT_FRAG_TAG + mCurrentFragmentId;

            mCurrentFragment = fm.findFragmentByTag(tag);
        } else {
            if (mStart == 0) {
                getFragment(new RaceRoundTimeoutCompleteFrag(), 2);
            } else {
                getFragment(new RaceRoundTimeoutFrag(), 1);
            }
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
        outState.putLong(K_START, mStart);
        outState.putInt(K_CUR_FRAG_ID, mCurrentFragmentId);
        super.onSaveInstanceState(outState);
    }

    public void onResume() {
        super.onResume();
        registerReceiver(onBroadcast, new IntentFilter(IComm.RCV_UPDATE));
    }

    public void onPause() {
        super.onPause();

        unregisterReceiver(onBroadcast);

    }

    public void onDestroy() {
        super.onDestroy();
    }

    public void getFragment(Fragment f, int id) {
        //f.setRetainInstance(true);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        String tag = RACE_ROUND_TIMEOUT_FRAG_TAG + id;
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
            if (intent.hasExtra(IComm.MSG_SERVICE_CALLBACK)) {
                Bundle extras = intent.getExtras();
                if (extras == null) return;

                String data = extras.getString(IComm.MSG_SERVICE_CALLBACK, "");
                if (data == null) {
                    return;
                }

                if (mCurrentFragment == null) {
                    return;
                }

                if (data.equals("start_pressed")) {
                    // Dismiss the activity if the timer is still counting down
                    // If the timeout has completed force the user to interact with the screen
                    if (mCurrentFragmentId == 1) {
                        finish();
                    }
                }
            }
        }
    };
}

