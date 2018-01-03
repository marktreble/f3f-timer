/*
 * RaceTimerFrag2
 * Working time
 */
package com.marktreble.f3ftimer.dialog;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.racemanager.RaceActivity;


public class RaceTimerFrag2 extends RaceTimerFrag {

	private Handler mHandler = new Handler();
	public long mStart;
	private long mLastSecond;
	
	public RaceTimerFrag2(){
		
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		RaceTimerActivity a = (RaceTimerActivity)getActivity();

        if (savedInstanceState != null) {
	    } else {
	    	mStart = System.currentTimeMillis();
			mLastSecond = 0;
		    mHandler.postDelayed(updateClock, 10);
	    }

        // Begin the timeout dialog timeout
        // Confusing? - yes. This stops the timeout being annoyingly invoked when working time has started
        // Unless of course the model is not launched before time is up!
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
		ml.setVisibility(View.VISIBLE);
	    ml.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	mHandler.removeCallbacks(updateClock);
	        	next();

   	        }
	    });
	    
        Button ab = (Button) mView.findViewById(R.id.button_abort);
		ab.setVisibility(View.VISIBLE);
		ab.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	mHandler.removeCallbacks(updateClock);
	        	RaceTimerActivity a = (RaceTimerActivity)getActivity();
	        	a.sendCommand("abort");
				a.sendCommand("begin_timeout");
				a.setResult(RaceActivity.RESULT_ABORTED, null);
	        	a.finish();
	            
				Intent i = new Intent("com.marktreble.f3ftimer.onLiveUpdate");
				i.putExtra("com.marktreble.f3ftimer.value.state", 0);
				a.sendBroadcast(i);
	        }
	    });

		TextView status = (TextView) mView.findViewById(R.id.status);
		status.setText(getString(R.string.working_time));

		super.setPilotName();

		if (((RaceTimerActivity)getActivity()).mWindowState == RaceTimerActivity.WINDOW_STATE_MINIMIZED){
			setMinimized();
		}

		return mView;
	}
	
	private Runnable updateClock = new Runnable(){
		public void run(){
        	long elapsed = System.currentTimeMillis() - mStart;
        	float seconds = (float)elapsed/1000;
        	if (seconds>30) seconds = 30;

			TextView cd = (TextView) mView.findViewById(R.id.time);
			String str_time = String.format("%.2f", 30-seconds);
			cd.setText(str_time);

			TextView min = (TextView) mView.findViewById(R.id.mintime);
			min.setText(str_time);

			/* give .5 leadtime for speaking the numbers */
			int s = (int) Math.floor(seconds + 0.5);

			RaceTimerActivity a = (RaceTimerActivity)getActivity();

			/* only send when the second changes, and not 100 times per second */
			if (s != mLastSecond) {
				/* send to ResultsServer Live Listener */
				Intent i = new Intent("com.marktreble.f3ftimer.onLiveUpdate");
				i.putExtra("com.marktreble.f3ftimer.value.workingTime", 30.0f - (float)Math.ceil(seconds));
				a.sendBroadcast(i);
			}

			if (s==10 && s != mLastSecond) a.sendCommand("20");
			if (s==15 && s != mLastSecond) a.sendCommand("15");
			if (s==20 && s != mLastSecond) a.sendCommand("10");
			if (s==21 && s != mLastSecond) a.sendCommand("9");
			if (s==22 && s != mLastSecond) a.sendCommand("8");
			if (s==23 && s != mLastSecond) a.sendCommand("7");
			if (s==24 && s != mLastSecond) a.sendCommand("6");
			if (s==25 && s != mLastSecond) a.sendCommand("5");
			if (s==26 && s != mLastSecond) a.sendCommand("4");
			if (s==27 && s != mLastSecond) a.sendCommand("3");
			if (s==28 && s != mLastSecond) a.sendCommand("2");
			if (s==29 && s != mLastSecond) a.sendCommand("1");
			if (s==30 && s != mLastSecond){
        		// Runout of working time
        		// -- pilot scores zero!
        		
        		a.scorePilotZero(a.mPilot.id);
	        	a.setResult(RaceActivity.RESULT_OK, null);
	        	a.finish();
        	} else {
        		mHandler.postDelayed(updateClock, 10);
        	}
			mLastSecond = s;
		}
	};
	
	public void next(){
		mHandler.removeCallbacks(updateClock);
 

 		RaceTimerActivity a = (RaceTimerActivity)getActivity();
 		
		// Send model launched to server
 		a.sendCommand("launch");
		
		/* send to TcpIoService for UI tracking */
		Intent i = new Intent("com.marktreble.f3ftimer.onUpdateFromUI");
		i.putExtra("com.marktreble.f3ftimer.ui_callback", "model_launched");
		a.sendBroadcast(i);

		/* send to ResultsServer Live Listener */
		i = new Intent("com.marktreble.f3ftimer.onLiveUpdate");
		i.putExtra("com.marktreble.f3ftimer.value.state", 3);
		a.sendBroadcast(i);
		
    	// Move on to 30 climbout timer
    	a.getFragment(new RaceTimerFrag3(), 3);
	}
	
	public void startPressed(){
		next();
	}

 }
