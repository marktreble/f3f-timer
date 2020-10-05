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
import android.os.ResultReceiver;
import androidx.fragment.app.FragmentTransaction;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.api.API;
import com.marktreble.f3ftimer.dialog.F3fTimerAPILoginActivity;
import com.marktreble.f3ftimer.dialog.GenericAlert;
import com.marktreble.f3ftimer.dialog.GenericListPicker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class F3ftimerApiImportRace extends BaseImport
        implements API.APICallbackInterface {

    private static final int DLG_LOGIN = 1;

    private API mAPITask = null;

    private String mToken;

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
            Intent intent = new Intent(mActivity, F3fTimerAPILoginActivity.class);
            startActivityForResult(intent, DLG_LOGIN);
        }


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        if (requestCode == DLG_LOGIN) {
            if (resultCode == RESULT_OK) {
                mDataSource = data.getStringExtra("datasource");
                String username = data.getStringExtra("username");
                String password = data.getStringExtra("password");

                showProgress(getString(R.string.connecting_to_server));

                // Auto prepend the http if needed
                if (!mDataSource.substring(0, 7).equals("http://")
                        && !mDataSource.substring(0, 8).equals("https://")) {
                    mDataSource = "http://" + mDataSource;
                }

                Map<String, String> params = new HashMap<>();
                params.put(API.ENDPOINT_KEY, API.API_IMPORT);
                params.put("u", username);
                params.put("p", password);

                mAPITask = new API();
                mAPITask.mCallback = this;
                mAPITask.request = API.API_IMPORT;
                mAPITask.makeAPICall(this, mDataSource, API.httpmethod.POST, params);

            }
        }
    }

    public void onAPISuccess(String request, JSONObject data) {
        mAPITask = null;
        hideProgress();

        if (request.equals(API.API_IMPORT)) {
            String token = "";
            JSONArray race_list = new JSONArray();
            try {
                token = data.getString("token");
                race_list = data.getJSONArray("data");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (race_list.length() > 0) {
                mToken = token;
                showRaceNamesDialog(race_list);
            } else {
                String[] buttons_array = new String[1];
                buttons_array[0] = getString(android.R.string.cancel);

                mDLG = GenericAlert.newInstance(
                        getString(R.string.ttl_no_races_available),
                        getString(R.string.msg_no_races_available),
                        buttons_array,
                        new ResultReceiver(new Handler()) {
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

        if (request.equals(API.API_IMPORT_RACE)) {
            JSONObject race_data = null;
            try {
                race_data = data.getJSONObject("data");
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
                        new ResultReceiver(new Handler()) {
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

    }

    public void onAPIError(String request, JSONObject data) {
        mAPITask = null;
        hideProgress();

        if (data == null) {
            String[] buttons_array = new String[1];
            buttons_array[0] = getString(android.R.string.cancel);

            mDLG = GenericAlert.newInstance(
                    getString(R.string.ttl_network_error),
                    getString(R.string.msg_network_error),
                    buttons_array,
                    new ResultReceiver(new Handler()) {
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
        String message;
        try {
            message = data.getString("data");

            if (message.equals("LOGIN_FAILED")) {

                Intent intent = new Intent(mActivity, F3fTimerAPILoginActivity.class);
                startActivityForResult(intent, DLG_LOGIN);
            }
        } catch (JSONException e) {
            e.printStackTrace();
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
                new ResultReceiver(new Handler()) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        super.onReceiveResult(resultCode, resultData);
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

        showProgress(getString(R.string.downloading_race));

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                downloadRace(mAvailableRaceIds.get(which));
            }
        }, 1000);
    }

    public void downloadRace(String id) {
        Map<String, String> params = new HashMap<>();
        params.put(API.ENDPOINT_KEY, API.API_IMPORT_RACE);
        params.put("t", mToken);
        params.put("rid", id);

        mAPITask = new API();
        mAPITask.mCallback = this;
        mAPITask.request = API.API_IMPORT_RACE;
        mAPITask.makeAPICall(this, mDataSource, API.httpmethod.POST, params);
    }
}