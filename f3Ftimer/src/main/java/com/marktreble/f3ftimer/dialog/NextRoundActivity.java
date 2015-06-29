/*
 * RaceTimerActivity
 * Main Timer UI
 * Presented in 3 fragments
 */

package com.marktreble.f3ftimer.dialog;


import com.marktreble.f3ftimer.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentManager;
import android.view.KeyEvent;
import android.view.WindowManager.LayoutParams;


public class NextRoundActivity extends FragmentActivity {
	
	private Fragment mCurrentFragment;
	public int round_id;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.race_timer);

		
		Intent intent = getIntent();
		if (intent.hasExtra("round_id")){
			Bundle extras = intent.getExtras();
			round_id = extras.getInt("round_id");
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
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
	    if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
	    	// disable back button
	        return false;
	    }

	    return super.onKeyDown(keyCode, event);
	}
	
	public void onResume(){
		super.onResume();
     	registerReceiver(onBroadcast, new IntentFilter("com.marktreble.f3ftimer.onUpdate"));	  
	}
	
	public void onPause(){
		super.onPause();
		
		unregisterReceiver(onBroadcast);
		
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

				if (data.equals("start_pressed")){
					((RaceTimerFrag)mCurrentFragment).startPressed();
				}
				

			}
		}
    };

}
