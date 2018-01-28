/*
 * RaceTimerFrag1
 * Entry Point for Timer UI
 */
package com.marktreble.f3ftimer.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

public class RaceTimerFragNextRound extends RaceTimerFrag {
	
	public RaceTimerFragNextRound(){
		
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.race_timer_frag_next_round, container, false);

        Button ab = (Button) mView.findViewById(R.id.button_abort);
	    ab.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	abort();
	            
	        }
	    });

		Button fin = (Button) mView.findViewById(R.id.button_finish_race);
		fin.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finishRace();
			}
		});
	    
        Button nr = (Button) mView.findViewById(R.id.button_next_round);
	    nr.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	next();
	            
	        }
	    });
		
	    NextRoundActivity a = (NextRoundActivity)getActivity();
	    
		TextView current_round = (TextView) mView.findViewById(R.id.current_round);
		current_round.setText(String.format("%s %d", getString(R.string.end_of_round), a.round_id));

		TextView next_round = (TextView) mView.findViewById(R.id.next_round);
		next_round.setText(getString(R.string.next_round));
		
		return mView;
	}	
	

	public void abort(){
		NextRoundActivity a = (NextRoundActivity)getActivity();
    	a.setResult(RaceActivity.RESULT_ABORTED, null);
    	a.finish();
	}

	public void finishRace(){
		NextRoundActivity a = (NextRoundActivity)getActivity();
		a.setResult(RaceActivity.RACE_FINISHED, null);
		a.finish();
	}
	
	public void next(){
		NextRoundActivity a = (NextRoundActivity)getActivity();
    	a.setResult(RaceActivity.RESULT_OK, null);
    	a.finish();
	}
	
	public void startPressed(){
		next();
	}
 }
