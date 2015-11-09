/*
 * NewRaceFrag2
 * Page 2 for New Race form
 * Pilot Picker
 * with Random/Manual Order Shuffling
 */
package com.marktreble.f3ftimer.dialog;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.pilot.PilotData;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

import android.app.Activity;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

public class NewRaceFrag3 extends ListFragment {

	private ArrayAdapter<String> mArrAdapter;
	private LayoutInflater mInflater;
	
	private ArrayList<Integer> mArrSelectedIds;	// ArrayList of selected pilot ids is same order as mArrSelected
	private boolean manualScramble = false;
	
	public ArrayList<String> mArrNames; // ArrayList of all pilots in database
	private ArrayList<Integer> mArrIds;	 // ArrayList of database ids (order matching mArrNames - alphabetical order)
	public ArrayList<Integer> mArrNumbers;	 // ArrayList of database ids (order matching mArrNames - alphabetical order)

	String[] _names;  	// String array of all pilots in database    	
	Integer[] _ids;  	// String array of all pilots in database    	
	boolean[] _selections;	// bool array of which has been selected

	private AlertDialog mDlg;
	
	public NewRaceFrag3(){
		
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (getActivity().getClass().getName().equals(getActivity().getClass().getPackage().getName() + ".NewRaceActivity")) {
            NewRaceActivity a = (NewRaceActivity) getActivity();
            mArrSelectedIds = a.pilots;
        } else {
            FlyingOrderEditActivity a = (FlyingOrderEditActivity) getActivity();
            mArrSelectedIds = a.pilots;
        }

        if (savedInstanceState != null){

        	_names = savedInstanceState.getStringArray("names");
        	ArrayList<Integer> ids = savedInstanceState.getIntegerArrayList("ids");
        	_ids = ids.toArray(new Integer[ids.size()]);
        	_selections = savedInstanceState.getBooleanArray("selections");
        	
        } else {
    	    // Initialise the pilot picker data

            getUnselectedArray();
        }

        getSelectedArray();


    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		mInflater = inflater;
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.race_new_frag3, container, false);
        
        // Listener for next button
		Button next = (Button) v.findViewById(R.id.button_next);
	    next.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	// Save to database and quit the activity

