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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class BaseExport extends AppCompatActivity {

    final String[] filetypes = {"json", "csv"};
    final static int EXPORT_FILE_TYPE_JSON = 0;
    final static int EXPORT_FILE_TYPE_CSV = 1;

    private static final int WRITE_REQUEST_CODE = 2;

    Context mContext;
    Activity mActivity;

    static final String DIALOG = "dialog";

    GenericAlert mDLG;
    GenericRadioPicker mDLG3;

    protected Integer mExportFileType = -1;
    protected JSONArray mArrExportFiles = new JSONArray();

    String mProgressMessage;

    protected static final long PROGRESS_DELAY = 500;

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

            try {
                mArrExportFiles = new JSONArray(savedInstanceState.getString("export_races"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("progress_message", mProgressMessage);
        outState.putString("export_races", mArrExportFiles.toString());
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
        if (resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        if (requestCode == WRITE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    showProgress(getString(R.string.exporting));

                    final Uri uri = data.getData();

                    final int takeFlags = getIntent().getFlags()
                            & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    // Check for the freshest data.
                    getContentResolver().takePersistableUriPermission(uri, takeFlags);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            writeDocument(uri);
                        }
                    }, PROGRESS_DELAY);

                } else {
                    finish();
                }
            }
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

    protected void showExportTypeList() {
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

                                    call("beginExport", null);
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

    protected void createDocument() {
        JSONObject r = null;
        String fileName = "";
        try {
            r = mArrExportFiles.getJSONObject(0);
            fileName = r.getString("name");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (r == null){
            finish();
            return;
        }

        String mimeType = "";
        switch (mExportFileType) {
            case EXPORT_FILE_TYPE_JSON:
                mimeType = "application/json";
                fileName+= ".json";
                break;
            case EXPORT_FILE_TYPE_CSV:
                mimeType = "text/csv";
                fileName+= ".csv";
                break;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

        // Filter to only show results that can be "opened", such as
        // a file (as opposed to a list of contacts or timezones).
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Create a file with the requested MIME type.
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, WRITE_REQUEST_CODE);
    }

    private void writeDocument(Uri uri) {

        JSONObject r = null;
        String data = "";
        try {
            r = mArrExportFiles.getJSONObject(0);
            data = r.getString("data");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (r == null){
            finish();
            return;
        }

        try {
            ParcelFileDescriptor doc = getContentResolver().openFileDescriptor(uri, "w");
            if (doc != null) {
                FileDescriptor desc = doc.getFileDescriptor();

                FileOutputStream fileOutputStream =
                        new FileOutputStream(desc);
                fileOutputStream.write(data.getBytes());
                // Let the document provider know you're done by closing the stream.
                fileOutputStream.close();
                doc.close();
            } else {
                finish();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mArrExportFiles.remove(0);
        if (mArrExportFiles.length() > 0) {
            createDocument();
        } else {
            finish();
        }
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

                Log.d("PPP", "RECEIVED: " + cmd + " WITH: " + dta);

                if (cmd.equals("beginExport")) {
                    beginExport();
                }

                if (cmd.equals("showExportTypeList")) {
                    showExportTypeList();
                }

                if (cmd.equals("createDocument")) {
                    createDocument();
                }
            }
        }
    };

}
