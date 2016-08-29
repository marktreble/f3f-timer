/*
 * RaceTimerFrag3
 * Climbout
 */
package com.marktreble.f3ftimer.dialog;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class RaceTimerFrag3 extends RaceTimerFrag {

	private Handler mHandler = new Handler();
	private long mStart;

	public RaceTimerFrag3(){
		
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
	    } else {
	    	mStart = System.currentTimeMillis();
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
        mView = inflater.inflate(R.layout.race_timer_frag3, container, false);
        
		Button ab = (Button) mView.findViewById(R.id.button_abort);
	    ab.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	mHandler.removeCallbacks(updateClock);
	        	RaceTimerActivity a = (RaceTimerActivity)getActivity();
	        	a.sendCommand("abort");
	        	//a.setResult(RaceActivity.RESULT_ABORTED, null);
	        	//a.finish();
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
			
			int s = (int) Math.floor(seconds);
			RaceTimerActivity a = (RaceTimerActivity)getActivity();
			
			
			if (s==10) a.sendCommand("20");
			if (s==15) a.sendCommand("15");
			if (s==20) a.sendCommand("10");
			if (s==21) a.sendCommand("9");
			if (s==22) a.sendCommand("8");
			if (s==23) a.sendCommand("7");
			if (s==24) a.sendCommand("6");
			if (s==25) a.sendCommand("5");
			if (s==26) a.sendCommand("4");
			if (s==27) a.sendCommand("3");
			if (s==28) a.sendCommand("2");
			if (s==29) a.sendCommand("1");
			
        	if (s==30){
        		// Runout of climbout time
        		// Force the server to start the clock
				a.sendCommand("0"); // Informs the driver that this was a late entry
        		next();
        		
        	} else {
        		mHandler.postDelayed(updateClock, 10);
        	}

		}
	};
	
	public void setOffCourse(){
		TextView status = (TextView) mView.findViewById(R.id.status);
		status.setText("Off The Course");
	}

	public void next(){
		mHandler.removeCallbacks(updateClock);
    	RaceTimerActivity a = (RaceTimerActivity)getActivity();
   		a.getFragment(new RaceTimerFrag4(), 4);
	}

	public void startPressed(){
		// Ignore
	}
	
 }
