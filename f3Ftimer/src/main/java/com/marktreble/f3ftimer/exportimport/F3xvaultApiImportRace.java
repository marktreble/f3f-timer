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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.FragmentTransaction;
import android.text.format.DateFormat;
import android.util.Log;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.api.API;
import com.marktreble.f3ftimer.dialog.F3xvaultAPILoginActivity;
import com.marktreble.f3ftimer.dialog.GenericAlert;
import com.marktreble.f3ftimer.dialog.GenericListPicker;
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

// Get Race list: http://www.f3xvault.com/api.php?login=#####e&password=#####&function=searchEvents&event_type_code=f3f&show_future=1&date_from=2017-08-28
// Get Pilots: http://www.f3xvault.com/api.php?login=#####&password=#####&function=getEventPilots&event_id=?

public class F3xvaultApiImportRace extends BaseImport
        implements API.APICallbackInterface {

    private static final int DLG_LOGIN = 1;
    private int mRequestCode = 0;

    private API mAPITask = null;

    private String mUsername;
    private String mPassword;

    private ArrayList<String> mAvailableRaceIds;

    static final String DIALOG = "dialog";

    GenericAlert mDLG;
    GenericListPicker mDLG2;

    String mDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show source/auth dialog
        if (savedInstanceState == null) {
            Intent intent = new Intent(mActivity, F3xvaultAPILoginActivity.class);
            mRequestCode = DLG_LOGIN;
            mStartForResult.launch(intent);
            //startActivityForResult(intent, DLG_LOGIN);
        }
    }

    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                if (mRequestCode == DLG_LOGIN) {
                    Intent data = result.getData();
                    assert data != null;
                    final String username = data.getStringExtra("username");
                    final String password = data.getStringExtra("password");

                    showProgress(getString(R.string.connecting_to_server));

                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                        mDataSource = "https://www.f3xvault.com/api.php";
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
                        mAPITask.mCallback = F3xvaultApiImportRace.this;
                        mAPITask.request = API.F3XV_IMPORT;
                        mAPITask.mAppendEndpoint = false;
                        mAPITask.mIsJSON = false;
                        mAPITask.makeAPICall(F3xvaultApiImportRace.this, mDataSource, API.httpMethod.GET, params);
                        }
                    }, PROGRESS_DELAY);

                }
            } else {
                finish();
            }
        });

    public void onAPISuccess(String request, JSONObject data) {
        mAPITask = null;
        hideProgress();

        Log.d("f3xv", request + " : " + API.F3XV_IMPORT);
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
                    String[] fields;
                    while ((fields = reader.readNext()) != null) {
                        String id = fields[0];
                        String name = fields[2];
                        JSONObject race = new JSONObject();
                        race.put("id", id);
                        race.put("name", name);
                        race_list.put(race);
                    }
                } catch (IOException e) {
                    Log.e("Exception", "File write failed: " + e);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.i("f3xv", race_list.toString());

            if (race_list.length() > 0) {
                showRaceNamesDialog(race_list);
            } else {

                String[] buttons_array = new String[1];
                buttons_array[0] = getString(android.R.string.cancel);

                mDLG = GenericAlert.newInstance(
                        getString(R.string.ttl_no_races_available),
                        getString(R.string.msg_no_races_available),
                        buttons_array,
                        new ResultReceiver(new Handler(Looper.getMainLooper())) {
                            @Override
                            protected void onReceiveResult(int resultCode, Bundle resultData) {
                                super.onReceiveResult(resultCode, resultData);

                                mActivity.finish();
                            }
                        }
                );

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.addToBackStack(null);
                ft.add(mDLG, DIALOG);
                ft.commit();

            }
        }

        if (request.equals(API.F3XV_IMPORT_RACE)) {
            JSONObject race_data = null;
            try {
                String csvdata = data.getString("data");
                race_data = parseRaceCSV(csvdata);

            } catch (JSONException e) {
                e.printStackTrace();
            }


            if (race_data != null) {
                super.importRaceJSON(race_data.toString());
                mActivity.setResult(RESULT_OK);
                mActivity.finish();

            } else {
                String[] buttons_array = new String[1];
                buttons_array[0] = getString(android.R.string.cancel);

                mDLG = GenericAlert.newInstance(
                        getString(R.string.ttl_import_failed),
                        getString(R.string.msg_import_failed),
                        buttons_array,
                        new ResultReceiver(new Handler(Looper.getMainLooper())) {
                            @Override
                            protected void onReceiveResult(int resultCode, Bundle resultData) {
                                super.onReceiveResult(resultCode, resultData);

                                mActivity.finish();
                            }
                        }
                );

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.add(mDLG, DIALOG);
                ft.commit();

            }
        }

    }

    public void onAPIError(String request, JSONObject data) {
        mAPITask = null;
        hideProgress();
        Log.e("f3xv", request + " : " + API.F3XV_IMPORT);
        Log.e("f3xv", request + " : " + data);

        if (data == null) {
            String[] buttons_array = new String[1];
            buttons_array[0] = getString(android.R.string.cancel);

            mDLG = GenericAlert.newInstance(
                    getString(R.string.ttl_network_error),
                    getString(R.string.msg_network_error),
                    buttons_array,
                    new ResultReceiver(new Handler(Looper.getMainLooper())) {
                        @Override
                        protected void onReceiveResult(int resultCode, Bundle resultData) {
                            super.onReceiveResult(resultCode, resultData);

                            mActivity.finish();
                        }
                    }
            );

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.addToBackStack(null);
            ft.add(mDLG, DIALOG);
            ft.commit();

            return;
        }
        String message = null;
        try {
            message = data.getString("data");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (message != null) {

            Intent intent = new Intent(mActivity, F3xvaultAPILoginActivity.class);
            mRequestCode = DLG_LOGIN;
            mStartForResult.launch(intent);
            //startActivityForResult(intent, DLG_LOGIN);
        }
    }

    private void showRaceNamesDialog(JSONArray racenames) {
        mAvailableRaceIds = new ArrayList<>();
        ArrayList<String> racelist = new ArrayList<>();
        for (int i = 0; i < racenames.length(); i++) {
            JSONObject r = racenames.optJSONObject(i);
            String id = r.optString("id");
            String name = r.optString("name");
            mAvailableRaceIds.add(id);
            racelist.add(name);
        }

        String[] buttons_array = new String[1];
        buttons_array[0] = getString(android.R.string.cancel);

        mDLG2 = GenericListPicker.newInstance(
                getString(R.string.ttl_select_race_import),
                racelist,
                buttons_array,
                new ResultReceiver(new Handler(Looper.getMainLooper())) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        super.onReceiveResult(resultCode, resultData);
                        Log.d("f3xv", "RESULT = " + resultCode);
                        if (resultCode == 0) {
                            mDLG2.dismiss();
                            mActivity.finish();
                        } else if (resultCode >= 100) {
                            raceClicked(resultCode - 100);
                        }
                    }
                }
        );

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(mDLG2, DIALOG);
        ft.commit();

    }

    public void raceClicked(final int which) {
        if (which < 0 || which > mAvailableRaceIds.size()) {
            mDLG2.dismiss();
            mActivity.finish();
        }
        mDLG2.dismiss();

        showProgress(getString(R.string.downloading_race));

        new Handler(Looper.getMainLooper()).postDelayed(() -> downloadRace(mAvailableRaceIds.get(which)), PROGRESS_DELAY);

    }

    public void downloadRace(String id) {

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
        mAPITask.makeAPICall(this, mDataSource, API.httpMethod.GET, params);
    }
}
