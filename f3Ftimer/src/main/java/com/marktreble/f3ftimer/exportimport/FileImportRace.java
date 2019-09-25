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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.marktreble.f3ftimer.R;
import com.nononsenseapps.filepicker.FilePickerActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by marktreble on 27/12/14.
 */
public class FileImportRace extends BaseImport {

    // final static String TAG = "FileImportRace";

    private static final int ACTION_PICK_FILE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Intent i = new Intent(mContext, FilteredFilePickerActivity.class);
        // This works if you defined the intent filter
        // Intent i = new Intent(Intent.ACTION_GET_CONTENT);

        // Set these depending on your use case. These are the defaults.
        i.putExtra(FilteredFilePickerActivity.EXTRA_ALLOW_MULTIPLE, true);
        i.putExtra(FilteredFilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        i.putExtra(FilteredFilePickerActivity.EXTRA_MODE, FilteredFilePickerActivity.MODE_FILE);

        // Configure initial directory by specifying a String.
        // You could specify a String like "/storage/emulated/0/", but that can
        // dangerous. Always use Android's API calls to get paths to the SD-card or
        // internal memory.
        i.putExtra(FilteredFilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

        startActivityForResult(i, ACTION_PICK_FILE);

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTION_PICK_FILE && resultCode == Activity.RESULT_OK) {
            if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
                // Multiple files import
                StringBuilder failures = new StringBuilder();
                int successes = 0;
                // For JellyBean and above
                ClipData clip = data.getClipData();

                if (clip != null) {
                    for (int i = 0; i < clip.getItemCount(); i++) {
                        Uri uri = clip.getItemAt(i).getUri();
                        // Do something with the URI
                        boolean success = importFile(uri);

                        if (!success) {
                            String filename = uri.toString();
                            filename = filename.substring(filename.lastIndexOf("/"));
                            failures.append(filename);
                            failures.append(", ");
                        } else {
                            successes++;
                        }
                    }
                }

                if (successes > 0) setResult(RESULT_OK);

                String strFailures = failures.toString();
                if (strFailures.equals("")) {
                    finish();
                } else {
                    strFailures = strFailures.substring(0, strFailures.length() - 2);
                    new AlertDialog.Builder(mContext, R.style.FilePickerAlertDialogTheme)
                            .setTitle("Wrong file types")
                            .setMessage("Sorry, f3f timer can only import files in 'json' format. Some of your chosen files failed to import (" + strFailures + ")")
                            .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();

                                }
                            })
                            .show();

                }

            } else {
                Log.d("FILEIMPORT", "SINGLE");
                Uri uri = data.getData();
                if (uri == null) return;

                // Do something with the URI
                boolean success = importFile(uri);

                if (success) {
                    mActivity.setResult(RESULT_OK);
                    mActivity.finish();

                } else {
                    String filename = uri.toString();
                    String[] parts = filename.split("\\.");
                    String extension = parts[parts.length - 1];

                    new AlertDialog.Builder(mContext, R.style.AppTheme)
                            .setTitle("Wrong file type (." + extension + ")")
                            .setMessage("Sorry, f3f timer can only import files in 'json' format")
                            .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                    mActivity.finish();
                                }
                            })
                            .show();
                }
            }
        } else {
            finish();
        }
    }

    private boolean importFile(Uri uri) {
        String filename = uri.toString();

        String[] parts = filename.split("\\.");
        String extension = parts[parts.length - 1];

        if (extension.equals("json")) {
            String data = readFile(uri);
            if (!data.equals("")) {
                super.importRaceJSON(data);
                return true;
            }
        }

        if (extension.equals("csv")) {
            String data = readFile(uri);
            if (!data.equals("")) {
                super.importRaceCSV(data);
                return true;
            }
        }
        return false;

    }

    private String readFile(Uri uri) {
        String path = uri.getPath();
        if (path == null) return "";

        File f = new File(path);
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "";
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int i;
        try {
            i = inputStream.read();
            while (i != -1) {
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteArrayOutputStream.toString();
    }
}
