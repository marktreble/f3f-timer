/*
 * RaceTimerActivity
 * Main Timer UI
 * Presented in 3 fragments
 */

package com.marktreble.f3ftimer.dialog;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;


public class RaceTimerActivity extends FragmentActivity {
	
	public Pilot mPilot;
	public Race mRace;
    public int mRound;
	public String mNumber;
	public boolean mWindLegal;
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

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().setFlags(LayoutParams.FLAG_NOT_TOUCH_MODAL,
				LayoutParams.FLAG_NOT_TOUCH_MODAL);

		// ...but notify us that it happened.
		getWindow().setFlags(LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
				LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.race_timer);

		int pid;
		int rid;
	
		mContext = this;
		mActivity = this;
		
		Intent intent = getIntent();
		if (intent.hasExtra("pilot_id")){
			Bundle extras = intent.getExtras();
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
			String tag = "racetimerfrag"+Integer.toString(mCurrentFragmentId);

		    mCurrentFragment = (RaceTimerFrag)fm.findFragmentByTag(tag);

		} else {
            // Pass the race/pilot details to the service
            Log.d("startPilot:", "Race ID = "+Integer.toString(mRace.id));
            Log.d("startPilot:", "Pilot ID = "+Integer.toString(mPilot.id));
            Log.d("startPilot:", "Round ID = "+Integer.toString(mRound));

            Intent i = new Intent("com.marktreble.f3ftimer.onUpdateFromUI");
            i.putExtra("com.marktreble.f3ftimer.ui_callback", "start_pilot");
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

		mResize = (ImageView)findViewById(R.id.window_resize);
		mResize.setVisibility(View.VISIBLE);
		mResize.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mWindowState == WINDOW_STATE_FULL){
					mWindowState = WINDOW_STATE_MINIMIZED;
					mResize.setImageDrawable(ContextCompat.getDrawable(mContext ,R.drawable.expand));
					setMinimized(true);

				} else {
					mWindowState = WINDOW_STATE_FULL;
					mResize.setImageDrawable(ContextCompat.getDrawable(mContext ,R.drawable.minimize));
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
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
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
	public void onRestoreInstanceState(Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);

		int rid=savedInstanceState.getInt("race_id");
		int pid=savedInstanceState.getInt("pilot_id");

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
	
	public void onResume(){

        super.onResume();
     	registerReceiver(onBroadcast, new IntentFilter("com.marktreble.f3ftimer.onUpdate"));

		FrameLayout layout = (FrameLayout) findViewById(R.id.dialog1).getRootView();
		layout.post(new Runnable() {
			@Override
			public void run() {
				// Retain the mimimized state when rotated
				if (mWindowState == WINDOW_STATE_MINIMIZED){
					mResize.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.expand));
					setMinimized(false);
				}
			}
		});


	}
	
	public void onPause(){
		super.onPause();
		
		unregisterReceiver(onBroadcast);
        hideProgress();
	}
	
	public void onDestroy(){
		super.onDestroy();
        if (isFinishing()) {
            if (mADialog != null) {
                mADialog.dismiss();
                mADialog = null;

            }
        }

    }
	
	public void getFragment(Fragment f, int id){
		f.setRetainInstance(true);
		FragmentManager fm = getSupportFragmentManager();
    	FragmentTransaction ft = fm.beginTransaction();
    	String tag = "racetimerfrag"+Integer.toString(id);
    	ft.replace(R.id.dialog1, f, tag);
    	ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN );
    	ft.commit();
    	mCurrentFragment = (RaceTimerFrag)f;
    	mCurrentFragmentId = id;
	}
		
	public void stopTimerService(){
    	Intent serviceIntent = new Intent("com.marktreble.f3ftimer.RaceTimerService");
    	stopService(serviceIntent);
	}
	
	
	public void scorePilotZero(Pilot p){
		RacePilotData datasource = new RacePilotData(this);
  		datasource.open();
		datasource.setPilotTimeInRound(mRace.id, p.id, mRound, 0);
		datasource.close();
	}
	
    public void reflight(){
        RacePilotData datasource = new RacePilotData(this);
        datasource.open();
        datasource.setPilotTimeInRound(mRace.id, mPilot.id, mRound, 0);
        datasource.setReflightInRound(mRace.id, mPilot.id, mRound);
        datasource.close();

        mActivity.setResult(RaceActivity.RESULT_OK);
        finish();
    }
    
	// Binding for UI->Service Communication
	public void sendCommand(String cmd){
		Intent i = new Intent("com.marktreble.f3ftimer.onUpdateFromUI");
		i.putExtra("com.marktreble.f3ftimer.ui_callback", cmd);
        Log.d("SEND COMMAND", cmd);
		sendBroadcast(i);		
	}

	// Binding for UI->Service Communication
	public void sendOrderedCommand(String cmd){
		Intent i = new Intent("com.marktreble.f3ftimer.onUpdateFromUI");
		i.putExtra("com.marktreble.f3ftimer.ui_callback", cmd);
		i.putExtra("com.marktreble.f3ftimer.round", mRound);
		sendOrderedBroadcast(i, null);
	}

	// Binding for Service->UI Communication
    private BroadcastReceiver onBroadcast = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.hasExtra("com.marktreble.f3ftimer.service_callback")){
				Bundle extras = intent.getExtras();
				String data = extras.getString("com.marktreble.f3ftimer.service_callback");
				if (data == null){
					return;
				}
				
				if (mCurrentFragment == null){
					return;
				}
				if (data.equals("show_progress")){
                    Log.d("TTS", "DIALOG SHOWN");
					showProgress();
				}

				if (data.equals("hide_progress")){
                    Log.d("TTS", "DIALOG HID");
					hideProgress();
				}

				if (data.equals("off_course")){
					if (mCurrentFragment.getClass().equals(RaceTimerFrag3.class)){
						((RaceTimerFrag3)mCurrentFragment).setOffCourse();
					} 
				}
			
				if (data.equals("on_course")){
					// Check for the current fragment
					// Only call next if the current fragment is RaceTimerFrag3
					// if it has already moved on to RaceTimeFrag4, then this is a late buzz
					if (mCurrentFragment.getClass().equals(RaceTimerFrag3.class)){
						((RaceTimerFrag3)mCurrentFragment).next();
					}
				}

				if (data.equals("leg_complete")){
					long time = extras.getLong("com.marktreble.f3ftimer.time");
					int number = extras.getInt("com.marktreble.f3ftimer.number");
                    if (mCurrentFragment.getClass().equals(RaceTimerFrag4.class)) {
                        ((RaceTimerFrag4) mCurrentFragment).setLeg(number, time);
                    }
				}

				if (data.equals("run_complete")){
					Float time = extras.getFloat("com.marktreble.f3ftimer.time");
                    if (mCurrentFragment.getClass().equals(RaceTimerFrag4.class)) {
                        ((RaceTimerFrag4) mCurrentFragment).setFinal(time);
                    }
				}

				if (data.equals("run_finalised")){
					if (mCurrentFragment.getClass().equals(RaceTimerFrag4.class)) {
						((RaceTimerFrag4) mCurrentFragment).cont();
					}
			    	// End the activity

			    	//mActivity.setResult(RaceActivity.RESULT_OK);
			    	//mActivity.finish();
					
				}
				
				if (data.equals("wind_illegal")){
					mWindLegal = false;
					(mCurrentFragment).setWindWarning(true);
				}

				if (data.equals("wind_legal")){
					mWindLegal = true;
					(mCurrentFragment).setWindWarning(false);
				}

				if (data.equals("start_pressed")){
					(mCurrentFragment).startPressed();
				}
				
				if (data.equals("cancel")){
					finish();
				}
				
				if (data.equals("no_out_stream")){
					mADialog = new AlertDialog.Builder(mContext)
				    .setTitle("No Output Stream Available")
				    .setMessage("Check that the timer board is plugged in, and powered?")
				    .setPositiveButton(android.R.string.ok, null).show();
					
					
				}

                if (data.equals("driver_stopped")){
                    // End the activity
                    mActivity.setResult(RaceActivity.RESULT_ABORTED);
                    //mActivity.finish();

                }


            }
		}
        };
    
    private void showProgress(){
        mPDialog = new ProgressDialog(mContext);
        mPDialog.setMessage("Loading Language...");
        mPDialog.setCancelable(false);
        mPDialog.show();
    }
    
    private void hideProgress(){
        if (mPDialog != null){
            mPDialog.cancel();
            mPDialog.dismiss();
            mPDialog = null;
        }
    }

    private void setMinimized(boolean animated){
		Resources r = getResources();
		int px = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, r.getDisplayMetrics());

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

	private void setExpanded(){
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
