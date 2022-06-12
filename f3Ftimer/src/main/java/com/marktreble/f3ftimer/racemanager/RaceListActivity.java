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

package com.marktreble.f3ftimer.racemanager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
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
import com.marktreble.f3ftimer.F3FtimerApplication;
import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.pilot.PilotData;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.dialog.AboutActivity;
import com.marktreble.f3ftimer.dialog.GenericAlert;
import com.marktreble.f3ftimer.dialog.GenericListPicker;
import com.marktreble.f3ftimer.dialog.HelpActivity;
import com.marktreble.f3ftimer.dialog.NewRaceActivity;
import com.marktreble.f3ftimer.dialog.SettingsActivity;
import com.marktreble.f3ftimer.exportimport.BluetoothExportRace;
import com.marktreble.f3ftimer.exportimport.BluetoothImportRace;
import com.marktreble.f3ftimer.exportimport.F3ftimerApiImportRace;
import com.marktreble.f3ftimer.exportimport.F3xvaultApiImportRace;
import com.marktreble.f3ftimer.exportimport.FileExportRace;
import com.marktreble.f3ftimer.exportimport.FileImportRace;
import com.marktreble.f3ftimer.pilotmanager.PilotsActivity;
import com.marktreble.f3ftimer.resultsmanager.ResultsActivity;

import java.util.ArrayList;
import java.util.Arrays;

