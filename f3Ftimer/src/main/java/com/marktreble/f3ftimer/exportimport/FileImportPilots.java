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
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.dialog.GenericAlert;
import com.nononsenseapps.filepicker.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileImportPilots extends BaseImport {

    // final static String TAG = "FileImportPilots";

    private static final int ACTION_PICK_FILE = 1;

    static final String DIALOG = "dialog";

    GenericAlert mDLG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
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
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTION_PICK_FILE && resultCode == Activity.RESULT_OK) {
            if (data.getBooleanExtra(FilteredFilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
                // Multiple files import
                StringBuilder failures = new StringBuilder();
                int successes = 0;
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

                    String[] buttons_array = new String[1];
                    buttons_array[0] = getString(android.R.string.cancel);

                    mDLG = GenericAlert.newInstance(
                            getString(R.string.err_wrong_file_type),
                            String.format(getString(R.string.msg_wrong_file_type), strFailures),
                            buttons_array,
                            new ResultReceiver(new Handler()) {
                                @Override
                                protected void onReceiveResult(int resultCode, Bundle resultData) {
                                    super.onReceiveResult(resultCode, resultData);

                                    if (resultCode == 0) {
                                        mActivity.finish();
                                    }
                                }
                            }
                    );

                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.addToBackStack(null);
                    ft.add(mDLG, DIALOG);
                    ft.commit();


                }

            } else {
                Log.d("FILEIMPORT", "SINGLE");
                Uri uri = data.getData();

                if (uri == null) return;

                // Do something with the URI
                boolean success = importFile(uri);

                if (success) {
                    setResult(RESULT_OK);
                    finish();

                } else {
                    String filename = uri.toString();
                    String[] parts = filename.split("\\.");
                    String extension = parts[parts.length - 1];

                    String[] buttons_array = new String[1];
                    buttons_array[0] = getString(android.R.string.cancel);

                    mDLG = GenericAlert.newInstance(
                            String.format(getString(R.string.err_wrong_file_type_s), extension),
                            getString(R.string.msg_wrong_file_type_s),
                            buttons_array,
                            new ResultReceiver(new Handler()) {
                                @Override
                                protected void onReceiveResult(int resultCode, Bundle resultData) {
                                    super.onReceiveResult(resultCode, resultData);

                                    if (resultCode == 0) {
                                        mActivity.finish();
                                    }
                                }
                            }
                    );

                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.addToBackStack(null);
                    ft.add(mDLG, DIALOG);
                    ft.commit();
                }
            }
        } else {
            finish();
        }
    }

    private boolean importFile(Uri uri) {
        showProgress(getString(R.string.importing));

        String filename = uri.toString();

        String[] parts = filename.split("\\.");
        String extension = parts[parts.length - 1];

        if (extension.equals("json")) {
            String data = readFile(uri);
            if (!data.equals("")) {
                super.importPilotsJSON(data);
                return true;
            }
        }

        if (extension.equals("csv")) {
            String data = readFile(uri);
            if (!data.equals("")) {
                super.importPilotsCSV(data);
                return true;
            }
        }
        return false;

    }

    private String readFile(Uri uri) {
        File f = Utils.getFileForUri(uri);

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
