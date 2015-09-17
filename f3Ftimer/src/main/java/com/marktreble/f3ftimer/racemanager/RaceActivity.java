/*
 * RaceActivity
 * Shows List of contestants, along with status of race
 * Also allows new pilots to be added (always appended to end of list)
 */
package com.marktreble.f3ftimer.racemanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.marktreble.f3ftimer.*;
import com.marktreble.f3ftimer.data.pilot.*;
import com.marktreble.f3ftimer.data.race.*;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;
import com.marktreble.f3ftimer.dialog.*;
import com.marktreble.f3ftimer.driver.*;
import com.marktreble.f3ftimer.filesystem.SpreadsheetExport;
import com.marktreble.f3ftimer.pilotmanager.PilotsActivity;
import com.marktreble.f3ftimer.resultsmanager.ResultsActivity;
import com.marktreble.f3ftimer.wifi.Wifi;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class RaceActivity extends ListActivity {
	
	public static boolean DEBUG = true;
	public static int RESULT_ABORTED = 2;
	public static int ROUND_SCRUBBED = 3;
	
	// Dialogs
	static int DLG_SETTINGS = 0;
	static int DLG_TIMER = 1;
	static int DLG_NEXT_ROUND = 2;
	static int DLG_TIMEOUT = 3;
	static int DLG_TIME_SET = 4;
    static int DLG_PILOT_EDIT = 5;
    static int DLG_FLYING_ORDER_EDIT = 6;
    static int DLG_GROUP_SCORE_EDIT = 7;

	private ArrayAdapter<String> mArrAdapter;
    private ArrayList<String> mArrNames;
    private ArrayList<String> mArrNumbers;
    private ArrayList<Pilot> mArrPilots;
    private ArrayList<Integer> mArrRounds;
    private ArrayList<Integer> mArrGroups;
    private ArrayList<Boolean> mFirstInGroup;

	private Integer mRid;
	private Integer mRnd;
	private Race mRace;
	private String mInputSource;
	private boolean mPrefResults;
    private boolean mPrefResultsDisplay;
    private String mPrefExternalDisplay;

    private boolean mRoundComplete;
	private boolean mRoundNotStarted;
	private Pilot mNextPilot;

	private boolean mPilotDialogShown = false; // Used to determine the action when the start button is pressed
	
	private Context mContext;
	
	public AlertDialog mDlg;
	String[] _options;  	// String array of all pilots in database    	
	boolean[] _selections;	// bool array of which has been selected
	
	boolean mWifiSavedState = false;

    private TextView mPower;
    private ImageView mStatus;
    private boolean mConnectionStatus;

    private ListView mListView;
    private static Parcelable mListViewScrollPos = null;

    private int mGroupScoring = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        Log.i("RACEACTIVITY", "ONCREATE");
		super.onCreate(savedInstanceState);
		
		ImageView view = (ImageView)findViewById(android.R.id.home);
		Resources r = getResources();
		int px = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, r.getDisplayMetrics());
		view.setPadding(0, 0, px, 0);
		
		mContext = this;
		
		setContentView(R.layout.race);
			
		Intent intent = getIntent();
		if (intent.hasExtra("race_id")){
			Bundle extras = intent.getExtras();
			mRid = extras.getInt("race_id");
		}
		
		RaceData datasource = new RaceData(this);
  		datasource.open();
  		Race race = datasource.getRace(mRid);
  		datasource.close();
  		mRace = race;
  		
  		setRound();


        mListView = getListView();
        registerForContextMenu(mListView);
        
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // Get values from preferences
        getPreferences();
		
        if (savedInstanceState == null){
	        // Start Results server

	       	startServers();
			
        	
        }
        getNamesArray();
        setList();
	}
	
    @Override
    public void onDestroy(){
        Log.i("DRIVER (Race Activity)", "Destroyed");
        super.onDestroy();
        if (isFinishing())
            stopServers();
    }

	
	public void onBackPressed (){
		// destroy the servers when back is pressed
		stopServers();
		super.onBackPressed();
	}
	
	public void startServers(){
    	// Start Results server
		if (mPrefResults){
			Log.i("START SERVICE","RESULTS");
			if (Wifi.canEnableWifiHotspot(this)){
    			mWifiSavedState = Wifi.enableWifiHotspot(this);
                Log.i("WIFI", "Enabled - saved state " + ((mWifiSavedState) ? "On" : "Off"));
			}
            RaceResultsService.stop(this);
            
			Intent serviceIntent = new Intent(this, RaceResultsService.class);			    	
	        serviceIntent.putExtra("com.marktreble.f3ftimer.race_id", mRid);
	    	startService(serviceIntent);
		}

        // Start Results Display Server
        if (mPrefResultsDisplay && !mPrefExternalDisplay.equals("")){
            Log.i("START SERVICE", "RESULTS Display");
            RaceResultsDisplayService.stop(this);

            RaceResultsDisplayService.startRDService(this,  mPrefExternalDisplay);
        }

        // Stop Any Timer Drivers
        USBJEService.stop(this);
        USBArduinoService.stop(this);
        SoftBuzzerService.stop(this);
        
        Intent serviceIntent = null;
        
        // Start Timer Driver
        Bundle extras = getIntent().getExtras();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Map<String,?> keys = sharedPref.getAll();

        for(Map.Entry<String,?> entry : keys.entrySet()){
            extras.putString(entry.getKey(), entry.getValue().toString());
        }

        USBJEService.startDriver(this, mInputSource, mRid, extras);
        USBArduinoService.startDriver(this, mInputSource, mRid, extras);
        SoftBuzzerService.startDriver(this, mInputSource, mRid, extras);
		
	}
	
	public void stopServers(){
		// Stop all Servers
		Log.i("STOP SERVICE","RESULTS");

        if (RaceResultsService.stop(this)){
    		if (Wifi.canEnableWifiHotspot(this)){
				Wifi.disableWifiHotspot(this, mWifiSavedState);
			}
       	}

        RaceResultsDisplayService.stop(this);

        USBJEService.stop(this);
        USBArduinoService.stop(this);
        SoftBuzzerService.stop(this);

    }
	
	@Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("round", mRnd);
        outState.putInt("raceid", mRid);
        outState.putSerializable("mArrRounds", mArrRounds);
        outState.putBoolean("connection_status", mConnectionStatus);
        mListViewScrollPos = mListView.onSaveInstanceState();
        outState.putParcelable("listviewscrollpos", mListViewScrollPos);
        Log.i("RACEACTIVITY", "SAVEINSTANCE");

    }

    @SuppressWarnings("unchecked")
	@Override
	public void onRestoreInstanceState(@NonNull Bundle savedInstanceState){
        mRnd=savedInstanceState.getInt("round");
        mRid=savedInstanceState.getInt("raceid");
        mArrRounds = (ArrayList<Integer>)savedInstanceState.getSerializable("mArrRounds");
        mConnectionStatus=savedInstanceState.getBoolean("connection_status");
        mListViewScrollPos = savedInstanceState.getParcelable("listviewscrollpos");
        if (mListView != null)
            mListView.onRestoreInstanceState(mListViewScrollPos);
        Log.i("RACEACTIVITY", "RESTOREINSTANCE");

    }
	
	public void onResume(){
        Log.i("RACEACTIVITY", "RESUME");

        super.onResume();
		
     	registerReceiver(onBroadcast, new IntentFilter("com.marktreble.f3ftimer.onUpdate"));	
     	
		mPower = (TextView)findViewById(R.id.battery_level);
        registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        mStatus = (ImageView)findViewById(R.id.connection_status);
        if (mConnectionStatus) {
            mStatus.setImageDrawable(getResources().getDrawable(R.drawable.on));
        } else{
            mStatus.setImageDrawable(getResources().getDrawable(R.drawable.off));
        }
        //
        sendCommand("get_connection_status");

        String sInputSource = mInputSource;
     	boolean sPrefResults = mPrefResults;
     	getPreferences();
     	
     	if (!sInputSource.equals(mInputSource) 	// Input src changed
     		|| sPrefResults!=mPrefResults 		// Results server toggled
     		){
     		stopServers();
     		startServers();
     	}
     	
     	
   	}
	
	public void onPause(){
        Log.i("RACEACTIVITY", "PAUSE");

        super.onPause();
		unregisterReceiver(onBroadcast);
		unregisterReceiver(mBatInfoReceiver);
	}
	
	private void getPreferences(){
     	SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		mInputSource = sharedPref.getString("pref_input_src", getString(R.string.Demo));
        mPrefResults = sharedPref.getBoolean("pref_results_server", false);
        mPrefResultsDisplay = sharedPref.getBoolean("pref_results_display", false);
        mPrefExternalDisplay = sharedPref.getString("pref_external_display", "");
	}
	
	private void setRound(){
  		mRnd = mRace.round;

        RaceData datasource = new RaceData(RaceActivity.this);
        datasource.open();
        mGroupScoring = datasource.getGroups(mRid, mRnd);
        datasource.close();

  		TextView tt = (TextView) findViewById(R.id.race_title);
  		tt.setText("Round "+Integer.toString(mRnd) + " - "+mRace.name);


	}
	/*
	 * Start a pilot on his run
	 */
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// Pilot has been clicked, so start the Race Timer Activity

        Pilot p = mArrPilots.get(position);
        int round = mArrRounds.get(position);
        if (p.time==0 && !p.flown && p.status!=Pilot.STATUS_RETIRED){
        	 mPilotDialogShown = showPilotDialog(round, p.id);
        }
	}
	
	/*
	 * Return from dialogs
	 */
	 
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("RACEACTIVITY", "ACTIVITYRESULT");

        super.onActivityResult(requestCode, resultCode, data);
		mPilotDialogShown = false;
        getNamesArray();
        mArrAdapter.notifyDataSetChanged();

		Log.i("RESULT CODE", Integer.toString(resultCode));
		Log.i("REQUEST CODE", Integer.toString(requestCode));

        // Clear the shown flag regardless of success
        if (requestCode == RaceActivity.DLG_TIMER)
            mPilotDialogShown = false;

		if(resultCode==RaceActivity.RESULT_OK){
			if (requestCode == RaceActivity.DLG_NEXT_ROUND){
				// Positive response from next round dialog
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        nextRound();
                    }
                }, 600);
			}
			if (requestCode == RaceActivity.DLG_TIMER){
				// Response from completed run

				if (mNextPilot != null){
					// Bring up next pilot's dialog
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mListView.smoothScrollToPositionFromTop(mNextPilot.position - 1, 0, 500);
                        }
                    }, 100);

                    if (!mFirstInGroup.get(mNextPilot.position)) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                showNextPilot();
                                sendCommand("begin_timeout");
                            }
                        }, 600);
                    }
				} else {
					// Bring up the next round dialog
					showNextRound();
				}
			}
			if (requestCode == RaceActivity.DLG_TIME_SET){
				// Response from setting the time manual
				String time = data.getStringExtra("time");
                int pilot_id = data.getIntExtra("pilot", 0);
                int round = data.getIntExtra("round", 0);

                if (!time.equals("")) {
                    float new_time = Float.parseFloat(time);
                    RacePilotData datasource = new RacePilotData(RaceActivity.this);
                    datasource.open();
                    datasource.setPilotTimeInRound(mRid, pilot_id, round, new_time);
                    datasource.close();

                    // Update the spread file
                    new SpreadsheetExport().writeFile(mContext, mRace);
                }

			}
			if (requestCode == RaceActivity.DLG_TIMEOUT){
				// Send command to Service to say that timeout has been resumed
				sendCommand("timeout_resumed");
				
				// Resume timeout - start next pilot's dialog
				if (mNextPilot != null){
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mListView.smoothScrollToPositionFromTop(mNextPilot.position - 1, 0, 500);
                        }
                    }, 100);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showNextPilot();
                        }
                    }, 600);
				}
			}
			
			if (requestCode == RaceActivity.DLG_PILOT_EDIT){
				// Response from editing the pilot - refresh the pilot list
			}

            if (requestCode== RaceActivity.DLG_FLYING_ORDER_EDIT){
                // Response from editing the flying order
            }

            if (requestCode== RaceActivity.DLG_GROUP_SCORE_EDIT){
                String num_groups = data.getStringExtra("num_groups");
                
                int num;
                try {
                    num = Integer.parseInt(num_groups);
                } catch (NumberFormatException e){
                    num = 0;
                }
                
                Log.i("GROUP SCORING", "CHANGED NUMBER OF GROUPS TO "+num_groups);
                RaceData datasource = new RaceData(RaceActivity.this);
                datasource.open();
                datasource.setGroups(mRid, mRnd, num);
                datasource.close();
                setRound();
            }

            getNamesArray();
            mArrAdapter.notifyDataSetChanged();
		}
		
		if(resultCode==RaceActivity.ROUND_SCRUBBED){
			if (requestCode == RaceActivity.DLG_TIMEOUT){
				sendCommand("cancel_timeout");

                if (mGroupScoring == 1) {
                    // Not group scored
                    // Delete all times from round
                    RacePilotData datasource = new RacePilotData(RaceActivity.this);
                    datasource.open();
                    datasource.deleteRound(mRid, mRnd);
                    datasource.close();

                    // Delete the round
                    RaceData datasource2 = new RaceData(RaceActivity.this);
                    datasource2.open();
                    datasource2.deleteRound(mRid, mRnd);
                    datasource2.close();
                } else {
                    // Just delete the times for the current group
                    RacePilotData datasource = new RacePilotData(RaceActivity.this);
                    datasource.open();
                    datasource.deleteGroup(mRid, mRnd, mNextPilot.position, mArrGroups, mArrPilots);
                    datasource.close();
                }


                getNamesArray();
                mArrAdapter.notifyDataSetChanged();
			}
		}
        invalidateOptionsMenu(); // Refresh menu so that any changes in state are shown
    }
	
	/*
	 * Create the context menu, and handle the result
	 */
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v.getId()==android.R.id.list) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
		    menu.setHeaderTitle(mArrNames.get(info.position));
		    
		    Pilot p = mArrPilots.get(info.position);
		    		    
		    if ((p.status & Pilot.STATUS_NORMAL) == Pilot.STATUS_NORMAL && p.flown)
		    	menu.add(Menu.NONE, 0, 0, "Award Reflight");

		    if ((p.status & Pilot.STATUS_NORMAL) == Pilot.STATUS_NORMAL && p.flown){
	    		menu.add(Menu.NONE, 1, 1, "Add Penalty");
	    		menu.add(Menu.NONE, 2, 2, "Remove Penalty");
		    }
		    if ((p.status & Pilot.STATUS_RETIRED) == 0 && !p.flown)
		    	menu.add(Menu.NONE, 3, 3, "Skip Round (Award 0 points)");

		    if ((p.status & Pilot.STATUS_RETIRED) == 0)		    
		    	menu.add(Menu.NONE, 4, 4, "Retire From Race");

		    if ((p.status & Pilot.STATUS_RETIRED) >0)		    
		    	menu.add(Menu.NONE, 5, 5, "Reinstate");
		    
		    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
			boolean allowManualEntry = sharedPref.getBoolean("pref_manual_entry", true);
		    if (allowManualEntry)
		    	menu.add(Menu.NONE, 6, 6, "Enter Time Manually");
		    
	    	menu.add(Menu.NONE, 7, 7, "Edit Pilot details");

		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
        Log.i("RACEACTIVITY", "CONTEXT ITEM SELECTED");

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		int menuItemIndex = item.getItemId();

		Pilot p = mArrPilots.get(info.position);
        Integer round = mArrRounds.get(info.position);
        
		RacePilotData datasource = new RacePilotData(RaceActivity.this);
  		datasource.open();

		if (menuItemIndex == 0){
			// Award Reflight
	  		datasource.setReflightInRound(mRid, p.id, round);
		}

		if (menuItemIndex == 1){
			// Penalty
            datasource.incPenalty(mRid, p.id, round);
		}

		if (menuItemIndex == 2){
			// Penalty (remove)
	  		datasource.decPenalty(mRid, p.id, round);
		}

		if (menuItemIndex == 3){
			// Zero
			scorePilotZero(p);
		}

		if (menuItemIndex == 4){
			// Retired
	  		datasource.setRetired(true, mRid, p.id);
		}

		if (menuItemIndex == 5){
			// Retired
	  		datasource.setRetired(false, mRid, p.id);
		}

		if (menuItemIndex == 6){
			// Manual time entry dialog
	  		enterManualTimeForPilot(p, round);
		}

		if (menuItemIndex == 7){
			// Pilot edit dialog
	  		editPilot(p);
		}

		datasource.close();

        getNamesArray();
        mArrAdapter.notifyDataSetChanged();
	   	return true;
	}
	
	public void scorePilotZero(Pilot p){
		RacePilotData datasource = new RacePilotData(RaceActivity.this);
  		datasource.open();
		datasource.setPilotTimeInRound(mRid, p.id, mRnd, 0);
		datasource.close();

        getNamesArray();
        mArrAdapter.notifyDataSetChanged();
        invalidateOptionsMenu();
	}
	
	/*
	 * Get Pilots from database to populate the listview
	 */
	
	private void getNamesArray(){
        Log.i("RACEACTIVITY", "GET NAMESARRAY");

		RacePilotData datasource = new RacePilotData(this);
		datasource.open();
		ArrayList<ArrayList<Pilot>> allPilots = new ArrayList<>();
        for (int r=mRnd;r<mRnd+mRace.rounds_per_flight; r++)
           allPilots.add(datasource.getAllPilotsForRace(mRid, r, mRace.offset));


        if (mArrNames == null) {
            Log.i("RACEACTIVITY", "INITIALISE NAMESARRAY");

            mArrNames = new ArrayList<>();
            mArrNumbers = new ArrayList<>();
            mArrPilots = new ArrayList<>();
            mArrRounds = new ArrayList<>();
            mArrGroups = new ArrayList<>();
            mFirstInGroup = new ArrayList<>();
        }

        int sz = allPilots.get(0).size() * mRace.rounds_per_flight;
        
        while(mArrNames.size() < sz) mArrNames.add("");
        while(mArrNumbers.size() < sz) mArrNumbers.add("");
        while(mArrPilots.size() < sz) mArrPilots.add(new Pilot());
        while(mArrRounds.size() < sz) mArrRounds.add(0);
        while(mArrGroups.size() < sz) mArrGroups.add(0);
        while(mFirstInGroup.size() < sz) mFirstInGroup.add(false);

		mRoundComplete = true;
		mRoundNotStarted = true;
		mNextPilot = null;
		
		
		// Find ftr
        for (int r=0;r<mRace.rounds_per_flight; r++) {
            int c = 0; // Flying order number

            int g = 0; // Current group we are calculating

            float[] ftg = new float[mGroupScoring+1]; // Fastest time in group (used for calculating normalised scores)
            for (int i=0; i<mGroupScoring+1; i++)
                ftg[i]= 9999;

            boolean first = true;
            int group_size = (int)Math.floor(sz/mGroupScoring);
            int remainder = sz - (mGroupScoring * group_size);

            for (Pilot p : allPilots.get(r)) {
                if (g<remainder){
                    if (c>= (group_size+1)*(g+1)) {
                        g++;
                        first = true;
                    }
                } else {
                    if (c>= ((group_size+1)*remainder) + (group_size*((g+1)-remainder))) {
                        g++;
                        first = true;
                    }
                }

                Log.d("GROUPSDEBUG", "SZ:" + Integer.toString(group_size) + " G:" + Integer.toString(g) + " REM:" + Integer.toString(remainder));
                int position = (c * mRace.rounds_per_flight) + r;

                Log.d("GET NAMES ARRAY", String.format("%s %s", p.firstname, p.lastname));
                mArrNames.set(position, String.format("%s %s", p.firstname, p.lastname));
                mArrNumbers.set(position, String.format("%d.", c + 1));
                mArrPilots.set(position, p);
                mArrRounds.set(position, mRnd + r);
                mArrGroups.set(position, g);
                mFirstInGroup.set(position, first);
                c++;
                first = false;


                if (p.time == 0 && (((p.status & Pilot.STATUS_RETIRED) == 0) && (!p.flown))) {
                    // Unset round complete flag
                    // Somebody who isn't retired hasn't flown
                    mRoundComplete = false;
                } else {
                    // Unset round not started flag
                    // Somebody has flown
                    mRoundNotStarted = false;
                }

                // Get the next pilot in the running order
                if (p.time == 0 && !p.flown &&
                        (((p.status & Pilot.STATUS_NORMAL) == Pilot.STATUS_NORMAL) || ((p.status & Pilot.STATUS_REFLIGHT) == Pilot.STATUS_REFLIGHT))
                        && (mNextPilot == null || mNextPilot.position>position)) {
                    mNextPilot = p;
                    mNextPilot.position = position;
                    // Log the round number against the next pilot, so that the data is avaailable to the external "start_pressed" message
                    mNextPilot.round = mRnd+r;
                }

                ftg[g] = (p.time > 0) ? Math.min(ftg[g], p.time) : ftg[g];
            }

            // Set points for each pilot
            for (int i=0; i<mArrPilots.size(); i+=mRace.rounds_per_flight){
                Pilot p = mArrPilots.get(i+r);
                if (p.time > 0)
                    p.points = (int) ((ftg[mArrGroups.get(i+r)] / p.time) * 1000);

                if (p.time == 0 && p.flown) // Avoid division by 0
                    p.points = 0;

                p.points -= p.penalty * 100;

                if (p.time == 0 && p.status == Pilot.STATUS_RETIRED) // Avoid division by 0
                    p.points = 0;
            }
        }
        
		datasource.close();
	}
	
	private void setList(){
        Log.i("RACEACTIVITY", "SET LIST");

	    mArrAdapter = new ArrayAdapter<String>(this, R.layout.listrow_racepilots , R.id.text1, mArrNames){
   	   		@Override
   	   		public View getView(int position, View convertView, ViewGroup parent) {
                View row;
                
                if (mArrNames.get(position) == null) return null;
                
                if (null == convertView) {
                row = getLayoutInflater().inflate(R.layout.listrow_racepilots, parent, false);
                } else {
                row = convertView;
                }
                
                Pilot p = mArrPilots.get(position);

                Log.i("STATUS", Integer.toString(p.status)+":"+Boolean.toString(p.flown)+":" + mArrNames.get(position)+":"+Integer.toString(p.pilot_id));
                
                TextView p_number = (TextView) row.findViewById(R.id.number);
                p_number.setText(mArrNumbers.get(position));

                TextView p_name = (TextView) row.findViewById(R.id.text1);
                p_name.setText(mArrNames.get(position));
                p_name.setPaintFlags(p_name.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                p_name.setTextColor(getResources().getColor(R.color.text3));

                View group_header = row.findViewById(R.id.group_header);
                TextView group_header_label = (TextView) row.findViewById(R.id.group_header_label);
                if (mGroupScoring>1 && mFirstInGroup.get(position)){
                    group_header.setVisibility(View.VISIBLE);
                    group_header_label.setText("Group "+(mArrGroups.get(position)+1));
                } else {
                    group_header.setVisibility(View.GONE);
                }

                Drawable flag = p.getFlag(mContext);
        		if (flag != null){
        		    p_name.setCompoundDrawablesWithIntrinsicBounds(flag, null, null, null);
        		    int padding = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
        		    p_name.setCompoundDrawablePadding(padding);
        		}
        		
           		row.setBackgroundColor(getResources().getColor(R.color.background));
           		
                if (p.status==Pilot.STATUS_RETIRED){
                	p_name.setPaintFlags(p_name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                	p_name.setTextColor(getResources().getColor(R.color.red));
                }

            	if (p.status == Pilot.STATUS_REFLIGHT){
            		p_name.setTextColor(getResources().getColor(R.color.green));
            		row.setBackgroundColor(getResources().getColor(R.color.lt_grey));
   	   			}

                TextView penalty = (TextView) row.findViewById(R.id.penalty);
                if (p.penalty >0){
                	penalty.setText(getResources().getString(R.string.penalty) + p.penalty);
                } else {
                	penalty.setText(getResources().getString(R.string.empty));                	
                }
                
                TextView time = (TextView) row.findViewById(R.id.time);
                if (p.time==0 && !p.flown){
                	time.setText(getResources().getString(R.string.notime));
                	if (mNextPilot!=null && mNextPilot.pilot_id == p.pilot_id && mNextPilot.position == position)
                   		row.setBackgroundColor(getResources().getColor(R.color.lt_grey));
                		
                } else {
                	time.setText(String.format("%.2f", p.time));
               		row.setBackgroundColor(getResources().getColor(R.color.dk_grey));
                }

                TextView points = (TextView) row.findViewById(R.id.points);
                if (p.flown || p.status==Pilot.STATUS_RETIRED){
            		points.setText(Float.toString(p.points));
                } else {
            		points.setText("");
                }

                return row;
            }
   	   	};
   	   	setListAdapter(mArrAdapter);
	}
	
	/*
	 * Handle + button press
	 * Get all Pilots that are not in the race
	 * Display a picker
	 * Add selected pilots into the database and dismiss
	 */
	private void getRemainingNamesArray(){
		PilotData datasource = new PilotData(this);
		datasource.open();
		ArrayList<Pilot> allPilots = datasource.getAllPilotsExcept(mArrPilots);
		datasource.close();
		
		mArrNames = new ArrayList<>();
		mArrPilots = new ArrayList<>();
		
		for (Pilot p : allPilots){
			mArrNames.add(String.format("%s %s", p.firstname, p.lastname));
			mArrPilots.add(p);
			
		}
	}
	
	private void showPilotsDialog(){
		getRemainingNamesArray();
	    _options = new String[mArrNames.size()];
	    _options = mArrNames.toArray(_options);	  
	    _selections = new boolean[ _options.length ];

        mDlg = new AlertDialog.Builder( this )
    	.setTitle( "Select Pilots to Add" )
    	.setMultiChoiceItems( _options, _selections, new DialogSelectionClickHandler() )
    	.setPositiveButton( "OK", new DialogButtonClickHandler() )
    	.show();
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
					// Update the list
					for (int i=0; i<_options.length; i++){
						if (_selections[i]){
							addPilot(mArrPilots.get(i));
						}
						
					}
					// Dismiss picker, so update the listview!
                    updateListView();

		   	   		mDlg = null;
					break;
			}
		}
	}

    Runnable updateListView = new Runnable(){
        public void run(){
            getNamesArray();
            mArrAdapter.notifyDataSetChanged();
        }
    };

    public void updateListView(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(updateListView);
            }
        }, 1000);

    }

    private boolean showPilotDialog(int round, int pilot_id){
        if (mPilotDialogShown) return true;
        Intent intent = new Intent(this, RaceTimerActivity.class);
        intent.putExtra("pilot_id", pilot_id);
        intent.putExtra("race_id", mRid);
        intent.putExtra("round", round);
        startActivityForResult(intent,DLG_TIMER);

        return true;
    }

	private void showNextPilot(){
        int round = mArrRounds.get(mNextPilot.position);
		mPilotDialogShown = showPilotDialog(round, mNextPilot.id);
	}
	
	private void showNextRound(){
        invalidateOptionsMenu(); // Refresh menu so that next round becomes active
		Intent intent = new Intent(this, NextRoundActivity.class);
		intent.putExtra("round_id", mRnd);
		startActivityForResult(intent, DLG_NEXT_ROUND);
	}
	
	private void enterManualTimeForPilot(Pilot p, Integer round){
		Intent intent = new Intent(this, TimeEntryActivity.class);
        intent.putExtra("pilot_id", p.id);
        intent.putExtra("round", round);
       	startActivityForResult(intent, DLG_TIME_SET);
	}
	
	private void showTimeout(long start){
		Intent intent = new Intent(mContext, RaceRoundTimeoutActivity.class);
    	intent.putExtra("start", start);
        intent.putExtra("group_scored", (mGroupScoring>1));
        startActivityForResult(intent, DLG_TIMEOUT);
	}
	
	private void showTimeoutComplete(){
		Intent intent = new Intent(mContext, RaceRoundTimeoutActivity.class);
        intent.putExtra("start", 0l);
        intent.putExtra("group_scored", (mGroupScoring>1));
       	startActivityForResult(intent, DLG_TIMEOUT);
	}
	
	public boolean isServiceRunning(String serviceClassName){
        final ActivityManager activityManager = (ActivityManager)getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        final List<android.app.ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        for (android.app.ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getClassName().equals(serviceClassName)){
                return true;
            }
        }
        return false;
	}
	
	// Binding for UI->Service Communication
	public void sendCommand(String cmd){
		Intent i = new Intent("com.marktreble.f3ftimer.onUpdateFromUI");
		i.putExtra("com.marktreble.f3ftimer.ui_callback", cmd);
		sendBroadcast(i);		
	}
	
	// Binding for Service->UI Communication
    private BroadcastReceiver onBroadcast = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.hasExtra("com.marktreble.f3ftimer.service_callback")){
				Bundle extras = intent.getExtras();
				String data = extras.getString("com.marktreble.f3ftimer.service_callback");
				if (data == null){
					return;
				}
                Log.i("RACE ACTIVITY RECEIVER", data);

				if (data.equals("start_pressed")){
					if (!mPilotDialogShown){
						if (mNextPilot != null){
							Intent intent2 = new Intent(mContext, RaceTimerActivity.class);
							intent2.putExtra("pilot_id", mNextPilot.id);
                            intent2.putExtra("race_id", mRid);
                            intent2.putExtra("round", mNextPilot.round);
							startActivityForResult(intent2, DLG_TIMER);
						}
					}
				}
				
				if (data.equals("show_timeout")){
					long start =  intent.getLongExtra("start", 0);
					if (start>0)
						showTimeout(start);
				}

				if (data.equals("show_timeout_complete")){
					showTimeoutComplete();
				}

                if (data.equals("driver_started")){
                    Log.i("RACE ACT Service->UI", "driver_started");
                    mConnectionStatus = true;
                    mStatus.setImageDrawable(getResources().getDrawable(R.drawable.on));
                }

                if (data.equals("driver_stopped")){
                    Log.i("RACE ACT Service->UI", "driver_stopped");
                    mConnectionStatus = false;
                    mStatus.setImageDrawable(getResources().getDrawable(R.drawable.off));
                }

            }
		}
        };
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.race, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
        getNamesArray(); // just to ensure that the bools below are up to date!
        
        // Round Complete (Only enable when the round is actually complete)
	    menu.getItem(0).setEnabled(mRoundComplete); 
        
        // Change Flying Order (Only enable before the round is started)
	    menu.getItem(1).setEnabled(mRoundNotStarted); 
        
        // Group scoring (disable when multiple flights are being used)
        menu.getItem(2).setEnabled(mRace.rounds_per_flight<2);
        
	    return super.onPrepareOptionsMenu(menu);

	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	    	case R.id.menu_next_round:
	    		nextRound();
	    		return true;
	    	case R.id.menu_change_flying_order:
	    		changeFlyingOrder();
	    		return true;
	    	case R.id.menu_group_score:
	    		groupScore();
	    		return true;
	        case R.id.menu_add_pilots:
	        	showPilotsDialog();
	            return true;
	        case R.id.menu_settings:
	            settings();
	            return true;
            case R.id.menu_pilot_manager:
                pilotManager();
                return true;
            case R.id.menu_results_manager:
                resultsManager();
                return true;
            case R.id.menu_help:
                help();
                return true;
            case R.id.menu_about:
                about();
                return true;
            default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	private void nextRound(){
		RaceData datasource = new RaceData(this);
		datasource.open();
		mRace = datasource.nextRound(mRid);
		mRnd = mRace.round;
		datasource.close();

		setRound();
        getNamesArray();
        mArrAdapter.notifyDataSetChanged();
        invalidateOptionsMenu(); // Refresh menu so that next round becomes active

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mListView.smoothScrollToPositionFromTop(0, 0, 500);
            }
        }, 100);

        // Bring up next pilot's dialog
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                showNextPilot();
            }
        }, 1000);
		
	}
	
	public void changeFlyingOrder(){
		Intent intent = new Intent(mContext, FlyingOrderEditActivity.class);
        intent.putExtra("race_id", mRid);
    	startActivityForResult(intent, DLG_FLYING_ORDER_EDIT);
	}

	public void groupScore(){
		Intent intent = new Intent(mContext, GroupScoreEditActivity.class);
    	startActivityForResult(intent, DLG_GROUP_SCORE_EDIT);
	}

	private void addPilot(Pilot p){
		RacePilotData datasource = new RacePilotData(this);
		datasource.open();
		datasource.addPilot(p, mRid);
		datasource.close();
	}
	
	private void editPilot(Pilot p){
		Intent intent = new Intent(this, PilotsEditActivity.class);
        intent.putExtra("pilot_id", p.id);
        intent.putExtra("race_id", mRid);
        intent.putExtra("caller", "racemanager");
    	startActivityForResult(intent, DLG_PILOT_EDIT);
	}
	
	public void settings(){
		Intent intent = new Intent(mContext, SettingsActivity.class);
    	startActivityForResult(intent, DLG_SETTINGS);
	}

    public void pilotManager(){
        Intent intent = new Intent(mContext,PilotsActivity.class);
        startActivity(intent);
    }

    public void resultsManager(){
        Intent intent = new Intent(mContext, ResultsActivity.class);
        startActivity(intent);
    }

	public void help(){
		Intent intent = new Intent(mContext, HelpActivity.class);
    	startActivity(intent);
	}

	public void about(){
		Intent intent = new Intent(mContext, AboutActivity.class);
    	startActivity(intent);
	}

	private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
	    @Override
	    public void onReceive(Context ctxt, Intent intent) {
	      int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
	      mPower.setText(String.valueOf(level) + "%");
	    }
	  };
}