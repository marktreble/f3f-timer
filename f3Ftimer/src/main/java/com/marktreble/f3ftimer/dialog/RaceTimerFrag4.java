/*
 * RaceTimerFrag4
 * Clock has started
 */
package com.marktreble.f3ftimer.dialog;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.os.Bundle;
import android.os.Handler;

public class RaceTimerFrag4 extends RaceTimerFrag {

	private Handler mHandler = new Handler();
	private long mStart;
	private int mLap = 0;
	private long mEstimate = 0;
	private Float mFinalTime = -1.0f;
	
    private boolean mClickedOnce = false;
    private boolean mStartPressed = false;

	public RaceTimerFrag4(){
		
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
        mView = inflater.inflate(R.layout.race_timer_frag4, container, false);
        
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

        Button refly = (Button) mView.findViewById(R.id.button_refly);
        refly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickedOnce) return;
                mClickedOnce = true;
				mStartPressed = true;
                reflight();

            }
        });
        
        Button fin = (Button) mView.findViewById(R.id.button_finish);
	    fin.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
                if (mClickedOnce) return;
                mClickedOnce = true;
				mStartPressed = true;
	        	next();
	            
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

        setLeg(mLap, mEstimate);
		if (mFinalTime>=0)
			setFinal(mFinalTime);
		
		super.setPilotName();
		
		return mView;
	}	
	
	private Runnable updateClock = new Runnable(){
		public void run(){
        	long elapsed = System.currentTimeMillis() - mStart;
        	float seconds = (float)elapsed/1000;

			TextView cd = (TextView) mView.findViewById(R.id.time);
			String str_time = String.format("%.2f", seconds);
			if (mLap>0){
				str_time += String.format(" T%d", mLap);
			}
			cd.setText(str_time);
			
       		mHandler.postDelayed(updateClock, 10);

		}
	};

	public void setLeg(int number, long estimated){
		// Stop the clock here
		if (number == 10){
            long elapsed = System.currentTimeMillis() - mStart;
            mHandler.removeCallbacks(updateClock);
            TextView cd = (TextView) mView.findViewById(R.id.time);
            cd.setText("");

            RaceTimerActivity a = (RaceTimerActivity)getActivity();
            a.sendCommand(String.format("::%.2f", (float)elapsed/1000));
        }

		mLap = number;
		mEstimate = estimated;
		
		if (number>0){
			TextView lap = (TextView) mView.findViewById(R.id.lap);
			String str_lap = String.format("Turn: %d", number);
			lap.setText(str_lap);
		}
		
		if (estimated>0){
			TextView est = (TextView) mView.findViewById(R.id.estimated);
			String str_est = String.format("Est: %.2f", (float)estimated/1000);
			est.setText(str_est);
		}
		
	}
	
	public void setFinal(Float time){
		mHandler.removeCallbacks(updateClock);
		TextView cd = (TextView) mView.findViewById(R.id.time);
		String str_time = String.format("%.2f", time);
		cd.setText(str_time);

		TextView lap = (TextView) mView.findViewById(R.id.lap);
		lap.setText("");

		TextView est = (TextView) mView.findViewById(R.id.estimated);
		est.setText("");

		TextView status = (TextView) mView.findViewById(R.id.status);
		status.setText("Run Complete");

		Button a = (Button) mView.findViewById(R.id.button_abort);
		a.setVisibility(View.GONE);

        Button baseA = (Button) mView.findViewById(R.id.base_A);
        Button baseB = (Button) mView.findViewById(R.id.base_B);
        baseA.setVisibility(View.GONE);
        baseB.setVisibility(View.GONE);

        Button f = (Button) mView.findViewById(R.id.button_finish);
		f.setVisibility(View.VISIBLE);

        Button r = (Button) mView.findViewById(R.id.button_refly);
        r.setVisibility(View.VISIBLE);

		mFinalTime = time;
	}
		
	public void next() {
		RaceTimerActivity a = (RaceTimerActivity) getActivity();
		// Tell Driver to finalise the score
		// Driver will post back run_finalised when finished
		a.sendOrderedCommand("finalise");

	}

	public void cont(){
		RaceTimerActivity a = (RaceTimerActivity) getActivity();
		RaceTimerFrag5 f = new RaceTimerFrag5();
		f.mFinalTime = mFinalTime;
		a.getFragment(f, 5);
	}
	
    public void reflight(){
        RaceTimerActivity a = (RaceTimerActivity)getActivity();
        a.reflight();
        
    }
	public void startPressed(){
		if (mFinalTime<0) return; // Ignore if the race is still in progress
		mClickedOnce = true;
		if (!mStartPressed) {
			mStartPressed = true;
			next();
		}
	}
 }
