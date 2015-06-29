/*
 * RaceTimerFrag1
 * Entry Point for Timer UI
 * Working time
 */
package com.marktreble.f3ftimer.dialog;

import java.lang.System;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.os.Bundle;
import android.os.Handler;


public class RaceTimerFrag2 extends RaceTimerFrag {

	private Handler mHandler = new Handler();
	public long mStart;
	
	public RaceTimerFrag2(){
		
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (savedInstanceState != null) {
	    } else {
	    	mStart = System.currentTimeMillis();
		    mHandler.postDelayed(updateClock, 10);
	    }
        
        // Begin the timeout dialog timeout
        // Confusing? - yes. This stops the timeout being annoyingly invoked when working time has started
        // Unless of course the model is not launched before time is up!
        RaceTimerActivity a = (RaceTimerActivity)getActivity();
    	a.sendCommand("timeout_resumed");
    }
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		mHandler.removeCallbacks(updateClock);
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.race_timer_frag2, container, false);
        
		Button ml = (Button) mView.findViewById(R.id.button_model_launched);
	    ml.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	mHandler.removeCallbacks(updateClock);
	        	next();

   	        }
	    });
	    
        Button ab = (Button) mView.findViewById(R.id.button_abort);
	    ab.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	mHandler.removeCallbacks(updateClock);
	        	RaceTimerActivity a = (RaceTimerActivity)getActivity();
	        	a.sendCommand("abort");
	        	a.setResult(RaceActivity.RESULT_ABORTED, null);
	        	a.finish();
	            
	        }
	    });
	    
		super.setPilotName();
		    	
		return mView;
	}
	
	private Runnable updateClock = new Runnable(){
		public void run(){
        	long elapsed = System.currentTimeMillis() - mStart;
        	float seconds = (float)elapsed/1000;
        	if (seconds>30) seconds = 30;

			TextView cd = (TextView) mView.findViewById(R.id.countdown);
			String str_time = String.format("%.2f", 30-seconds);
			cd.setText(str_time);
			
        	if (seconds==30){
        		// Runout of working time
        		// -- pilot scores zero!
        		
        		RaceTimerActivity a = (RaceTimerActivity)getActivity();
        		a.scorePilotZero(a.mPilot);
	        	a.setResult(RaceActivity.RESULT_OK, null);
	        	a.finish();
        	} else {
        		mHandler.postDelayed(updateClock, 10);
        	}

		}
	};
	
	public void next(){
		mHandler.removeCallbacks(updateClock);
 

 		RaceTimerActivity a = (RaceTimerActivity)getActivity();
 		
		// Send model launched to server
 		a.sendCommand("launch");
		
		
    	// Move on to 30 climbout timer
    	a.getFragment(new RaceTimerFrag3(), 3);
	}
	
	public void startPressed(){
		next();
	}

 }
