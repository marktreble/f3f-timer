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

import com.marktreble.f3ftimer.data.pilot.PilotData;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;

/**
 * Created by marktreble on 09/12/2015.
 */
public class BaseExport extends Activity {

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

    protected void promptForSaveFolder(){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mSaveFolder = sharedPref.getString("export_save_folder", Environment.getExternalStorageDirectory().getPath());

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
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor ed = sharedPref.edit();
            ed.putString("export_save_folder", uri.getPath());
            ed.commit();

            promptForSaveFolder();
        } else {
            promptForSaveFolder();
        }
    }

    void beginExport(){}

    protected String getSerialisedRaceData(int race_id, int round){
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

        String data = String.format("{\"race\":%s, \"racepilots\":%s,\"racetimes\":%s,\"racegroups\":%s}\n\n", race, racepilots, racetimes, racegroups);

        return data;
    }

    protected String getSerialisedRaceDataFile(int race_id, int round){
        RaceData datasource = new RaceData(mContext);
        datasource.open();
        String race = datasource.getSerialized(race_id);
        String racegroups = datasource.getGroupsSerialized(race_id, round);
        datasource.close();

        RacePilotData datasource2 = new RacePilotData(mContext);
        datasource2.open();
        String racepilots = datasource2.getPilotsSerialized(race_id);
        String racetimes = datasource2.getTimesSerializedExt(race_id, round);
        datasource2.close();

        String data = String.format("{\"race\":%s, \"racepilots\":%s,\"racetimes\":%s,\"racegroups\":%s}\n\n", race, racepilots, racetimes, racegroups);

        return data;
    }

    protected String getSerialisedPilotData(){
        PilotData datasource = new PilotData(mContext);
        datasource.open();
        String pilots = datasource.getSerialized();
        datasource.close();

        return pilots;

    }
}
