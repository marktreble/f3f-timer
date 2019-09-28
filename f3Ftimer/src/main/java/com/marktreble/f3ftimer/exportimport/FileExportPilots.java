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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.filesystem.FileExport;

/**
 * Created by marktreble on 27/12/14.
 */
public class FileExportPilots extends BaseExport {

    // final static String TAG = "FileExportPilots";

    private int mExportFileType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDlg = new AlertDialog.Builder(this, R.style.AppTheme_AlertDialog)

                .setTitle("Select file type")
                .setSingleChoiceItems(filetypes, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mExportFileType = which;
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int clicked) {
                        if (mExportFileType >= 0) {
                            mDlg.dismiss();
                            promptForSaveFolder(null);

                        }
                    }
                })
                .show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDlg != null) mDlg = null;
    }


    @Override
    protected void beginExport() {
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
