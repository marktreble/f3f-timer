/*
 * RaceTimerFrag4
 * Clock has started
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
import com.marktreble.f3ftimer.racemanager.RaceActivity;

public class RaceTimerFrag4 extends RaceTimerFrag {

	private Handler mHandler = new Handler();
	private long mStart;
	private int mLap = 0;
	private long mEstimate = 0;
	private Float mFinalTime = -1.0f;
	private long mLastSecond = 0;
	
    private boolean mClickedOnce = false;
    private boolean mStartPressed = false;
	private int status = 0;

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
        mView = inflater.inflate(R.layout.race_timer_frag2, container, false);
        
        Button ab = (Button) mView.findViewById(R.id.button_abort);
		ab.setText(getResources().getText(R.string.abort));
		ab.setVisibility(View.VISIBLE);
	    ab.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	mHandler.removeCallbacks(updateClock);
	        	RaceTimerActivity a = (RaceTimerActivity)getActivity();
	        	a.sendCommand("abort");
				a.sendCommand("begin_timeout");
				a.getFragment(new RaceTimerFrag6(), 6); // Abort submenu (reflight or score 0)
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

		TextView statusText = (TextView) mView.findViewById(R.id.status);
		if (status == 0) {
			statusText.setText(R.string.on_course);
		} else {
			statusText.setText(R.string.off_course);
		}

        setLeg(mLap, mEstimate, 0, 0, 0, "");
		if (mFinalTime>=0)
			setFinal(mFinalTime, 0, "");
		
		super.setPilotName();

		if (((RaceTimerActivity)getActivity()).mWindowState == RaceTimerActivity.WINDOW_STATE_MINIMIZED) {
			setMinimized();
		}

		return mView;
	}	

	public void setOnCourse(){
		if (mView == null) {
			status = 0;
		} else {
			TextView status = (TextView) mView.findViewById(R.id.status);
			status.setText(R.string.on_course);
		}
	}

	public void setOffCourse(){
		if (mView == null) {
			status = 1;
		} else {
			TextView status = (TextView) mView.findViewById(R.id.status);
			status.setText(R.string.off_course);
		}
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

			int s = (int) Math.floor(seconds);
			if (s != mLastSecond) {
				Intent i = new Intent("com.marktreble.f3ftimer.onLiveUpdate");
				i.putExtra("com.marktreble.f3ftimer.value.flightTime", 0.0f + s);
				RaceTimerActivity a = (RaceTimerActivity) getActivity();
				a.sendBroadcast(i);
			}
			mLastSecond = s;

			TextView min = (TextView) mView.findViewById(R.id.mintime);
			min.setText(str_time);

			mHandler.postDelayed(updateClock, 10);

		}
	};

	public void setLeg(int number, long estimated, long fastestLegTime, long legTime, long deltaTime, String fastestFlightPilot){
		RaceTimerActivity a = (RaceTimerActivity)getActivity();

		// Stop the clock here
		if (number == 10 && mFinalTime<0){
            long elapsed = System.currentTimeMillis() - mStart;
            mHandler.removeCallbacks(updateClock);
            TextView cd = (TextView) mView.findViewById(R.id.time);
            cd.setText("");

			TextView min = (TextView) mView.findViewById(R.id.mintime);
			min.setText("");

            Intent i = new Intent("com.marktreble.f3ftimer.onLiveUpdate");
            i.putExtra("com.marktreble.f3ftimer.value.flightTime", elapsed/1000.0f);
			a.sendBroadcast(i);
            a.sendCommand(String.format("::%.2f", (float)elapsed/1000));
        }

		mLap = number;
		mEstimate = estimated;
		
		if (number>0){
			Intent i = new Intent("com.marktreble.f3ftimer.onLiveUpdate");
			i.putExtra("com.marktreble.f3ftimer.value.turnNumber", number);
            i.putExtra("com.marktreble.f3ftimer.value.legTime", legTime / 1000.0f);
			i.putExtra("com.marktreble.f3ftimer.value.fastestLegTime", fastestLegTime / 1000.0f);
			i.putExtra("com.marktreble.f3ftimer.value.fastestLegPilot", fastestFlightPilot);
			i.putExtra("com.marktreble.f3ftimer.value.deltaTime", deltaTime / 1000.0f);
			i.putExtra("com.marktreble.f3ftimer.value.estimatedTime", estimated / 1000.0f);
			a.sendBroadcast(i);

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
	
	public void setFinal(Float time, float fastestFlightTime, String fastestFlightPilot){
		mHandler.removeCallbacks(updateClock);
		TextView cd = (TextView) mView.findViewById(R.id.time);
		String str_time = String.format("%.2f", time);
		cd.setText(str_time);

		TextView min = (TextView) mView.findViewById(R.id.mintime);
		min.setText(str_time);

		TextView lap = (TextView) mView.findViewById(R.id.lap);
		lap.setText("");

		TextView est = (TextView) mView.findViewById(R.id.estimated);
		est.setText("");

		TextView status = (TextView) mView.findViewById(R.id.status);
		status.setText("Run Complete");

		Button abort = (Button) mView.findViewById(R.id.button_abort);
		abort.setVisibility(View.GONE);

        Button baseA = (Button) mView.findViewById(R.id.base_A);
        Button baseB = (Button) mView.findViewById(R.id.base_B);
        baseA.setVisibility(View.GONE);
        baseB.setVisibility(View.GONE);

        Button f = (Button) mView.findViewById(R.id.button_finish);
		f.setVisibility(View.VISIBLE);

        Button r = (Button) mView.findViewById(R.id.button_refly);
        r.setVisibility(View.VISIBLE);

		mFinalTime = time;

		// Start Round Timeout now
		RaceTimerActivity a = (RaceTimerActivity) getActivity();
		a.sendCommand("begin_timeout");

		/* send to ResultsServer Live Listener */
		Intent i = new Intent("com.marktreble.f3ftimer.onLiveUpdate");
        i.putExtra("com.marktreble.f3ftimer.value.fastestFlightTime", fastestFlightTime);
		i.putExtra("com.marktreble.f3ftimer.value.fastestFlightPilot", fastestFlightPilot);
		i.putExtra("com.marktreble.f3ftimer.value.state", 6);
		a.sendBroadcast(i);
	}
		
	public void next() {
		RaceTimerActivity a = (RaceTimerActivity) getActivity();
		// Tell Driver to finalise the score
		// Driver will post back run_finalised when finished
		a.sendOrderedCommand("finalise");
	}

	public void cont(){
		RaceTimerActivity a = (RaceTimerActivity) getActivity();

		/* send to ResultsServer Live Listener */
		Intent i = new Intent("com.marktreble.f3ftimer.onLiveUpdate");
		i.putExtra("com.marktreble.f3ftimer.value.state", 0);
		a.sendBroadcast(i);
		
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(a);
		boolean automatic_progression = sharedPref.getBoolean("pref_automatic_pilot_progression", false);
		if (automatic_progression) {
			a.sendCommand("abort");
			a.setResult(RaceActivity.RESULT_OK);
			a.finish();
		} else {
			RaceTimerFrag5 f = new RaceTimerFrag5();
			f.mFinalTime = mFinalTime;
			a.getFragment(f, 5);
		}
	}
	
    public void reflight(){
        RaceTimerActivity a = (RaceTimerActivity)getActivity();
        a.reflight();
        
		/* send to ResultsServer Live Listener */
		Intent i = new Intent("com.marktreble.f3ftimer.onLiveUpdate");
		i.putExtra("com.marktreble.f3ftimer.value.state", 0);
		a.sendBroadcast(i);
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
