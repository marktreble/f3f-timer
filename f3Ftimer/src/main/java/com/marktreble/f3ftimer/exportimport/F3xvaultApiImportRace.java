package com.marktreble.f3ftimer.exportimport;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.api.API;
import com.marktreble.f3ftimer.dialog.F3fTimerAPILoginActivity;
import com.marktreble.f3ftimer.dialog.F3xvaultAPILoginActivity;
import com.opencsv.CSVReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by marktreble on 27/08/2017.
 */


// Get Race list: http://www.f3xvault.com/api.php?login=#####e&password=#####&function=searchEvents&event_type_code=f3f&show_future=1&date_from=2017-08-28
// Get Pilots: http://www.f3xvault.com/api.php?login=#####&password=#####&function=getEventPilots&event_id=?

public class F3xvaultApiImportRace extends BaseImport
        implements API.APICallbackInterface {

    private static final int DLG_LOGIN = 1;

    private API mAPITask = null;

    private String mUsername;
    private String mPassword;

    private ArrayList<String> mAvailableRaceIds;

    AlertDialog mDlg;
    AlertDialog.Builder mDlgb;

    String mDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.api);

        // Show source/auth dialog

        Intent intent = new Intent(mActivity, F3xvaultAPILoginActivity.class);
        startActivityForResult(intent, DLG_LOGIN);


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == DLG_LOGIN) {
                String username = data.getStringExtra("username");
                String password = data.getStringExtra("password");

                showProgress("Connecting to Server..");

                mDataSource = "http://www.f3xvault.com/api.php";
                Map<String, String> params = new HashMap<>();
                params.put(API.ENDPOINT_KEY, API.F3XV_IMPORT);
                params.put("login", username);
                params.put("password", password);
                params.put("function", API.F3XV_IMPORT);
                params.put("event_type_code", "f3f");
                params.put("show_future", "1");
                params.put("date_from", DateFormat.format("yyyy-MM-dd", new Date()).toString());

                mUsername = username;
                mPassword = password;

                mAPITask = new API();
                mAPITask.mCallback = this;
                mAPITask.request = API.F3XV_IMPORT;
                mAPITask.mAppendEndpoint = false;
                mAPITask.mIsJSON = false;
                mAPITask.makeAPICall(this, mDataSource, API.httpmethod.GET, params);

            }
        } else {
            finish();
        }
    }

    public void showProgress(String msg){
        View progress = findViewById(R.id.progress);
        TextView progressLabel = (TextView)findViewById(R.id.progressLabel);

        progressLabel.setText(msg);
        progress.setVisibility(View.VISIBLE);
    }

    public void hideProgress(){
        View progress = findViewById(R.id.progress);
        progress.setVisibility(View.GONE);
    }

    public void onAPISuccess(String request, JSONObject data) {
        mAPITask = null;
        hideProgress();

        Log.i("f3xv", request+" : "+API.F3XV_IMPORT);
        if (request.equals(API.F3XV_IMPORT)) {
            JSONArray race_list = new JSONArray();

            try {
                String csvdata = data.getString("data");
                String tmpfile = "f3xvdata.txt";
                File file;
                try {
                    file = File.createTempFile(tmpfile, null, mContext.getCacheDir());
                    OutputStream os = new FileOutputStream(file);
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(os);
                    outputStreamWriter.write(csvdata);
                    outputStreamWriter.close();

                    CSVReader reader = new CSVReader(new FileReader(file.getAbsolutePath()));
                    String [] fields;
                    while ((fields = reader.readNext()) != null) {
                        String id = fields[0];
                        String name = fields[2];
                        JSONObject race = new JSONObject();
                        race.put("id", id);
                        race.put("name", name);
                        race_list.put(race);
                    }
                }
                catch (IOException e) {
                    Log.e("Exception", "File write failed: " + e.toString());
                }

            } catch (JSONException  e) {
                e.printStackTrace();
            }

            Log.i("f3xv", race_list.toString());

            if (race_list.length()>0) {
                showRaceNamesDialog(race_list);
            } else {
                new AlertDialog.Builder(mContext)
                        .setTitle("No Races Available")
                        .setMessage("No races are available for download at the moment.")
                        .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mActivity.finish();
                            }
                        })
                        .show();
            }
        }

        if (request.equals(API.F3XV_IMPORT_RACE)) {
            JSONObject race_data = new JSONObject();
            JSONObject race = new JSONObject();
            JSONArray race_pilots = new JSONArray();

            try {
                String csvdata = data.getString("data");
                String tmpfile = "f3xvdata.txt";
                File file;
                int line_no = 0;
                int bib_no = 1;
                try {
                    file = File.createTempFile(tmpfile, null, mContext.getCacheDir());
                    OutputStream os = new FileOutputStream(file);
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(os);
                    outputStreamWriter.write(csvdata);
                    outputStreamWriter.close();

                    CSVReader reader = new CSVReader(new FileReader(file.getAbsolutePath()));
                    String [] fields;
                    JSONObject pilot;
                    while ((fields = reader.readNext()) != null) {
                        switch (line_no++) {
                            case 0:
                                // Race Data
                                race.put("name", fields[1]);
                                race.put("type", "1");
                                race.put("offset", "0");
                                race.put("status", "0");
                                race.put("round", "1");
                                race.put("rounds_per_flight", "1");
                                race.put("start_number", "1");
                                race.put("race_id", fields[0]);
                                break;
                            case 1:
                                // Pilot headers - ignore
                                break;
                            default:
                                // Pilots
                                int pilot_bib_number = Integer.parseInt(fields[1]);
                                while (bib_no++<pilot_bib_number && bib_no<200){
                                    pilot = new JSONObject();
                                    race_pilots.put(pilot);

                                }
                                pilot = new JSONObject();
                                pilot.put("pilot_id", fields[0]);
                                pilot.put("status", "1");
                                pilot.put("firstname", fields[2]);
                                pilot.put("lastname", fields[3]);
                                pilot.put("email", "");
                                pilot.put("frequency", "");
                                pilot.put("models", "");
                                pilot.put("nationality", "");
                                pilot.put("language", "");
                                pilot.put("team", fields[7]);
                                race_pilots.put(pilot);
                                break;

                        }
                    }
                    race_data.put("race", race);
                    race_data.put("racepilots", race_pilots);
                    race_data.put("racetimes", new JSONArray());
                    race_data.put("racegroups", new JSONArray());

                }
                catch (IOException e) {
                    Log.e("Exception", "File write failed: " + e.toString());
                }

            } catch (JSONException  e) {
                e.printStackTrace();
            }


            if (race_data != null) {
                super.importRaceJSON(race_data.toString());
                mActivity.setResult(RESULT_OK);
                mActivity.finish();

            } else {
                new AlertDialog.Builder(mContext)
                        .setTitle("Import Failed")
                        .setMessage("Sorry, something went wrong!")
                        .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mActivity.finish();
                            }
                        })
                        .show();
            }
        }

    }

    public void onAPIError(String request, JSONObject data) {
        mAPITask = null;
        hideProgress();

        if (data == null){
            new AlertDialog.Builder(mContext)
                    .setTitle("Network Error")
                    .setMessage("Sorry, no response from server.")
                    .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mActivity.finish();
                        }
                    })
                    .show();
            return;
        }
        String message = null;
        try {
            message = data.getString("data");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (message != null){

            Intent intent = new Intent(mActivity, F3xvaultAPILoginActivity.class);
            startActivityForResult(intent, DLG_LOGIN);
        }
    }

    private void showRaceNamesDialog(JSONArray racenames){
        mAvailableRaceIds = new ArrayList<>();
        ArrayList<String> racelist = new ArrayList<>();
        for (int i = 0; i < racenames.length(); i++) {
            JSONObject r = racenames.optJSONObject(i);
            String id =r.optString("id");
            String name =r.optString("name");
            mAvailableRaceIds.add(id);
            racelist.add(name);
        }

        CharSequence[] list = racelist.toArray(new CharSequence[racelist.size()]);
        mDlgb = new AlertDialog.Builder(mContext)
                .setTitle("Races Available for Import")
                .setCancelable(true)
                .setItems(list, raceClickListener);

        mDlg = mDlgb.create();
        mDlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (mAPITask == null)
                    mActivity.finish();
            }
        });
        mDlg.show();
    }

    private final DialogInterface.OnClickListener raceClickListener = new DialogInterface.OnClickListener(){
        @Override
        public void onClick(DialogInterface dialog, int which) {
            downloadRace(mAvailableRaceIds.get(which));
            dialog.cancel();
        }
    };

    public void downloadRace(String id){
        Log.i("ONACTIVITY", "DOWNLOADING RACE");

        showProgress("Downloading Race..");

        Map<String, String> params = new HashMap<>();
        params.put(API.ENDPOINT_KEY, API.F3XV_IMPORT_RACE);
        params.put("login", mUsername);
        params.put("password", mPassword);
        params.put("function", API.F3XV_IMPORT_RACE);
        params.put("event_id", id);

        mAPITask = new API();
        mAPITask.mCallback = this;
        mAPITask.request = API.F3XV_IMPORT_RACE;
        mAPITask.mAppendEndpoint = false;
        mAPITask.mIsJSON = false;
        mAPITask.makeAPICall(this, mDataSource, API.httpmethod.GET, params);
    }
}
