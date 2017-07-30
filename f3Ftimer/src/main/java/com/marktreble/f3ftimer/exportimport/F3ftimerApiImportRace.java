package com.marktreble.f3ftimer.exportimport;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.api.API;
import com.marktreble.f3ftimer.dialog.F3fTimerAPILoginActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by marktreble on 04/09/2016.
 */

public class F3ftimerApiImportRace extends BaseImport
    implements API.APICallbackInterface {

    private static final int DLG_LOGIN = 1;

    private API mAPITask = null;

    private String mToken;

    private ArrayList<String> mAvailableRaceIds;

    AlertDialog mDlg;
    AlertDialog.Builder mDlgb;

    String mDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.api);

        // Show source/auth dialog

        Intent intent = new Intent(mActivity, F3fTimerAPILoginActivity.class);
        startActivityForResult(intent, DLG_LOGIN);

        Log.i("ONACTIVITY", mActivity.toString());

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == DLG_LOGIN) {
                mDataSource = data.getStringExtra("datasource");
                String username = data.getStringExtra("username");
                String password = data.getStringExtra("password");

                showProgress("Connecting to Server..");

                // Auto prepend the http if needed
                if (!mDataSource.substring(0,7).equals("http://")
                    && !mDataSource.substring(0,8).equals("https://")){
                    mDataSource = "http://"+mDataSource;
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

        if (request.equals(API.API_IMPORT)) {
            String token = "";
            JSONArray race_list = new JSONArray();
            try {
                token = data.getString("token");
                race_list = data.getJSONArray("data");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (race_list.length()>0) {
                mToken = token;
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

        if (request.equals(API.API_IMPORT_RACE)) {
            JSONObject race_data = null;
            try {
                race_data = data.getJSONObject("data");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (race_data != null) {
                super.importRace(race_data.toString());
                Log.i("ONACTIVITY", mActivity.toString());
                Log.i("ONACTIVITY", "RETURNING "+RESULT_OK);
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

        if (message.equals("LOGIN_FAILED")){

            Intent intent = new Intent(mActivity, F3fTimerAPILoginActivity.class);
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
        params.put(API.ENDPOINT_KEY, API.API_IMPORT_RACE);
        params.put("t", mToken);
        params.put("rid", id);

        mAPITask = new API();
        mAPITask.mCallback = this;
        mAPITask.request = API.API_IMPORT_RACE;
        mAPITask.makeAPICall(this, mDataSource, API.httpmethod.POST, params);
    }
}