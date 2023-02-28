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

import androidx.annotation.NonNull;

import com.marktreble.f3ftimer.dialog.GenericRadioPicker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by marktreble on 27/12/14.
 */
public class FileExportPilots extends BaseExport {

    // final static String TAG = "FileExportPilots";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            showExportTypeList();
        }
    }

    public void onSaveInstanceState(@NonNull Bundle outState) {
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
