package com.marktreble.f3ftimer.exportimport;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;
import com.marktreble.f3ftimer.filesystem.FileExport;
import com.marktreble.f3ftimer.filesystem.SpreadsheetExport;
import com.nononsenseapps.filepicker.FilePickerActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by marktreble on 27/12/14.
 */
public class FileExportRace extends BaseExport {

    final static String TAG = "FileExportRace";

    ArrayList<String> mArrNames;
    ArrayList<Race> mArrRaces;

    String[] _options;  	// String array of all races in database
    boolean[] _selections;	// bool array of which has been selected

    private int mExportFileType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showRaceList();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (mDlg != null) mDlg = null;
    }


    
    private void showRaceList(){

        RaceData datasource = new RaceData(this);
        datasource.open();
        ArrayList<Race> allRaces = datasource.getAllRaces();
        datasource.close();

        mArrNames = new ArrayList<>();
        mArrRaces = new ArrayList<>();

        for (Race r : allRaces){
            mArrNames.add(r.name);
            mArrRaces.add(r);
        }

        _options = new String[mArrNames.size()];
        _options = mArrNames.toArray(_options);
        _selections = new boolean[ _options.length ];

        mDlg = new AlertDialog.Builder( this )
                .setTitle( "Select races to export" )
                .setMultiChoiceItems( _options, _selections, new DialogSelectionClickHandler() )
                .setPositiveButton( "OK", new DialogInterface.OnClickListener() {
                    public void onClick( DialogInterface dialog, int clicked )
                    {
                        Log.d("EXPORT", "ONCLICKPOSITOVE");
                        mDlg.dismiss();
                        showExportTypeList();
                    }
                } )
                .show();
    }

    private void showExportTypeList(){

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
                        if (mExportFileType >=0) {
                            mDlg.dismiss();
                            promptForSaveFolder(null);
                        }
                    }
                } )
                .show();
    }

    public class DialogSelectionClickHandler implements DialogInterface.OnMultiChoiceClickListener
    {
        public void onClick( DialogInterface dialog, int clicked, boolean selected )
        {
            _selections[clicked] = selected;
        }
    }

    @Override
    protected void beginExport(){
        for (int i = 0; i < _options.length; i++) {
            if (_selections[i]) {
                exportRaceData(mArrRaces.get(i));
            }
        }

        finish();
    }

    private void exportRaceData(Race r){
        // Serialize all race data, pilots, times + groups

        switch (mExportFileType){
            case EXPORT_FILE_TYPE_JSON:
                new FileExport().writeExportFile(mContext, super.getSerialisedRaceData(r.id, r.round), r.name + ".json", mSaveFolder);
                break;

            case EXPORT_FILE_TYPE_CSV:
                new FileExport().writeExportFile(mContext, super.getCSVRaceData(r.id, r.round), r.name + ".csv", mSaveFolder);
                break;
        }

    }
}
