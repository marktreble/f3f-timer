/*
 * PilotsActivity
 * Entry point for Pilot Manager App
 * Provide a list of Pilots in the database
 * Pilots can be added/edited or deleted
 */
package com.marktreble.f3ftimer.pilotmanager;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.pilot.PilotData;
import com.marktreble.f3ftimer.dialog.AboutActivity;
import com.marktreble.f3ftimer.dialog.HelpActivity;
import com.marktreble.f3ftimer.dialog.PilotsEditActivity;
import com.marktreble.f3ftimer.exportimport.FileExportPilots;
import com.marktreble.f3ftimer.exportimport.FileImportPilots;
import com.marktreble.f3ftimer.racemanager.RaceListActivity;
import com.marktreble.f3ftimer.resultsmanager.ResultsActivity;

import java.util.ArrayList;

public class PilotsActivity extends ListActivity {

    // Dialogs
    static int DLG_ADD_PILOT = 1;
    static int DLG_EDIT_PILOT = 2;
    static int DLG_IMPORT = 2;

    private ArrayAdapter<String> mArrAdapter;
    private ArrayList<String> mArrNames;
    private ArrayList<Integer> mArrIds;
    private ArrayList<Pilot> mArrPilots;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ImageView view = (ImageView) findViewById(android.R.id.home);
        Resources r = getResources();
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, r.getDisplayMetrics());
        view.setPadding(0, 0, px, 0);

        setContentView(R.layout.pilot_manager);

        mContext = this;

        getNamesArray();
        setList();

        registerForContextMenu(getListView());
    }

    private void setList() {

        mArrAdapter = new ArrayAdapter<String>(this, R.layout.listrow, R.id.text1, mArrNames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row;

                if (null == convertView) {
                    row = getLayoutInflater().inflate(R.layout.listrow, parent, false);
                } else {
                    row = convertView;
                }

                Pilot p = mArrPilots.get(position);

                TextView p_name = (TextView) row.findViewById(R.id.text1);
                p_name.setText(mArrNames.get(position));

                Drawable flag = p.getFlag(mContext);
                if (flag != null) {
                    p_name.setCompoundDrawablesWithIntrinsicBounds(flag, null, null, null);
                    int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
                    p_name.setCompoundDrawablePadding(padding);
                }
                return row;
            }
        };

        setListAdapter(mArrAdapter);
    }

    public void onBackPressed() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(this, PilotsEditActivity.class);
        Integer pid = mArrIds.get(position);
        intent.putExtra("pilot_id", pid);
        intent.putExtra("caller", "pilotmanager");
        startActivityForResult(intent, DLG_EDIT_PILOT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == DLG_ADD_PILOT)
                mArrAdapter.add("");
            getNamesArray();
            mArrAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        if (v.getId() == android.R.id.list) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle(mArrNames.get(info.position));
            menu.add(Menu.NONE, 0, 0, "Delete");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int menuItemIndex = item.getItemId();

        if (menuItemIndex == 0) {
            // Delete Selected
            Log.i("DELETING PILOT:", Integer.toString(info.position));
            Integer id = mArrIds.get(info.position);
            PilotData datasource = new PilotData(PilotsActivity.this);
            datasource.open();
            datasource.deletePilot(id);
            datasource.close();
            getNamesArray();
//          mArrAdapter.remove(mArrNames.get(info.position));
            mArrAdapter.notifyDataSetChanged();

        }
        return true;
    }

    private void getNamesArray() {
        PilotData datasource = new PilotData(this);
        datasource.open();
        ArrayList<Pilot> allPilots = datasource.getAllPilots();
        datasource.close();

        if (mArrNames == null) {
            mArrNames = new ArrayList<>();
            mArrIds = new ArrayList<>();
            mArrPilots = new ArrayList<>();
        }

        while (mArrNames.size() < allPilots.size()) mArrNames.add("");
        while (mArrIds.size() < allPilots.size()) mArrIds.add(0);
        while (mArrPilots.size() < allPilots.size()) mArrPilots.add(new Pilot());
        while (mArrNames.size() > allPilots.size()) mArrNames.remove(0);
        while (mArrIds.size() > allPilots.size()) mArrIds.remove(0);
        while (mArrPilots.size() < allPilots.size()) mArrPilots.remove(0);

        int c = 0;
        for (Pilot p : allPilots) {
            mArrNames.set(c, String.format("%s %s", p.firstname, p.lastname));
            mArrIds.set(c, p.id);
            mArrPilots.set(c, p);
            c++;

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i("MENU", "onCREATEOPTIONSMENU");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.pilots, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.menu_add_pilot:
                addPilot();
                return true;
            case R.id.menu_import_pilots:
                importPilots();
                return true;
            case R.id.menu_export_pilots:
                exportPilots();
                return true;
            case R.id.menu_race_manager:
                raceManager();
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

    public void addPilot() {
        Intent intent = new Intent(mContext, PilotsEditActivity.class);
        intent.putExtra("caller", "pilotmanager");
        startActivityForResult(intent, PilotsActivity.DLG_ADD_PILOT);
    }

    public void importPilots() {
        Intent intent = new Intent(mContext, FileImportPilots.class);
        startActivityForResult(intent, DLG_IMPORT);
    }

    public void exportPilots() {
        Intent intent = new Intent(mContext, FileExportPilots.class);
        startActivityForResult(intent, DLG_IMPORT);
    }

    public void raceManager() {
        Intent intent = new Intent(mContext, RaceListActivity.class);
        startActivity(intent);
    }

    public void resultsManager() {
        Intent intent = new Intent(mContext, ResultsActivity.class);
        startActivity(intent);
    }

    public void help() {
        Intent intent = new Intent(mContext, HelpActivity.class);
        startActivity(intent);
    }

    public void about() {
        Intent intent = new Intent(mContext, AboutActivity.class);
        startActivity(intent);
    }
}