public class RaceListActivity extends BaseActivity
        implements ListView.OnClickListener {

    static int DLG_NEW_RACE = 0;
    static int START_RACE = 1;
    static int DLG_IMPORT = 2;
    static int DLG_SETTINGS = 9;
    static final int PERMISSIONS_REQUEST = 101;

    private ArrayAdapter<String> mArrAdapter;
    private ArrayList<String> mArrNames;
    private ArrayList<Integer> mArrIds;

    public Intent mIntent;

    static final String DIALOG = "dialog";

    GenericAlert mDLG;
    GenericListPicker mDLG2;

    static final int IMPORT_SRC_BT = 100;
    static final int IMPORT_SRC_FILE = 101;
    static final int IMPORT_SRC_F3FTIMER_API = 102;
    static final int IMPORT_SRC_F3XVAULT_API = 103;

    static final int EXPORT_SRC_BT = 100;
    static final int EXPORT_SRC_FILE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mPageTitle = getString(R.string.app_race);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.race_manager);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (!checkIfAlreadyhavePermission()) {
                requestForSpecificPermission();
            }
        }

        mIntent = getIntent();

        getNamesArray();
        setList();

        ListView lv = findViewById(android.R.id.list);
        lv.setAdapter(mArrAdapter);
        registerForContextMenu(lv);
    }



    private boolean checkIfAlreadyhavePermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return (result == PackageManager.PERMISSION_GRANTED);
    }

    private void requestForSpecificPermission() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, PERMISSIONS_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                //not granted

                String[] buttons_array = new String[1];
                buttons_array[0] = getString(android.R.string.ok);

                mDLG = GenericAlert.newInstance(
                        getString(R.string.ttl_fs_permission_denied),
                        getString(R.string.msg_fs_permission_denied),
                        buttons_array,
                        null
                );

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.add(mDLG, DIALOG);
                ft.commit();
            }
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void setList() {
        mArrAdapter = new ArrayAdapter<String>(this, R.layout.listrow, mArrNames) {
            @Override
            public @NonNull
            View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row;

                if (null == convertView) {
                    row = getLayoutInflater().inflate(R.layout.listrow, parent, false);
                    row.setOnClickListener(RaceListActivity.this);
                    row.setOnCreateContextMenuListener(RaceListActivity.this);
                } else {
                    row = convertView;
                }

                row.setTag(mArrIds.get(position));

                TextView tv = row.findViewById(R.id.text1);
                tv.setText(mArrNames.get(position));

                return row;
            }
        };
    }

    @Override
    public void onBackPressed() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
    }

    @Override
    public void onClick(View v) {
        // Get the extras from this intent (will have usb perms!)
        Bundle extras = mIntent.getExtras();
        if (extras == null) extras = new Bundle();
        // Add the race id to this bundle
        int pid = (int)v.getTag();
        extras.putInt("race_id", pid);

        // Now start race activity with the modified bundle!
        Intent intent = new Intent(this, RaceActivity.class);
        intent.putExtras(extras);
        startActivityForResult(intent, RaceListActivity.START_RACE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /*
         * Update the race list
         */
        getNamesArray();
        mArrAdapter.notifyDataSetChanged();

        /*
         * Restart the app if the theme has changed
         */
        if (requestCode == RaceListActivity.DLG_SETTINGS) {
            ((F3FtimerApplication) getApplication()).restartApp();
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

    private void getNamesArray() {

        RaceData datasource = new RaceData(this);
        datasource.open();
        ArrayList<Race> allRaces = datasource.getAllRaces();
        datasource.close();

        if (mArrNames == null) {
            mArrNames = new ArrayList<>();
            mArrIds = new ArrayList<>();
        }

        while (mArrNames.size() < allRaces.size()) mArrNames.add("");
        while (mArrIds.size() < allRaces.size()) mArrIds.add(0);
        while (mArrNames.size() > allRaces.size()) mArrNames.remove(0);
        while (mArrIds.size() > allRaces.size()) mArrIds.remove(0);

        int c = 0;
        for (Race r : allRaces) {
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

    public void newRace() {
        PilotData datasource = new PilotData(this);
        datasource.open();
        ArrayList<Pilot> allPilots = datasource.getAllPilots();
        datasource.close();

        if (allPilots.size() == 0) {
            String[] buttons_array = new String[2];
            buttons_array[0] = getString(android.R.string.cancel);
            buttons_array[1] = getString(R.string.btn_open_pilot_manager);

            mDLG = GenericAlert.newInstance(
                    getString(R.string.err_no_pilots),
                    getString(R.string.err_no_pilots_instruction),
                buttons_array,
                new ResultReceiver(new Handler(Looper.getMainLooper())) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        super.onReceiveResult(resultCode, resultData);

                        if (resultCode == 1) {
                            Intent intent = new Intent(RaceListActivity.this, PilotsActivity.class);
                            startActivity(intent);
                        }
                    }
                }
            );

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(mDLG, DIALOG);
            ft.commit();

        } else {

            Intent intent = new Intent(RaceListActivity.this, NewRaceActivity.class);
            startActivityForResult(intent, RaceListActivity.DLG_NEW_RACE);
        }
    }

    public void importRace() {
        String[] buttons_array = new String[1];
        buttons_array[0] = getString(android.R.string.cancel);

        ArrayList<String> options = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.import_sources)));
        mDLG2 = GenericListPicker.newInstance(
                getString(R.string.select_import_source),
                options,
                buttons_array,
                new ResultReceiver(new Handler()) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        super.onReceiveResult(resultCode, resultData);
                        Intent intent;
                        switch (resultCode) {
                            case IMPORT_SRC_BT:
                                intent = new Intent(mContext, BluetoothImportRace.class);
                                startActivityForResult(intent, DLG_IMPORT);
                                break;
                            case IMPORT_SRC_FILE:
                                intent = new Intent(mContext, FileImportRace.class);
                                startActivityForResult(intent, DLG_IMPORT);
                                break;
                            case IMPORT_SRC_F3FTIMER_API:
                                intent = new Intent(mContext, F3ftimerApiImportRace.class);
                                startActivityForResult(intent, DLG_IMPORT);
                                break;
                            case IMPORT_SRC_F3XVAULT_API:
                                intent = new Intent(mContext, F3xvaultApiImportRace.class);
                                startActivityForResult(intent, DLG_IMPORT);
                                break;
                        }
                    }
                }
        );

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(mDLG2, DIALOG);
        ft.commit();

    }

    public void exportRace() {
        String[] buttons_array = new String[1];
        buttons_array[0] = getString(android.R.string.cancel);

        ArrayList<String> options = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.export_destinations)));
        mDLG2 = GenericListPicker.newInstance(
                getString(R.string.select_export_destination),
                options,
                buttons_array,
                new ResultReceiver(new Handler(Looper.getMainLooper())) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        super.onReceiveResult(resultCode, resultData);
                        Intent intent;
                        switch (resultCode) {
                            case EXPORT_SRC_BT:
                                intent = new Intent(mContext, BluetoothExportRace.class);
                                startActivity(intent);
                                break;
                            case EXPORT_SRC_FILE:
                                intent = new Intent(mContext, FileExportRace.class);
                                startActivity(intent);
                                break;
                        }
                    }
                }
        );

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(mDLG2, DIALOG);
        ft.commit();

    }

    public void settings() {
        Intent intent = new Intent(mContext, SettingsActivity.class);
        startActivityForResult(intent, DLG_SETTINGS);
    }

    public void pilotManager() {
        Intent intent = new Intent(mContext, PilotsActivity.class);
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
