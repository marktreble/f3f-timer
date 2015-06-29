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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager.LayoutParams;


public class RaceTimerActivity extends FragmentActivity {
	
	public Pilot mPilot;
	public Race mRace;
    public int mRound;
	public boolean mWindLegal;
	private RaceTimerFrag mCurrentFragment;
	private int mCurrentFragmentId;
	private Context mContext;
	private FragmentActivity mActivity;
	private ProgressDialog mPDialog;
	private AlertDialog mADialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
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
        }

		getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
	    if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
	    	// disable back button
	        return false;
	    }

	    return super.onKeyDown(keyCode, event);
	}
	
	@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("race_id", mRace.id);
        outState.putInt("pilot_id", mPilot.id);
        outState.putInt("round", mRound);
        outState.putInt("mCurrentFragmentId", mCurrentFragmentId);

    }
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState){
		int rid=savedInstanceState.getInt("race_id");
		int pid=savedInstanceState.getInt("pilot_id");

        mRound = savedInstanceState.getInt("round");
        mCurrentFragmentId = savedInstanceState.getInt("mCurrentFragmentId");

		RaceData datasource = new RaceData(this);
  		datasource.open();
  		mRace = datasource.getRace(rid);
  		datasource.close();
  		
		RacePilotData datasource2 = new RacePilotData(this);
  		datasource2.open();
  		mPilot = datasource2.getPilot(pid, 0);
  		datasource2.close();

	}
	
	public void onResume(){

        super.onResume();
     	registerReceiver(onBroadcast, new IntentFilter("com.marktreble.f3ftimer.onUpdate"));
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
			    	// End the activity
			    	mActivity.setResult(RaceActivity.RESULT_OK);
			    	mActivity.finish();	
					
				}
				
				if (data.equals("wind_illegal")){
					mWindLegal = false;
					((RaceTimerFrag)mCurrentFragment).setWindWarning(true);
				}

				if (data.equals("wind_legal")){
					mWindLegal = true;
					((RaceTimerFrag)mCurrentFragment).setWindWarning(false);
				}

				if (data.equals("start_pressed")){
					((RaceTimerFrag)mCurrentFragment).startPressed();
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
}
