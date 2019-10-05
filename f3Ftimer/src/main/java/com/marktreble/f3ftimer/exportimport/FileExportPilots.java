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

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.dialog.GenericRadioPicker;
import com.marktreble.f3ftimer.filesystem.FileExport;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by marktreble on 27/12/14.
 */
public class FileExportPilots extends BaseExport {

    // final static String TAG = "FileExportPilots";

    private Integer mExportFileType = -1;

    static final String DIALOG = "dialog";

    GenericRadioPicker mDLG3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {

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
                                        call("promptForSaveFolder", null);
                                    } else {
                                        mActivity.finish();
                                    }
                                    break;
                            }
                        }
                    }
            );

            mDLG3.show(getSupportFragmentManager(), DIALOG);
        } else {
            mExportFileType = savedInstanceState.getInt("mExportFileType");
            mSaveFolder = savedInstanceState.getString("mSaveFolder");
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("mExportFileType", mExportFileType);
        outState.putString("mSaveFolder", mSaveFolder);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    protected void beginExport() {
        showProgress(getString(R.string.exporting));

        exportPilotData();
        finish();
    }

    private void exportPilotData() {
        // Serialize all race data, pilots, times + groups

        switch (mExportFileType) {
            case EXPORT_FILE_TYPE_JSON:
                new FileExport().writeExportFile(mContext, super.getSerialisedPilotData(), "pilots.json", mSaveFolder);
                break;

            case EXPORT_FILE_TYPE_CSV:
                new FileExport().writeExportFile(mContext, super.getCSVPilotData(), "pilots.csv", mSaveFolder);
                break;
        }
    }
}
