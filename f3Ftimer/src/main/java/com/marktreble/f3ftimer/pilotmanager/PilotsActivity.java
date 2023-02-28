/*
 *     ___________ ______   _______
 *    / ____/__  // ____/  /_  __(_)___ ___  ___  _____
 *   / /_    /_ </ /_       / / / / __ `__ \/ _ \/ ___/
 *  / __/  ___/ / __/      / / / / / / / / /  __/ /
 * /_/    /____/_/        /_/ /_/_/ /_/ /_/\___/_/
 *
 * Open Source F3F timer UI and scores database
 *
 */

package com.marktreble.f3ftimer.pilotmanager;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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
import android.widget.ListView;
import android.widget.TextView;

import com.marktreble.f3ftimer.BaseActivity;
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

public class PilotsActivity extends BaseActivity
    implements ListView.OnClickListener {

    // Dialogs
    static int DLG_ADD_PILOT = 1;
    static int DLG_EDIT_PILOT = 2;
    static int DLG_IMPORT = 3;
    static int DLG_EXPORT = 4;

    private int mRequestCode = 0;

    private ArrayAdapter<String> mArrAdapter;
    private ArrayList<String> mArrNames;
    private ArrayList<Integer> mArrIds;
    private ArrayList<Pilot> mArrPilots;

    /**
     * Standard Activity class function
     *
     * @param savedInstanceState Bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mPageTitle = getString(R.string.app_pilots);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.pilot_manager);

        ListView lv = findViewById(android.R.id.list);
        registerForContextMenu(lv);

        getNamesArray();
        setList();
    }

    /**
     * Set up adapter for the ListView
     */
    private void setList() {

        mArrAdapter = new ArrayAdapter<String>(this, R.layout.listrow, R.id.text1, mArrNames) {
            @Override
            public @NonNull
            View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row;

                if (null == convertView) {
                    row = getLayoutInflater().inflate(R.layout.listrow, parent, false);
                    row.setOnClickListener(PilotsActivity.this);
                    row.setOnCreateContextMenuListener(PilotsActivity.this);
                } else {
                    row = convertView;
                }

                Pilot p = mArrPilots.get(position);

                TextView p_name = row.findViewById(R.id.text1);
                p_name.setText(mArrNames.get(position));

                Drawable flag = p.getFlag(mContext);
                if (flag != null) {
                    p_name.setCompoundDrawablesWithIntrinsicBounds(flag, null, null, null);
                    int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
                    p_name.setCompoundDrawablePadding(padding);
                }

                row.setTag(position);

                return row;
            }
        };

        ListView lv = findViewById(android.R.id.list);
        lv.setAdapter(mArrAdapter);
    }

    /**
     * Override onBackPressed
     */
    @Override
    public void onBackPressed() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
    }

    /**
     * ListView.onClickListener
     *
     * @param v View
     */
    @Override
    public void onClick(View v) {
        int position = (int)v.getTag();
        Intent intent = new Intent(this, PilotsEditActivity.class);
        Integer pid = mArrIds.get(position);
        intent.putExtra("pilot_id", pid);
        intent.putExtra("caller", "pilotmanager");
        mRequestCode = DLG_EDIT_PILOT;
        mStartForResult.launch(intent);
        //startActivityForResult(intent, DLG_EDIT_PILOT);
    }

    private final ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                if (mRequestCode == DLG_ADD_PILOT)
                    mArrAdapter.add("");
                getNamesArray();
                mArrAdapter.notifyDataSetChanged();
            }
        });

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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.pilots, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        int id = item.getItemId();
        if (id == R.id.menu_add_pilot) {
            addPilot();
        } else if (id == R.id.menu_import_pilots) {
            importPilots();
        } else if (id == R.id.menu_export_pilots) {
            exportPilots();
        } else if (id == R.id.menu_race_manager) {
            raceManager();
        } else if (id == R.id.menu_results_manager) {
            resultsManager();
        } else if (id == R.id.menu_help) {
            help();
        } else if (id == R.id.menu_about) {
            about();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public void addPilot() {
        Intent intent = new Intent(mContext, PilotsEditActivity.class);
        intent.putExtra("caller", "pilotmanager");
        mRequestCode = DLG_ADD_PILOT;
        mStartForResult.launch(intent);
        //startActivityForResult(intent, PilotsActivity.DLG_ADD_PILOT);
    }

    public void importPilots() {
        Intent intent = new Intent(mContext, FileImportPilots.class);
        mRequestCode = DLG_IMPORT;
        mStartForResult.launch(intent);
        //startActivityForResult(intent, DLG_IMPORT);
    }

    public void exportPilots() {
        Intent intent = new Intent(mContext, FileExportPilots.class);
        mRequestCode = DLG_EXPORT;
        mStartForResult.launch(intent);
        //startActivityForResult(intent, DLG_IMPORT);
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
