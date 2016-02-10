package com.marktreble.f3ftimer.exportimport;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.filesystem.SpreadsheetExport;

import java.util.ArrayList;

/**
 * Created by marktreble on 27/12/14.
 */
public class FileExportPilots extends BaseExport {

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
        exportPilotData();

        finish();
    }

    private void exportPilotData(){
        // Serialize all race data, pilots, times + groups
        String data = super.getSerialisedPilotData();
        Log.d("EXPORT DATA", data);

        new SpreadsheetExport().writeExportFile(mContext, data, "pilots.json", mSaveFolder);
    }
}
