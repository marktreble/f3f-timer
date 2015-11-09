/*
 * NewRaceFrag2
 * Page 2 for New Race form
 * Pilot Picker
 * with Random/Manual Order Shuffling
 */
package com.marktreble.f3ftimer.dialog;

import java.util.ArrayList;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.pilot.PilotData;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.content.Intent;
import android.os.Bundle;

public class NewRaceFrag2 extends ListFragment {

	private ArrayAdapter<String> mArrAdapter;
	private LayoutInflater mInflater;
	
	private ArrayList<Integer> mArrSelectedIds;	// ArrayList of selected pilot ids is same order as mArrSelected
	
	private ArrayList<String> mArrNames; // ArrayList of all pilots in database
	private ArrayList<Integer> mArrIds;	 // ArrayList of database ids (order matching mArrNames - alphabetical order)

	private TextView next_number;

	public NewRaceFrag2(){
		
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
	    mArrSelectedIds = new ArrayList<Integer>();

        NewRaceActivity a = (NewRaceActivity)getActivity();
        if (a.pilots!=null)
            mArrSelectedIds = a.pilots;
        
	    this.getNamesArray();
	
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		mInflater = inflater;
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.race_new_frag2, container, false);
        
        // Listener for next button
		Button next = (Button) v.findViewById(R.id.button_next);
	    next.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	// Save to database and quit the activity
	        	NewRaceActivity a = (NewRaceActivity)getActivity();
	        	a.pilots = mArrSelectedIds;
	        	a.getFragment(new NewRaceFrag3(), "newracefrag3");
	        }
	    });

		// Listener for skip button
		Button skip = (Button) v.findViewById(R.id.button_skip);
		skip.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Save to database and quit the activity
				mArrSelectedIds.add(0);
				setNextNumber();
			}
		});

		next_number = (TextView) v.findViewById(R.id.next_number);

		setNextNumber();

		return v;
	}
	
	
	private void setList(){
		mArrAdapter = new ArrayAdapter<String>(getActivity(), R.layout.listrow_pickpilots, R.id.text1 , mArrNames){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row;
                 
                if (null == convertView) {
                row = mInflater.inflate(R.layout.listrow_pickpilots, parent, false);
                } else {
                row = convertView;
                }

				TextView tv = (TextView) row.findViewById(R.id.text1);
				TextView nm = (TextView) row.findViewById(R.id.number);


                String name = getItem(position);
                Integer pid = mArrIds.get(position);
                if (mArrSelectedIds.contains(pid)){
                	int index = mArrSelectedIds.lastIndexOf(pid) + 1;
					nm.setText(Integer.toString(index));
					nm.setVisibility(View.VISIBLE);
                	row.setBackgroundColor(getResources().getColor(R.color.lt_grey));
                } else {
                	row.setBackgroundColor(getResources().getColor(R.color.transparent));
					nm.setVisibility(View.GONE);
                }
                
                tv.setText(name);

                return row;
            }
   	   	};
   	   	//mArrAdapter.notifyDataSetChanged();
        //getListView().invalidateViews();
        setListAdapter(mArrAdapter);
	}
		
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// Add the race id to this bundle
        Integer pid = mArrIds.get(position);
        
        if (mArrSelectedIds.contains(pid)){
            // Already selected   
        } else {
            mArrSelectedIds.add(pid);
        }
        getListView().invalidateViews();

		setNextNumber();
	}

    public boolean onBackPressed(){
        if (mArrSelectedIds.size() == 0){
            NewRaceActivity a = (NewRaceActivity)getActivity();
            a.pilots = mArrSelectedIds;
            a.getFragment(new NewRaceFrag1(), "newracefrag1");
            return true;
        }
        
        mArrSelectedIds.remove(mArrSelectedIds.size()-1);
        getListView().invalidateViews();
        setNextNumber();

        return true;
    }

	private void setNextNumber(){
		int index = mArrSelectedIds.size()+1;
		next_number.setText(Integer.toString(index));
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
	  super.onSaveInstanceState(savedInstanceState);
	}
	
	
	@Override
	public void onResume(){
		super.onResume();
		setList();
	}
			
	private void getNamesArray(){
		PilotData datasource = new PilotData(getActivity());
		datasource.open();
		ArrayList<Pilot> allPilots = datasource.getAllPilots();
		datasource.close();
		
		mArrNames = new ArrayList<>();
		mArrIds = new ArrayList<>();
		
		for (Pilot p : allPilots){
			mArrNames.add(String.format("%s %s", p.firstname, p.lastname));
			mArrIds.add(p.id);
			
		}
	}
 }
