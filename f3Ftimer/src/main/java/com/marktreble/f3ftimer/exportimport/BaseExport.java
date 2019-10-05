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

package com.marktreble.f3ftimer.exportimport;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.constants.IComm;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.pilot.PilotData;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;
import com.marktreble.f3ftimer.dialog.GenericAlert;
import com.marktreble.f3ftimer.dialog.GenericRadioPicker;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class BaseExport extends AppCompatActivity {

    final String[] filetypes = {"json", "csv"};
    final static int EXPORT_FILE_TYPE_JSON = 0;
    final static int EXPORT_FILE_TYPE_CSV = 1;

    private static final int ACTION_PICK_FOLDER = 1;

    Context mContext;
    Activity mActivity;
    String mSaveFolder;

    static final String DIALOG = "dialog";

    GenericAlert mDLG;
    GenericRadioPicker mDLG3;

    protected Integer mExportFileType = -1;

    String mProgressMessage;

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.api);

        mContext = this;
        mActivity = this;

        if (savedInstanceState != null) {
            mProgressMessage = savedInstanceState.getString("progress_message");
            View progress = findViewById(R.id.progress);
            TextView progressLabel = progress.findViewById(R.id.progressLabel);
            progressLabel.setText(mProgressMessage);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("progress_message", mProgressMessage);
    }

    protected void showProgress(final String msg) {
        mProgressMessage = msg;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View progress = findViewById(R.id.progress);
                TextView progressLabel = progress.findViewById(R.id.progressLabel);
                progressLabel.setText(msg);
            }
        });

    }

    protected void hideProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View progress = findViewById(R.id.progress);
                TextView progressLabel = progress.findViewById(R.id.progressLabel);
                progressLabel.setText("");
            }
        });
    }

    public void onResume() {
        super.onResume();

        this.registerReceiver(onBroadcast, new IntentFilter(IComm.RCV_UPDATE));
    }

    public void onPause() {
        super.onPause();

        this.unregisterReceiver(onBroadcast);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTION_PICK_FOLDER && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor ed = sharedPref.edit();
                ed.putString("export_save_folder", uri.getPath());
                ed.apply();

                promptForSaveFolder(uri.getPath());
            }

        } else {
            promptForSaveFolder(null);
        }
    }

    protected void beginExport() {

    }

    protected String getSerialisedRaceData(int race_id, int round) {
        RaceData datasource = new RaceData(mContext);
        datasource.open();
        String race = datasource.getSerialized(race_id);
        String racegroups = datasource.getGroupsSerialized(race_id, round);
        datasource.close();

        RacePilotData datasource2 = new RacePilotData(mContext);
        datasource2.open();
        String racepilots = datasource2.getPilotsSerialized(race_id);
        String racetimes = datasource2.getTimesSerialized(race_id, round);
        datasource2.close();

        return String.format("{\"race\":%s, \"racepilots\":%s,\"racetimes\":%s,\"racegroups\":%s}\n\n", race, racepilots, racetimes, racegroups);
    }

    protected String getSerialisedPilotData() {
        PilotData datasource = new PilotData(mContext);
        datasource.open();
        String pilots = datasource.getSerialized();
        datasource.close();

        return pilots;

    }

    protected String getCSVRaceData(int race_id, int round) {

        RaceData datasource = new RaceData(mContext);
        datasource.open();
        Race race = datasource.getRace(race_id);
        RaceData.Group[] racegroups = datasource.getGroups(race_id, round);
        datasource.close();

        RacePilotData datasource2 = new RacePilotData(mContext);
        datasource2.open();
        ArrayList<Pilot> racepilots = datasource2.getAllPilotsForRace(race_id, 0, race.offset, 0);
        datasource2.close();

        StringBuilder csvdata = new StringBuilder();

        String race_params = "";
        race_params += String.format("\"%d\",", race.race_id);
        race_params += String.format("\"%s\",", race.name);
        race_params += String.format("\"%s\",", ""); // F3XV Location
        race_params += String.format("\"%s\",", ""); // F3XV Start Date
        race_params += String.format("\"%s\",", ""); // F3XV End Date
        race_params += String.format("\"%s\",", ""); // F3XV Type
        race_params += String.format("\"%s\",", ""); // F3XV Num Rounds
        race_params += String.format("\"%d\",", race.round);
        race_params += String.format("\"%d\",", race.type);
        race_params += String.format("\"%d\",", race.offset);
        race_params += String.format("\"%d\",", race.status);
        race_params += String.format("\"%d\",", race.rounds_per_flight);
        race_params += String.format("\"%d\"\n", race.start_number);

        csvdata.append(race_params);
        csvdata.append("\n");

        for (Pilot p : racepilots) {
            String pilot_params = "";
            pilot_params += String.format("%d,", p.pilot_id);
            pilot_params += String.format("\"%s\",", p.number);
            pilot_params += String.format("\"%s\",", p.firstname);
            pilot_params += String.format("\"%s\",", p.lastname);
            pilot_params += ","; // F3XV Pilot Class - not required?
            pilot_params += String.format("\"%s\",", p.nac_no);
            pilot_params += String.format("\"%s\",", p.fai_id);
            pilot_params += ","; // F3XV FAI License?
            pilot_params += String.format("\"%s\",", p.team);
            // Extra data not in f3xvault api
            pilot_params += String.format("%d,", p.status);
            pilot_params += String.format("\"%s\",", p.email);
            pilot_params += String.format("\"%s\",", p.frequency);
            pilot_params += String.format("\"%s\",", p.models);
            pilot_params += String.format("\"%s\",", p.nationality);
            pilot_params += String.format("\"%s\"\n", p.language);
            csvdata.append(pilot_params);
        }

        csvdata.append("\n");

        StringBuilder group_params = new StringBuilder();
        StringBuilder start_params = new StringBuilder();
        for (RaceData.Group group : racegroups) {
            if (group_params.length() > 0) group_params.append(",");
            group_params.append(String.format("%d", group.num_groups));

            if (start_params.length() > 0) start_params.append(",");
            start_params.append(String.format("%d", group.start_pilot));
        }

        csvdata.append(group_params).append("\n");
        csvdata.append(start_params).append("\n");
        csvdata.append("\n");


        return csvdata.toString();
    }

    protected String getCSVPilotData() {
        PilotData datasource = new PilotData(mContext);
        datasource.open();
        String pilots = datasource.getCSV();
        datasource.close();

        return pilots;

    }

    protected void call(String func, @Nullable String data) {
        Intent i = new Intent(IComm.RCV_UPDATE);
        i.putExtra("cmd", func);
        i.putExtra("dta", data);
        sendBroadcast(i);
    }


    protected void promptForSaveFolder(String folder) {
        if (folder == null) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            mSaveFolder = sharedPref.getString("export_save_folder", Environment.getExternalStorageDirectory().getPath());
        } else {
            mSaveFolder = folder;
        }

        String[] buttons_array = new String[3];
        buttons_array[0] = getString(android.R.string.cancel);
        buttons_array[1] = getString(R.string.btn_change_path);
        buttons_array[2] = getString(R.string.btn_export);

        mDLG = GenericAlert.newInstance(
                getString(R.string.ttl_save_location),
                String.format(getString(R.string.msg_save_location), mSaveFolder),
                buttons_array,
                new ResultReceiver(new Handler()) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        super.onReceiveResult(resultCode, resultData);

                        switch (resultCode) {
                            case 0:
                                mActivity.finish();
                                break;
                            case 1:
                                Intent i = new Intent(mContext, FilteredFilePickerActivity.class);
                                // This works if you defined the intent filter
                                // Intent i = new Intent(Intent.ACTION_GET_CONTENT);

                                // Set these depending on your use case. These are the defaults.
                                i.putExtra(FilteredFilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                                i.putExtra(FilteredFilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
                                i.putExtra(FilteredFilePickerActivity.EXTRA_MODE, FilteredFilePickerActivity.MODE_DIR);

                                // Configure initial directory by specifying a String.
                                // You could specify a String like "/storage/emulated/0/", but that can
                                // dangerous. Always use Android's API calls to get paths to the SD-card or
                                // internal memory.
                                i.putExtra(FilteredFilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

                                startActivityForResult(i, ACTION_PICK_FOLDER);
                                mDLG.dismiss();
                                break;
                            case 2:
                                call("beginExport", null);
                                break;
                        }
                    }
                }
        );

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.addToBackStack(null);
        ft.add(mDLG, DIALOG);
        ft.commit();

    }

    private void showExportTypeList() {
        String[] buttons_array = new String[2];
        buttons_array[0] = getString(android.R.string.cancel);
        buttons_array[1] = getString(R.string.button_next);

        mDLG3 = GenericRadioPicker.newInstance(
                getString(R.string.ttl_select_file_type),
                new ArrayList<>(Arrays.asList(filetypes)),
                buttons_array,
                new ResultReceiver(new Handler()) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        super.onReceiveResult(resultCode, resultData);
                        switch (resultCode) {
                            case 0:
                                mActivity.finish();
                                break;
                            case 1:
                                mExportFileType = -1;
                                if (resultData.containsKey("checked")) {
                                    mExportFileType = resultData.getInt("checked");
                                }
                                if (mExportFileType >= 0) {
                                    call("promptForSaveFolder", null);
                                } else {
                                    mActivity.finish();
                                }
                                break;
                        }
                    }
                }
        );

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(mDLG3, DIALOG);
        ft.commit();

    }

    private BroadcastReceiver onBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("cmd")) {
                Bundle extras = intent.getExtras();
                if (extras == null) {
                    return;
                }
                String cmd = extras.getString("cmd", "");
                String dta = extras.getString("dta");

                if (cmd.equals("promptForSaveFolder")) {
                    promptForSaveFolder(dta);
                }

                if (cmd.equals("beginExport")) {
                    beginExport();
                }

                if (cmd.equals("showExportTypeList")) {
                    showExportTypeList();
                }
            }
        }
    };

}
