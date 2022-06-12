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

import android.animation.LayoutTransition;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.marktreble.f3ftimer.F3FtimerApplication;
import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.constants.IComm;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

public class RaceTimerActivity extends FragmentActivity {

    public Pilot mPilot;
    public Race mRace;
    public int mRound;
    public String mNumber;
    public boolean mWindLegal;
    public boolean mWindIllegalDuringFlight = false;
    private RaceTimerFrag mCurrentFragment;
    private int mCurrentFragmentId;
    private Context mContext;
    private FragmentActivity mActivity;
    private ProgressDialog mPDialog;
    private AlertDialog mADialog;
    private ImageView mResize;
    public int mWindowState;

    private int mNaturalHeight;

    public static int WINDOW_STATE_FULL = 0;
    public static int WINDOW_STATE_MINIMIZED = 1;

    static final String DIALOG = "dialog";

    GenericAlert mDLG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(LayoutParams.FLAG_NOT_TOUCH_MODAL,
                LayoutParams.FLAG_NOT_TOUCH_MODAL);

        // ...but notify us that it happened.
        getWindow().setFlags(LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);

        ((F3FtimerApplication) getApplication()).setTransparentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.race_timer);

        int pid;
        int rid;

        mContext = this;
        mActivity = this;

        Intent intent = getIntent();
        if (intent.hasExtra("pilot_id")) {
            Bundle extras = intent.getExtras();
            if (extras == null) {
                return;
            }

            pid = extras.getInt("pilot_id");
            rid = extras.getInt("race_id");
            mRound = extras.getInt("round");
            mNumber = extras.getString("bib_no");

            RaceData datasource = new RaceData(this);
            datasource.open();
            mRace = datasource.getRace(rid);
            datasource.setStatus(rid, Race.STATUS_IN_PROGRESS);
            datasource.close();

            RacePilotData datasource2 = new RacePilotData(this);
            datasource2.open();
            mPilot = datasource2.getPilot(pid, rid);
            datasource2.close();


        }

        if (savedInstanceState != null) {

            mCurrentFragmentId = savedInstanceState.getInt("mCurrentFragmentId");
            mWindowState = savedInstanceState.getInt("mWindowState");

            FragmentManager fm = getSupportFragmentManager();
            String tag = "racetimerfrag" + mCurrentFragmentId;

            mCurrentFragment = (RaceTimerFrag) fm.findFragmentByTag(tag);

        } else {
            // Pass the race/pilot details to the service
            Intent i = new Intent(IComm.RCV_UPDATE_FROM_UI);
            i.putExtra(IComm.MSG_UI_CALLBACK, "start_pilot");
            i.putExtra("com.marktreble.f3ftimer.pilot_id", mPilot.id);
            i.putExtra("com.marktreble.f3ftimer.race_id", mRace.id);
            i.putExtra("com.marktreble.f3ftimer.round", mRound);
            sendBroadcast(i);

            // Send an abort to reset the timer as a safety guard
            sendCommand("abort");

            RaceTimerFrag1 f;
            f = new RaceTimerFrag1();
            f.setRetainInstance(true);
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.dialog1, f, "racetimerfrag1");
            ft.commit();
            mCurrentFragment = f;
            mCurrentFragmentId = 1;

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
            mWindowState = sharedPref.getInt("pref_window_minized_state", WINDOW_STATE_FULL);

        }

        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

        mResize = findViewById(R.id.window_resize);
        mResize.setVisibility(View.VISIBLE);
        mResize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWindowState == WINDOW_STATE_FULL) {
                    mWindowState = WINDOW_STATE_MINIMIZED;
                    mResize.setImageDrawable(ContextCompat.getDrawable(mContext, R.mipmap.expand));
                    setMinimized(true);

                } else {
                    mWindowState = WINDOW_STATE_FULL;
                    mResize.setImageDrawable(ContextCompat.getDrawable(mContext, R.mipmap.minimize));
                    setExpanded();

                }

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("pref_window_minized_state", mWindowState);
                editor.apply();

            }
        });

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return !(keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0)
                && super.onKeyDown(keyCode, event);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("race_id", mRace.id);
        outState.putInt("pilot_id", mPilot.id);
        outState.putInt("round", mRound);
        outState.putInt("mCurrentFragmentId", mCurrentFragmentId);
        outState.putInt("mWindowState", mWindowState);

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        int rid = savedInstanceState.getInt("race_id");
        int pid = savedInstanceState.getInt("pilot_id");

        mRound = savedInstanceState.getInt("round");
        mCurrentFragmentId = savedInstanceState.getInt("mCurrentFragmentId");
        mWindowState = savedInstanceState.getInt("mWindowState");

        RaceData datasource = new RaceData(this);
        datasource.open();
        mRace = datasource.getRace(rid);
        datasource.close();

        RacePilotData datasource2 = new RacePilotData(this);
        datasource2.open();
        mPilot = datasource2.getPilot(pid, mRace.id);
        datasource2.close();

    }

    public void onResume() {

        super.onResume();
        registerReceiver(onBroadcast, new IntentFilter(IComm.RCV_UPDATE));

        FrameLayout layout = (FrameLayout) findViewById(R.id.dialog1).getRootView();
        layout.post(new Runnable() {
            @Override
            public void run() {
                // Retain the mimimized state when rotated
                if (mWindowState == WINDOW_STATE_MINIMIZED) {
                    mResize.setImageDrawable(ContextCompat.getDrawable(mContext, R.mipmap.expand));
                    setMinimized(false);
                }
            }
        });


    }

    public void onPause() {
        super.onPause();

        unregisterReceiver(onBroadcast);
        hideProgress();
    }

    public void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            if (mADialog != null) {
                mADialog.dismiss();
                mADialog = null;

            }
        }

    }

    public void getFragment(Fragment f, int id) {
        f.setRetainInstance(true);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        String tag = "racetimerfrag" + id;
        ft.replace(R.id.dialog1, f, tag);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();
        mCurrentFragment = (RaceTimerFrag) f;
        mCurrentFragmentId = id;
    }

    /*
    public void stopTimerService() {
        Intent serviceIntent = new Intent("com.marktreble.f3ftimer.RaceTimerService");
        stopService(serviceIntent);
    }
    */


    public void scorePilotZero(Pilot p) {
        RacePilotData datasource = new RacePilotData(this);
        datasource.open();
        datasource.setPilotTimeInRound(mRace.id, p.id, mRound, 0);
        datasource.close();
    }

    public void reflight() {
        RacePilotData datasource = new RacePilotData(this);
        datasource.open();
        datasource.setPilotTimeInRound(mRace.id, mPilot.id, mRound, 0);
        datasource.setReflightInRound(mRace.id, mPilot.id, mRound);
        datasource.close();

        mActivity.setResult(RaceActivity.RESULT_OK);
        finish();
    }

    // Binding for UI->Service Communication
    public void sendCommand(String cmd) {
        Intent i = new Intent(IComm.RCV_UPDATE_FROM_UI);
        i.putExtra(IComm.MSG_UI_CALLBACK, cmd);
        Log.d("SEND COMMAND", cmd);
        sendBroadcast(i);
    }

    // Binding for UI->Service Communication
    public void sendOrderedCommand(String cmd) {
        Intent i = new Intent(IComm.RCV_UPDATE_FROM_UI);
        i.putExtra(IComm.MSG_UI_CALLBACK, cmd);
        i.putExtra("com.marktreble.f3ftimer.round", mRound);
        sendOrderedBroadcast(i, null);
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
                if (data.equals("show_progress")) {
                    showProgress();
                }

                if (data.equals("hide_progress")) {
                    hideProgress();
                }

                if (data.equals("off_course")) {
                    if (mCurrentFragment.getClass().equals(RaceTimerFrag3.class)) {
                        ((RaceTimerFrag3) mCurrentFragment).setOffCourse();
                    }
                }

                if (data.equals("on_course")) {
                    // Check for the current fragment
                    // Only call next if the current fragment is RaceTimerFrag3
                    // if it has already moved on to RaceTimeFrag4, then this is a late buzz
                    if (mCurrentFragment.getClass().equals(RaceTimerFrag3.class)) {
                        ((RaceTimerFrag3) mCurrentFragment).next();
                    }
                }

                if (data.equals("leg_complete")) {
                    int legNumber = extras.getInt("com.marktreble.f3ftimer.number");
                    long estimatedFlightTime = extras.getLong("com.marktreble.f3ftimer.estimate");
                    long legTime = extras.getLong("com.marktreble.f3ftimer.legTime");
                    long fastestLegTime = extras.getLong("com.marktreble.f3ftimer.fastestLegTime");
                    String fastestFlightPilot = extras.getString("com.marktreble.f3ftimer.fastestFlightPilot");
                    long deltaTime = extras.getLong("com.marktreble.f3ftimer.delta");
                    if (mCurrentFragment.getClass().equals(RaceTimerFrag4.class)) {
                        ((RaceTimerFrag4) mCurrentFragment).setLeg(legNumber, estimatedFlightTime, fastestLegTime, legTime, deltaTime, fastestFlightPilot);
                    }
                }

                if (data.equals("run_complete")) {
                    float time = extras.getFloat("com.marktreble.f3ftimer.time");
                    float fastestFlightTime = extras.getFloat("com.marktreble.f3ftimer.fastestFlightTime");
                    String fastestFlightPilot = extras.getString("com.marktreble.f3ftimer.fastestFlightPilot");

                    if (mCurrentFragment.getClass().equals(RaceTimerFrag4.class)) {
                        if (mWindIllegalDuringFlight) {
                            // show wind warning if it was illegal during flight
                            mCurrentFragment.setWindWarning(true);
                        }
                        ((RaceTimerFrag4) mCurrentFragment).setFinal(time, fastestFlightTime, fastestFlightPilot);
                    }
                }

                if (data.equals("run_finalised")) {
                    if (mCurrentFragment.getClass().equals(RaceTimerFrag4.class)) {
                        ((RaceTimerFrag4) mCurrentFragment).cont();
                    }
                }

                if (data.equals("wind_illegal")) {
                    mWindLegal = false;
                    (mCurrentFragment).setWindWarning(true);
                }

                if (data.equals("wind_legal")) {
                    mWindLegal = true;
                    (mCurrentFragment).setWindWarning(false);
                }

                if (data.equals("start_pressed")) {
                    (mCurrentFragment).startPressed();
                }

                if (data.equals("cancel")) {
                    finish();
                }

                if (data.equals("no_out_stream")) {
                    String[] buttons_array = new String[1];
                    buttons_array[0] = getString(android.R.string.ok);

                    mDLG = GenericAlert.newInstance(
                            getString(R.string.ttl_no_output_stream),
                            getString(R.string.msg_no_output_stream),
                            buttons_array,
                            null
                    );

                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.addToBackStack(null);
                    ft.add(mDLG, DIALOG);
                    ft.commit();
                }

                if (data.equals("driver_stopped")) {
                    // End the activity
                    mActivity.setResult(RaceActivity.RESULT_ABORTED);
                }
            }
        }
    };

    private void showProgress() {
        mPDialog = new ProgressDialog(mContext);
        mPDialog.setMessage("Loading Language...");
        mPDialog.setCancelable(false);
        mPDialog.show();
    }

    private void hideProgress() {
        if (mPDialog != null) {
            mPDialog.cancel();
            mPDialog.dismiss();
            mPDialog = null;
        }
    }

    private void setMinimized(boolean animated) {
        Resources r = getResources();
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, r.getDisplayMetrics());

        mCurrentFragment.setMinimized();

        FrameLayout layout = (FrameLayout) findViewById(R.id.dialog1).getRootView();

        if (animated) {
            LayoutTransition transition = new LayoutTransition();
            transition.enableTransitionType(LayoutTransition.CHANGING);
            layout.setLayoutTransition(transition);
        }
        if (mNaturalHeight == 0) mNaturalHeight = layout.getLayoutParams().height;

        layout.getLayoutParams().height = px;
        getWindow().setGravity(Gravity.TOP);

        if (animated) {
            layout.setLayoutTransition(null);
        }

    }

    private void setExpanded() {
        FrameLayout layout = (FrameLayout) findViewById(R.id.dialog1).getRootView();
        LayoutTransition transition = new LayoutTransition();
        transition.enableTransitionType(LayoutTransition.CHANGING);
        layout.setLayoutTransition(transition);
        layout.getLayoutParams().height = mNaturalHeight;
        getWindow().setGravity(Gravity.CENTER);

        layout.setLayoutTransition(null);

        mCurrentFragment.setExpanded();

    }


}
