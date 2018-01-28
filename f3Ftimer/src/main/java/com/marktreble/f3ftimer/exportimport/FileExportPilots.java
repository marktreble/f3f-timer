package com.marktreble.f3ftimer.exportimport;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.marktreble.f3ftimer.filesystem.SpreadsheetExport;

/**
 * Created by marktreble on 27/12/14.
 */
public class FileExportPilots extends BaseExport {

    private int mExportFileType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        promptForSaveFolder();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (mDlg != null) mDlg = null;
    }

    @Override
    protected void beginExport(){

        mDlg = new AlertDialog.Builder( this )
                .setTitle( "Select file type" )
                .setSingleChoiceItems(filetypes, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mExportFileType = which;
                    }
                })
                .setPositiveButton( "OK", new DialogInterface.OnClickListener() {
                    public void onClick( DialogInterface dialog, int clicked )
                    {
                        if (mExportFileType >= 0) {
                            mDlg.dismiss();
                            exportPilotData();

                            finish();
                        }
                    }
                } )
                .show();
    }

    private void exportPilotData(){
        // Serialize all race data, pilots, times + groups

        switch (mExportFileType) {
            case EXPORT_FILE_TYPE_JSON:
                new SpreadsheetExport().writeExportFile(mContext, super.getJSONPilotData(), "pilots.json", mSaveFolder);
                break;
            case EXPORT_FILE_TYPE_CSV:
                new SpreadsheetExport().writeExportFile(mContext, super.getCSVPilotData(), "pilots.csv", mSaveFolder);
                break;
        }
    }
}
