/*
 * RaceListActivity
 * Entry Point for Race Manager App
 * Provides a list of races stored on the device along with add/delete controls
 * Races cannot be edited once initialised
 */
package com.marktreble.f3ftimer.racemanager;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.marktreble.f3ftimer.*;
import com.marktreble.f3ftimer.exportimport.BluetoothExport;
import com.marktreble.f3ftimer.exportimport.BluetoothImport;
import com.marktreble.f3ftimer.data.race.*;
import com.marktreble.f3ftimer.data.pilot.*;
import com.marktreble.f3ftimer.dialog.*;
import com.marktreble.f3ftimer.exportimport.FileExportRace;
import com.marktreble.f3ftimer.exportimport.FileImportRace;
import com.marktreble.f3ftimer.pilotmanager.PilotsActivity;
import com.marktreble.f3ftimer.resultsmanager.ResultsActivity;

public class RaceListActivity extends ListActivity {

    static int DLG_NEW_RACE = 0;
    static int START_RACE = 1;
    static int DLG_IMPORT = 2;

	private ArrayAdapter<String> mArrAdapter;
	private ArrayList<String> mArrNames;
	private ArrayList<Integer> mArrIds;

	public Intent mIntent;
	
	static final boolean DEBUG = true;

	private Context mContext;
    private ListActivity mActivity;

    private AlertDialog mDlg;
    private AlertDialog.Builder mDlgb;

    static final int IMPORT_SRC_BT = 0;
    static final int IMPORT_SRC_FILE = 1;

