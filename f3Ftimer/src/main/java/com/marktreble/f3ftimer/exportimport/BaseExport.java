package com.marktreble.f3ftimer.exportimport;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.pilot.PilotData;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;

import java.util.ArrayList;

/**
 * Created by marktreble on 09/12/2015.
 * Base class for data exports
 */
public abstract class BaseExport extends Activity {

    final CharSequence[] filetypes = {"json", "csv"};
    final static int EXPORT_FILE_TYPE_JSON = 0;
    final static int EXPORT_FILE_TYPE_CSV = 1;

    private static final int ACTION_PICK_FOLDER = 1;

    Context mContext;
    Activity mActivity;
    String mSaveFolder;

    public AlertDialog mDlg;

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mContext = this;
        mActivity = this;

    }

    protected void promptForSaveFolder(String folder) {
        if (folder == null) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            mSaveFolder = sharedPref.getString("export_save_folder", Environment.getExternalStorageDirectory().getPath());
        } else {
            mSaveFolder = folder;
        }

        mDlg = new AlertDialog.Builder(mContext)
                .setTitle("Save Location")
                .setMessage("Your file(s) will be exported to:\n" + mSaveFolder)
                .setNeutralButton("Change Path", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
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
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("Export", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        beginExport();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mActivity.finish();
                    }
                }).show();

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("EXPORT", "ONACTIVITYRESULT " + requestCode + ":" + resultCode);
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

    void beginExport() {
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
        String racetimes = datasource2.getTimesSerialized(race_id, round);
        datasource2.close();

        StringBuilder csvdata = new StringBuilder();

        String race_params = "";
        race_params += String.format("\"%d\",", race.race_id);
        race_params += String.format("\"%s\",", race.name);
        race_params += String.format("\"%s\",", ""); // F3XV Location
        race_params += String.format("\"%s\",", ""); // F3XV Start Date
        race_params += String.format("\"%s\",", ""); // F3XV End Date
        race_params += String.format("\"%s\",", ""); // F3XV Type
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

}
