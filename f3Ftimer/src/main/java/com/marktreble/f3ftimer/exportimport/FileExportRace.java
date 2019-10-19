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

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.dialog.GenericCheckboxPicker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class FileExportRace extends BaseExport {

    // final static String TAG = "FileExportRace";

    ArrayList<String> mArrNames;
    ArrayList<Integer> mArrIds;
    ArrayList<Integer> mArrRounds;


    String[] _options;    // String array of all races in database
    boolean[] _selections;    // bool array of which has been selected

    static final String DIALOG = "dialog";

    GenericCheckboxPicker mDLG4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            showRaceList();
        } else {
            _options = savedInstanceState.getStringArray("options");
            _selections = savedInstanceState.getBooleanArray("selections");

            mArrNames = savedInstanceState.getStringArrayList("names");
            mArrIds = savedInstanceState.getIntegerArrayList("ids");
            mArrRounds = savedInstanceState.getIntegerArrayList("rounds");

            int selectedCount = 0;
            for (boolean selection: _selections) {
                if (selection) selectedCount++;
            }
            Log.d("PPP", "RELOAD SELECTIONS CNT: " + selectedCount);
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putStringArray("options", _options);
        outState.putBooleanArray("selections", _selections);

        outState.putStringArrayList("names", mArrNames);
        outState.putIntegerArrayList("ids", mArrIds);
        outState.putIntegerArrayList("rounds", mArrRounds);

        int selectedCount = 0;
        for (boolean selection: _selections) {
            if (selection) selectedCount++;
        }
        Log.d("PPP", "SAVE SELECTIONS CNT: " + selectedCount);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        mDLG4.dismiss();
    }

    private void showRaceList() {

        RaceData datasource = new RaceData(this);
        datasource.open();
        ArrayList<Race> allRaces = datasource.getAllRaces();
        datasource.close();

        mArrNames = new ArrayList<>();
        mArrIds = new ArrayList<>();
        mArrRounds = new ArrayList<>();

        for (Race r : allRaces) {
            mArrNames.add(r.name);
            mArrIds.add(r.id);
            mArrRounds.add(r.round);
        }

        _options = new String[mArrNames.size()];
        _options = mArrNames.toArray(_options);
        _selections = new boolean[_options.length];

        String[] buttons_array = new String[2];
        buttons_array[0] = getString(android.R.string.cancel);
        buttons_array[1] = getString(android.R.string.ok);

        mDLG4 = GenericCheckboxPicker.newInstance(
                getString(R.string.ttl_select_race),
                mArrNames,
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
                                int selectedCount = 0;
                                if (resultData.containsKey("selected")) {
                                    _selections = resultData.getBooleanArray("selected");
                                    if (_selections != null) {
                                        for (boolean selection: _selections) {
                                            if (selection) selectedCount++;
                                        }
                                    }
                                }

                                if (selectedCount > 0) {
                                    call("showExportTypeList", null);
                                } else {
                                    // Nothing selected, so dismiss
                                    mActivity.finish();
                                }
                                break;
                        }
                    }
                }
        );

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(mDLG4, DIALOG);
        ft.commit();
    }

    @Override
    protected void beginExport() {
        mArrExportFiles = new JSONArray();
        for (int i = 0; i < _options.length; i++) {
            if (_selections[i]) {
                JSONObject o = new JSONObject();
                try {
                    o.put("data", getRaceData(mArrIds.get(i), mArrRounds.get(i), mExportFileType));
                    o.put("name", mArrNames.get(i));

                    mArrExportFiles.put(o);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        createDocument();
    }

    private String getRaceData(int id, int round, int type) {
        String response = "";
        switch (type) {
            case EXPORT_FILE_TYPE_JSON:
                response = getSerialisedRaceData(id, round);
                break;
            case EXPORT_FILE_TYPE_CSV:
                response =  getCSVRaceData(id, round);
                break;
        }
        return response;
    }
}