                if (getActivity().getClass().getName().equals(getActivity().getClass().getPackage().getName() + ".NewRaceActivity")) {
                    NewRaceActivity a = (NewRaceActivity) getActivity();
                    a.pilots = mArrSelectedIds;
                    a.saveNewRace();
                    a.setResult(Activity.RESULT_OK, null);
                    a.finish();
                } else {
                    FlyingOrderEditActivity a = (FlyingOrderEditActivity) getActivity();
                    a.pilots = mArrSelectedIds;
                    a.updateFlyingOrder();
                    a.setResult(Activity.RESULT_OK, null);
                    a.finish();
                }
	        }
	    });


	    // Listener for add button
	    Button add = (Button) v.findViewById(R.id.button_add);
	    add.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	showPilotsDialog();
	        }
	    });
	    
	    // Listener for scramble button
	    Button scramble = (Button) v.findViewById(R.id.button_scramble);
	    scramble.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	if (mArrSelectedIds.size()>1){
	        		scrambleSelectedArray();
	        		scrambleSelectedArray();
	        		scrambleSelectedArray();
	        		getSelectedArray();
	        		mArrAdapter.notifyDataSetChanged();
	        	}
	        }
	    });
	    
	    // Listener for manual re-order button
	    Button manual = (Button) v.findViewById(R.id.button_manual);
	    manual.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	// Show up/down buttons to pilot list view
	        	if (mArrSelectedIds.size()>1){
	        		
	        		manualScramble = !manualScramble;	
	        		mArrAdapter.notifyDataSetChanged();
	        	}
	        }
	    });
	    
	    setList();
		return v;
	}
	
	
	private void setList(){
    	   	mArrAdapter = new ArrayAdapter<String>(getActivity(), R.layout.listrow_reorder, R.id.text1 , mArrNames){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row;
                 
                if (null == convertView) {
                row = mInflater.inflate(R.layout.listrow_reorder, parent, false);
                } else {
                row = convertView;
                }
                
                TextView tv = (TextView) row.findViewById(R.id.text1);
                tv.setText(mArrNames.get(position));

				TextView bib_no = (TextView) row.findViewById(R.id.number);
				bib_no.setText(String.format("%d", mArrNumbers.get(position)));

            	Button btnup = (Button)row.findViewById(R.id.button_up);
                if (position==0 || !manualScramble){
                	btnup.setVisibility(View.INVISIBLE);
                } else {
                	btnup.setVisibility(View.VISIBLE);
                }
                
            	Button btndn = (Button)row.findViewById(R.id.button_down);
                if (position==mArrSelectedIds.size()-1 || !manualScramble){
                	btndn.setVisibility(View.INVISIBLE);
                } else {
                	btndn.setVisibility(View.VISIBLE);
                	
                }
                
                return row;
            }
   	   	};
         setListAdapter(mArrAdapter);
	}
	
	public void moveUp(View v){
		LinearLayout vwParentRow = (LinearLayout)v.getParent();
		ListView list= getListView();
        int position = list.getPositionForView(vwParentRow);
        
		int tmpint = mArrSelectedIds.get(position);
		mArrSelectedIds.set(position,  mArrSelectedIds.get(position-1));
		mArrSelectedIds.set(position-1,  tmpint);

		getSelectedArray();
		mArrAdapter.notifyDataSetChanged();
	}
	
	public void moveDown(View v){
		LinearLayout vwParentRow = (LinearLayout)v.getParent();
		ListView list= getListView();
        int position = list.getPositionForView(vwParentRow);

		int tmpint = mArrSelectedIds.get(position);
		mArrSelectedIds.set(position,  mArrSelectedIds.get(position+1));
		mArrSelectedIds.set(position+1,  tmpint);

		getSelectedArray();
		mArrAdapter.notifyDataSetChanged();
	}

    public boolean onBackPressed() {
        NewRaceActivity a = (NewRaceActivity) getActivity();
        a.pilots = mArrSelectedIds;
        a.getFragment(new NewRaceFrag2(), "newracefrag2");
        return true;
    }
        
        @Override
	public void onPause(){
		super.onPause();
		if (mDlg != null){
			mDlg.dismiss();
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
	  super.onSaveInstanceState(savedInstanceState);
	  savedInstanceState.putStringArray("names", _names);
	  ArrayList<Integer> ids = new ArrayList<>();
      for (Integer id : _ids) ids.add(id);
	  savedInstanceState.putIntegerArrayList("ids", ids);
	  savedInstanceState.putBooleanArray("selections", _selections);
	}
	
	
	@Override
	public void onResume(){
		super.onResume();
		if (mDlg != null){
			showPilotsDialog();
		}		
	}
		
	private void showPilotsDialog(){
		getUnselectedArray();
	    
        mDlg = new AlertDialog.Builder( getActivity() )
    	.setTitle( "Select Pilots" )
    	.setMultiChoiceItems( _names, _selections, new DialogSelectionClickHandler() )
    	.setPositiveButton( "OK", new DialogButtonClickHandler() )
    	.show();
	}
	
	private void getUnselectedArray(){
		PilotData datasource = new PilotData(getActivity());
		datasource.open();
		ArrayList<Pilot> allPilots = datasource.getAllPilots();
		datasource.close();
		
		ArrayList<String> arrUnselectedNames = new ArrayList<>();
		ArrayList<Integer> arrUnselectedIds = new ArrayList<>();
		
		
		
		for (Pilot p : allPilots){
			if (!mArrSelectedIds.contains(p.id)){
				arrUnselectedNames.add(String.format("%s %s", p.firstname, p.lastname));
				arrUnselectedIds.add(p.id);
			}
			
		}
		
	    _names = new String[arrUnselectedNames.size()];
	    _names = arrUnselectedNames.toArray(_names);
	    _ids = new Integer[arrUnselectedIds.size()];
	    _ids = arrUnselectedIds.toArray(_ids);
	    
	    _selections = new boolean[ _names.length ];

	}
	
	private void getSelectedArray(){
		PilotData datasource = new PilotData(getActivity());
		datasource.open();
		ArrayList<Pilot> allPilots = datasource.getAllPilots();
		datasource.close();
		
		if (mArrNames == null) {
			mArrNames = new ArrayList<>();
			mArrIds = new ArrayList<>();
			mArrNumbers = new ArrayList<>();
		}

		while(mArrNames.size() < mArrSelectedIds.size()) mArrNames.add("");
		while(mArrIds.size() < mArrSelectedIds.size()) mArrIds.add(0);
		while(mArrNumbers.size() < mArrSelectedIds.size()) mArrNumbers.add(0);


		for (Pilot p : allPilots){
			if (mArrSelectedIds.contains(p.id)){
				int index = mArrSelectedIds.lastIndexOf(p.id);
				mArrNames.set(index, String.format("%s %s", p.firstname, p.lastname));
				mArrIds.set(index, p.id);
				mArrNumbers.set(index, index+1);
			}
		}
		// Blank out skipped pilot numbers
		for (int index =0; index<mArrSelectedIds.size(); index++){
			if (mArrSelectedIds.get(index) == 0){
				mArrNames.set(index, "");
				mArrIds.set(index, 0);
				mArrNumbers.set(index, index+1);
			}
		}
	}
	
	private void scrambleSelectedArray(){
		Date date = new Date();
		long longDate=date.getTime();
		Random rnd = new Random(longDate);

		int sz = mArrSelectedIds.size();
		int rnd1;
		int rnd2;
		Integer tmpint;
		
		for (int i=0; i<=100; i++){
			// Generate 2 random numbers, and swap the values in the two indices
			if (sz>2){
				rnd1 = rnd.nextInt(1000)%sz;
				
				do {
					rnd2 = rnd.nextInt(1000)%sz;
				} while (rnd2 == rnd1);
			} else {
				rnd1=0;
				rnd2=1;
			}
						
			tmpint = mArrSelectedIds.get(rnd1);
			mArrSelectedIds.set(rnd1,  mArrSelectedIds.get(rnd2));
			mArrSelectedIds.set(rnd2,  tmpint);
		}
	}
	
	public class DialogSelectionClickHandler implements DialogInterface.OnMultiChoiceClickListener
	{
		public void onClick( DialogInterface dialog, int clicked, boolean selected )
		{
			_selections[clicked] = selected;
		}
	}
	

	public class DialogButtonClickHandler implements DialogInterface.OnClickListener
	{
		public void onClick( DialogInterface dialog, int clicked )
		{
			switch( clicked )
			{
				case DialogInterface.BUTTON_POSITIVE:
					// Dismiss picker, so update the listview!
					for (int i=0;i<_selections.length; i++){
						if (_selections[i] == true){
							mArrNames.add(_names[i]);
							mArrIds.add(_ids[i]);
							mArrSelectedIds.add(_ids[i]);
							
						}
						
					}
					getUnselectedArray();
						    
					getSelectedArray();
		   	   		mArrAdapter.notifyDataSetChanged();
		   	   		mDlg = null;
					break;
			}
		}
	}
 }