    static final int EXPORT_SRC_BT = 0;
    static final int EXPORT_SRC_FILE = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        Log.i("ONACTIVITYRESULT", "ONCREATE");
		super.onCreate(savedInstanceState);
		
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
		ImageView view = (ImageView)findViewById(android.R.id.home);
		Resources r = getResources();
		int px = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, r.getDisplayMetrics());
		view.setPadding(0, 0, px, 0);
				
		mIntent = getIntent();

		mContext = this;
        mActivity = this;
		
		setContentView(R.layout.race_manager);
			    
	    getNamesArray();
        setList();

        registerForContextMenu(getListView());
       
	}

    private void setList(){
        mArrAdapter = new ArrayAdapter<>(this, R.layout.listrow , mArrNames);
        setListAdapter(mArrAdapter);
    }

	public void onBackPressed(){
		Intent homeIntent = new Intent(Intent.ACTION_MAIN);
	    homeIntent.addCategory( Intent.CATEGORY_HOME );
	    homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  
	    startActivity(homeIntent); 
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// Get the extras from this intent (will have usb perms!)
		Bundle extras = mIntent.getExtras();
		if (extras == null) extras = new Bundle();
		// Add the race id to this bundle
        Integer pid = mArrIds.get(position);
        extras.putInt("race_id", pid);
        
        // Now start race activity with the modified bundle!
        Intent intent = new Intent(this, RaceActivity.class);
        intent.putExtras(extras);
    	startActivityForResult(intent, RaceListActivity.START_RACE);
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
        Log.i("ONACTIVITYRESULT", "REQUEST = " + Integer.toString(requestCode) + " RESULT = " + Integer.toString(resultCode));
		if(resultCode==RESULT_OK){
            if (requestCode == RaceListActivity.DLG_NEW_RACE) {
                Log.i("ONACTIVITYRESULT", "NOTIFYCHANGED");
                getNamesArray();
                mArrAdapter.notifyDataSetChanged();

            }

            if (requestCode == RaceListActivity.DLG_IMPORT) {
                getNamesArray();
                mArrAdapter.notifyDataSetChanged();
                
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("Import Race")
                        .setMessage("Your Race(s) have been imported")
                        .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });
                builder.create().show();
                
            }

            if (requestCode == RaceListActivity.DLG_IMPORT) {
                getNamesArray();
                mArrAdapter.notifyDataSetChanged();

                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("Import Race")
                        .setMessage("Your Race(s) have been imported")
                        .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });
                builder.create().show();

            }
		}
    }
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	    ContextMenuInfo menuInfo) {
	  if (v.getId()==android.R.id.list) {
	    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
	    menu.setHeaderTitle(mArrNames.get(info.position));
	    menu.add(Menu.NONE, 0, 0, "Delete");
	  }
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
	  AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
	  int menuItemIndex = item.getItemId();

	  if (menuItemIndex == 0){
		  // Delete Selected
		  Integer id = mArrIds.get(info.position);
  		  RaceData datasource = new RaceData(RaceListActivity.this);
  		  datasource.open();
  		  datasource.deleteRace(id);
  		  datasource.close();
		
  		  getNamesArray();
	   	  mArrAdapter.notifyDataSetChanged();


	  }
	  return true;
	}
	private void getNamesArray(){

		RaceData datasource = new RaceData(this);
		datasource.open();
		ArrayList<Race> allRaces = datasource.getAllRaces();
		datasource.close();

        if (mArrNames == null) {
            mArrNames = new ArrayList<>();
            mArrIds = new ArrayList<>();
        }

        while(mArrNames.size() < allRaces.size()) mArrNames.add("");
        while(mArrIds.size() < allRaces.size()) mArrIds.add(0);
        while(mArrNames.size() > allRaces.size()) mArrNames.remove(0);
        while(mArrIds.size() > allRaces.size()) mArrIds.remove(0);

        int c = 0;
		for (Race r: allRaces){
			mArrNames.set(c, String.format("%s", r.name));
			mArrIds.set(c, r.id);
            c++;
			
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.race_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	        case R.id.menu_new_race:
	            newRace();
	            return true;
            case R.id.menu_import_race:
                importRace();
                return true;
            case R.id.menu_export_race:
                exportRace();
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
	
	public void newRace(){
        PilotData datasource = new PilotData(this);
        datasource.open();
        ArrayList<Pilot> allPilots = datasource.getAllPilots();
        datasource.close();

        if (allPilots.size() == 0){
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle("No Pilots")
            .setMessage("Before you create a race, you must set up a database of pilots in the Pilot Manager")
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                }
            }).setPositiveButton("Open Pilot Manager", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent intent = new Intent(RaceListActivity.this, PilotsActivity.class);
                    startActivity(intent);
                }
            });
            builder.create().show();
        } else {

            Intent intent = new Intent(RaceListActivity.this, NewRaceActivity.class);
            startActivityForResult(intent, RaceListActivity.DLG_NEW_RACE);
        }
	}

    public void importRace(){
        mDlgb = new AlertDialog.Builder(mContext)
                .setTitle(R.string.select_import_source)
                .setItems(R.array.import_sources, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent;
                        switch (which) {
                            case IMPORT_SRC_BT:
                                intent = new Intent(mContext, BluetoothImport.class);
                                startActivityForResult(intent, DLG_IMPORT);
                                break;
                            case IMPORT_SRC_FILE:
                                intent = new Intent(mContext, FileImportRace.class);
                                startActivityForResult(intent, DLG_IMPORT);
                                break;
                        }
                    }
                });
        mDlg = mDlgb.create();
        mDlg.show();
    }

    public void exportRace(){
        mDlgb = new AlertDialog.Builder(mContext)
                .setTitle(R.string.select_export_source)
                .setItems(R.array.export_sources, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent;
                        switch (which) {
                            case EXPORT_SRC_BT:
                                intent = new Intent(mContext, BluetoothExport.class);
                                startActivity(intent);
                                break;
                            case EXPORT_SRC_FILE:
                                intent = new Intent(mContext, FileExportRace.class);
                                startActivity(intent);
                                break;
                        }
                    }
                });
        mDlg = mDlgb.create();
        mDlg.show();
    }

    public void settings(){
		Intent intent = new Intent(mContext, SettingsActivity.class);
    	startActivityForResult(intent, 1);
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
    	startActivityForResult(intent, 1);
	}

	public void about(){
		Intent intent = new Intent(mContext, AboutActivity.class);
    	startActivityForResult(intent, 1);
	}


}
