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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import androidx.fragment.app.FragmentTransaction;
import android.util.Log;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.data.CountryCodes;
import com.marktreble.f3ftimer.dialog.GenericAlert;
import com.opencsv.CSVReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class FileImportPilots extends BaseImport {

    // final static String TAG = "FileImportPilots";

    static final String DIALOG = "dialog";

    GenericAlert mDLG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

            // Filter to only show results that can be "opened", such as a
            // file (as opposed to a list of contacts or timezones)
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            // Filter to show only images, using the image MIME data type.
            // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
            // To search for all documents available via installed storage providers,
            // it would be "*/*".
            intent.setType("*/*");
            String[] mimetypes = {"application/json", "text/csv"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
            startActivityForResult(intent, ACTION_PICK_FILE);

        }
    }

    protected int importFile(Uri uri, String type) {
        showProgress(getString(R.string.importing));

        if (type == null) return IMPORT_RESULT_WRONG_TYPE;

        if (type.equals("application/json")
                || type.equals("text/csv")) {
            String data = null;

            try {
                data = readFile(uri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (data == null) return IMPORT_RESULT_NO_PERMISSION;
            if (data.equals("")) return IMPORT_RESULT_NO_PERMISSION;

            final String fileData = data;
            switch (type) {
                case "application/json":
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            importPilotsJSON(fileData);
                        }
                    }, PROGRESS_DELAY);
                    break;
                case "text/csv":
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            importPilotsCSV(fileData);
                        }
                    }, PROGRESS_DELAY);
                    break;
            }
            return IMPORT_RESULT_SUCCESS;
        }
        return IMPORT_RESULT_WRONG_TYPE;
    }

    private void importPilotsCSV(String data) {
        JSONArray pilot_data = parsePilotsCSV(data);

        if (pilot_data != null) {
            importPilotsJSON(pilot_data.toString());
            mActivity.setResult(RESULT_OK);
            mActivity.finish();

        } else {

            String[] buttons_array = new String[1];
            buttons_array[0] = getString(android.R.string.cancel);

            mDLG = GenericAlert.newInstance(
                    getString(R.string.ttl_import_failed),
                    getString(R.string.msg_import_failed),
                    buttons_array,
                    new ResultReceiver(new Handler()) {
                        @Override
                        protected void onReceiveResult(int resultCode, Bundle resultData) {
                            super.onReceiveResult(resultCode, resultData);

                            mActivity.finish();
                        }
                    }
            );

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.addToBackStack(null);
            ft.add(mDLG, DIALOG);
            ft.commit();
        }
    }

    private JSONArray parsePilotsCSV(String data) {
        JSONArray pilot_data = new JSONArray();

        try {
            String tmpfile = "csv.txt";
            File file;
            try {
                file = File.createTempFile(tmpfile, null, mContext.getCacheDir());
                OutputStream os = new FileOutputStream(file);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(os);
                outputStreamWriter.write(data);
                outputStreamWriter.close();

                CountryCodes countryCodes = CountryCodes.sharedCountryCodes(mContext);

                CSVReader reader = new CSVReader(new FileReader(file.getAbsolutePath()));
                String[] fields;
                JSONObject pilot;
                while ((fields = reader.readNext()) != null) {
                    pilot = new JSONObject();
                    pilot.put("firstname", fields[0]);
                    pilot.put("lastname", fields[1]);
                    pilot.put("nationality", countryCodes.findIsoCountryCode(fields[2]));
                    pilot.put("language", fields[3]);
                    pilot.put("team", fields[4]);
                    pilot.put("frequency", fields[5]);
                    pilot.put("models", fields[6]);
                    pilot.put("email", fields[7]);
                    pilot_data.put(pilot);
                }
            } catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
                return null;
            }

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return pilot_data;
    }

    protected void importComplete() {
        call("pilotsImported", null);
    }
}
