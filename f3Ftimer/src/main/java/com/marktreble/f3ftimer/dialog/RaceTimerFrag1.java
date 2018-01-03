/*
 * RaceTimerFrag1
 * Entry Point for Timer UI
 * Waits for CD to start working time, or model launch
 */
package com.marktreble.f3ftimer.dialog;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.os.Bundle;
import android.widget.TextView;

public class RaceTimerFrag1 extends RaceTimerFrag {

	
	public RaceTimerFrag1(){
		
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.race_timer_frag2, container, false);
        
		Button swt = (Button) mView.findViewById(R.id.button_start_working_time);
		swt.setVisibility(View.VISIBLE);
	    swt.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {	        	
	        	// Progress to the next UI
	        	next();
	            
	        }
	    });

		Button ml = (Button) mView.findViewById(R.id.button_model_launched);
		ml.setVisibility(View.VISIBLE);
	    ml.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v){
	        	model_launched();
	            
	        }
	    });
	    
        Button ab = (Button) mView.findViewById(R.id.button_abort);
		ab.setVisibility(View.VISIBLE);
		ab.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	RaceTimerActivity a = (RaceTimerActivity)getActivity();
	        	a.sendCommand("abort");

				a.setResult(RaceActivity.RESULT_ABORTED, null);
	        	a.finish();
	            
				Intent i = new Intent("com.marktreble.f3ftimer.onLiveUpdate");
				i.putExtra("com.marktreble.f3ftimer.value.state", 0);
				a.sendBroadcast(i);
	        }
	    });

		TextView status = (TextView) mView.findViewById(R.id.status);
		status.setVisibility(View.GONE);

		super.setPilotName();
		
		return mView;
	}
	
	public void next(){
    	RaceTimerActivity a = (RaceTimerActivity)getActivity();
    	    	
    	a.sendCommand("working_time");

		Intent i = new Intent("com.marktreble.f3ftimer.onUpdateFromUI");
		i.putExtra("com.marktreble.f3ftimer.ui_callback", "working_time_started");
		a.sendBroadcast(i);

		i = new Intent("com.marktreble.f3ftimer.onLiveUpdate");
		i.putExtra("com.marktreble.f3ftimer.value.state", 2);
		i.putExtra("com.marktreble.f3ftimer.value.workingTime", 0.0f);
		a.sendBroadcast(i);

    	a.getFragment(new RaceTimerFrag2(),2);
	}
	
	public void model_launched(){
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

    	a.getFragment(new RaceTimerFrag3(),3);

	}
	
	public void startPressed(){
		next();
	}
}
