/*
 * RaceTimerFrag3
 * Climbout
 */
package com.marktreble.f3ftimer.dialog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;

public class RaceTimerFrag3 extends RaceTimerFrag {

	private Handler mHandler = new Handler();
	private long mStart;
	private long mLastSecond;

	public RaceTimerFrag3(){
		
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
	    } else {
	    	mStart = System.currentTimeMillis();
			mLastSecond = 0;
		    mHandler.postDelayed(updateClock, 10);
		}
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

		Button ab = (Button) mView.findViewById(R.id.button_abort);
		ab.setVisibility(View.VISIBLE);
	    ab.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	mHandler.removeCallbacks(updateClock);
	        	RaceTimerActivity a = (RaceTimerActivity)getActivity();
	        	a.sendCommand("abort");

				Intent i = new Intent("com.marktreble.f3ftimer.onLiveUpdate");
				i.putExtra("com.marktreble.f3ftimer.value.state", 7);
				a.sendBroadcast(i);

				a.sendCommand("begin_timeout");
				a.getFragment(new RaceTimerFrag6(), 6); // Abort submenu (reflight or score 0)
	        }
	    });

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String soft_buttons = sharedPref.getString("pref_input_src", getString(R.string.Demo));
        if (soft_buttons.equals(getString(R.string.Demo))){
            Button baseA = (Button) mView.findViewById(R.id.base_A);
            baseA.setVisibility(View.VISIBLE);
            
            baseA.setOnClickListener(new Button.OnClickListener(){
                @Override
                public void onClick(View v){
                    RaceTimerActivity a = (RaceTimerActivity)getActivity();
                    a.sendCommand("baseA");
                }
            });

            Button baseB = (Button) mView.findViewById(R.id.base_B);
            baseB.setVisibility(View.VISIBLE);

            baseB.setOnClickListener(new Button.OnClickListener(){
                @Override
                public void onClick(View v){
                    RaceTimerActivity a = (RaceTimerActivity)getActivity();
                    a.sendCommand("baseB");
                }
            });

        }

		TextView status = (TextView) mView.findViewById(R.id.status);
		status.setText(getString(R.string.model_launched));

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
				Intent i = new Intent("com.marktreble.f3ftimer.onLiveUpdate");
				i.putExtra("com.marktreble.f3ftimer.value.climbOutTime", 30.0f - (float)Math.ceil(seconds));
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
        		// Runout of climbout time
        		// Force the server to start the clock
				a.sendCommand("0"); // Informs the driver that this was a late entry
        		next();
        		
        	} else {
        		mHandler.postDelayed(updateClock, 10);
        	}
			mLastSecond = s;
		}
	};
	
	public void setOffCourse(){
		TextView status = (TextView) mView.findViewById(R.id.status);
		status.setText(R.string.off_course);

		/* send to ResultsServer Live Listener */
        RaceTimerActivity a = (RaceTimerActivity)getActivity();
        Intent i = new Intent("com.marktreble.f3ftimer.onLiveUpdate");
        i.putExtra("com.marktreble.f3ftimer.value.state", 4);
        a.sendBroadcast(i);
	}

	public void next(){
		mHandler.removeCallbacks(updateClock);
    	RaceTimerActivity a = (RaceTimerActivity)getActivity();
   		a.getFragment(new RaceTimerFrag4(), 4);

		/* send to ResultsServer Live Listener */
        Intent i = new Intent("com.marktreble.f3ftimer.onLiveUpdate");
        i.putExtra("com.marktreble.f3ftimer.value.state", 5);
        a.sendBroadcast(i);
	}

	public void startPressed(){
		// Ignore
	}
	
 }
