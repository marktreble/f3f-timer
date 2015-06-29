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
        mView = inflater.inflate(R.layout.race_timer_frag1, container, false);
        
		Button swt = (Button) mView.findViewById(R.id.button_start_working_time);
	    swt.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {	        	
	        	// Progress to the next UI
	        	next();
	            
	        }
	    });
	    
		Button ml = (Button) mView.findViewById(R.id.button_model_launched);
	    ml.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v){
	        	model_launched();
	            
	        }
	    });
	    
        Button ab = (Button) mView.findViewById(R.id.button_abort);
	    ab.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	RaceTimerActivity a = (RaceTimerActivity)getActivity();
	        	a.sendCommand("abort");
	        	a.setResult(RaceActivity.RESULT_ABORTED, null);
	        	a.finish();
	            
	        }
	    });
	    
		super.setPilotName();
		
		return mView;
	}
	
	public void next(){
    	RaceTimerActivity a = (RaceTimerActivity)getActivity();
    	    	
    	a.sendCommand("working_time");
    	
    	a.getFragment(new RaceTimerFrag2(),2);
	}
	
	public void model_launched(){
    	RaceTimerActivity a = (RaceTimerActivity)getActivity();
    	    	
    	// Send model launched to server
    	 a.sendCommand("launch");
    	 		
    	a.getFragment(new RaceTimerFrag3(),3);

	}
	
	public void startPressed(){
		next();
	}
}
