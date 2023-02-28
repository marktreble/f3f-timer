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
import android.os.Looper;
import android.os.ResultReceiver;
import androidx.fragment.app.FragmentTransaction;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.dialog.GenericAlert;

import org.json.JSONObject;

import java.io.IOException;

public class FileImportRace extends BaseImport {

    // final static String TAG = "FileImportRace";

    static final String DIALOG = "dialog";

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
            mRequestCode = ACTION_PICK_FILE;
            mStartForResult.launch(intent);
            //startActivityForResult(intent, ACTION_PICK_FILE);

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
                    new Handler(Looper.getMainLooper()).postDelayed(() -> importRaceJSON(fileData), PROGRESS_DELAY);
                    break;
                case "text/csv":
                    new Handler(Looper.getMainLooper()).postDelayed(() -> importRaceCSV(fileData), PROGRESS_DELAY);
                    break;
            }
            return IMPORT_RESULT_SUCCESS;
        }
        return IMPORT_RESULT_WRONG_TYPE;

    }

    private void importRaceCSV(String data) {
        JSONObject race_data = parseRaceCSV(data);

        if (race_data != null) {
            importRaceJSON(race_data.toString());
            mActivity.setResult(RESULT_OK);
            mActivity.finish();

        } else {

            String[] buttons_array = new String[1];
            buttons_array[0] = getString(android.R.string.cancel);

            mDLG = GenericAlert.newInstance(
                    getString(R.string.ttl_import_failed),
                    getString(R.string.msg_import_failed),
                    buttons_array,
                    new ResultReceiver(new Handler(Looper.getMainLooper())) {
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

    protected void importComplete() {
        call("raceImported", null);
    }
}
