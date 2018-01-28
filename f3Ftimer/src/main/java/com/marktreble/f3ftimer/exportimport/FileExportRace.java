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
public class FileExportRace extends BaseExport {

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
                        if (mExportFileType >0) {
                            mDlg.dismiss();
                        promptForSaveFolder();
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
                new SpreadsheetExport().writeExportFile(mContext, super.getJSONRaceData(r.id, r.round), r.name + ".json", mSaveFolder);
                break;

            case EXPORT_FILE_TYPE_CSV:
                new SpreadsheetExport().writeExportFile(mContext, super.getCSVRaceData(r.id, r.round), r.name + ".csv", mSaveFolder);
                break;
        }

    }
}
