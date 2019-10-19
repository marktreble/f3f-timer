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
import android.util.Log;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.dialog.GenericRadioPicker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by marktreble on 27/12/14.
 */
public class FileExportPilots extends BaseExport {

    // final static String TAG = "FileExportPilots";

    static final String DIALOG = "dialog";

    GenericRadioPicker mDLG3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            showExportTypeList();
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    protected void beginExport() {
        mArrExportFiles = new JSONArray();
        JSONObject o = new JSONObject();
        try {
            o.put("data", getPilotData(mExportFileType));
            o.put("name", "pilots");

            mArrExportFiles.put(o);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        createDocument();
    }

    private String getPilotData(int type) {
        String response = "";
        switch (type) {
            case EXPORT_FILE_TYPE_JSON:
                response = getSerialisedPilotData();
                break;
            case EXPORT_FILE_TYPE_CSV:
                response =  getCSVPilotData();
                break;
        }
        return response;
    }
}
