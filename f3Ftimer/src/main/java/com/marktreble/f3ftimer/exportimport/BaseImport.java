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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.constants.IComm;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.pilot.PilotData;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;
import com.marktreble.f3ftimer.dialog.GenericAlert;
import com.opencsv.CSVReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Objects;

public abstract class BaseImport extends AppCompatActivity {

    Context mContext;
    public Activity mActivity;

    static final String DIALOG = "dialog";

    GenericAlert mDLG;

    String mProgressMessage;

    protected static final long PROGRESS_DELAY = 500;
    protected static final int ACTION_PICK_FILE = 1;

    protected static final int IMPORT_RESULT_CANCELLED = 0;
    protected static final int IMPORT_RESULT_WRONG_TYPE = 1;
    protected static final int IMPORT_RESULT_NO_PERMISSION = 2;
    protected static final int IMPORT_RESULT_SUCCESS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.api);

        mContext = this;
        mActivity = this;

        if (savedInstanceState != null) {
            mProgressMessage = savedInstanceState.getString("progress_message");
            if (mProgressMessage != null) {
                if (!mProgressMessage.equals("")) {
                    showProgress(mProgressMessage);
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("progress_message", mProgressMessage);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        if (requestCode == ACTION_PICK_FILE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        final String type = mContext.getContentResolver().getType(uri);
                        switch (importFile(uri, type)) {
                            case IMPORT_RESULT_WRONG_TYPE:
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        hideProgress();
                                        call("wrongFileType", type);
                                    }
                                }, PROGRESS_DELAY);
                                break;
                            case IMPORT_RESULT_NO_PERMISSION:
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        hideProgress();
                                        call("permissionDenied", type);
                                    }
                                }, PROGRESS_DELAY);
                                break;
                            case IMPORT_RESULT_SUCCESS:
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        hideProgress();
                                        importComplete();
                                    }
                                }, PROGRESS_DELAY);
                                break;

                        }
                    }
                }
            }
        }
    }

    protected void showProgress(final String msg) {
        mProgressMessage = msg;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View progress = findViewById(R.id.progress);
                progress.setVisibility(View.VISIBLE);
                TextView progressLabel = progress.findViewById(R.id.progressLabel);
                progressLabel.setText(msg);
            }
        });

    }

    protected void hideProgress() {
        mProgressMessage = "";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View progress = findViewById(R.id.progress);
                progress.setVisibility(View.GONE);
                TextView progressLabel = progress.findViewById(R.id.progressLabel);
                progressLabel.setText("");
            }
        });

    }

    public void onResume() {
        super.onResume();

        this.registerReceiver(onBroadcast, new IntentFilter(IComm.RCV_UPDATE));
    }

    public void onPause() {
        super.onPause();

        this.unregisterReceiver(onBroadcast);
    }

    protected int importFile(Uri uri, String type) {
        return IMPORT_RESULT_CANCELLED;
    }

    protected void importComplete() {
        // Virtual Function
    }

    protected String readFile(Uri uri) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream =
                     getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    protected void call(String func, @Nullable String data) {
        Log.d("PPP", "CALLING: " + func + " WITH: " + data);
        Intent i = new Intent(IComm.RCV_UPDATE);
        i.putExtra("cmd", func);
        i.putExtra("dta", data);
        sendBroadcast(i);
    }

    private void connectionDenied(String deviceName) {
        String[] buttons_array = new String[1];
        buttons_array[0] = getString(android.R.string.cancel);

        mDLG = GenericAlert.newInstance(
                getString(R.string.ttl_connection_denied),
                String.format(getString(R.string.msg_connection_denied), deviceName),
                buttons_array,
                new ResultReceiver(new Handler()) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        super.onReceiveResult(resultCode, resultData);

                        finish();
                    }
                }
        );

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.addToBackStack(null);
        ft.add(mDLG, DIALOG);
        ft.commit();
    }

    private void wrongFileType(String extension) {
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

    private void permissionDenied() {
        String[] buttons_array = new String[1];
        buttons_array[0] = getString(android.R.string.cancel);

        mDLG = GenericAlert.newInstance(
                getString(R.string.err_permission_denied),
                getString(R.string.msg_permission_denied),
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

    private void raceImported() {
        String[] buttons_array = new String[1];
        buttons_array[0] = getString(android.R.string.ok);

        mDLG = GenericAlert.newInstance(
                getString(R.string.ttl_race_imported),
                getString(R.string.msg_race_imported),
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

    private void pilotsImported() {
        String[] buttons_array = new String[1];
        buttons_array[0] = getString(android.R.string.ok);

        mDLG = GenericAlert.newInstance(
                getString(R.string.ttl_pilots_imported),
                getString(R.string.msg_pilots_imported),
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

    private BroadcastReceiver onBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("cmd")) {
                Bundle extras = intent.getExtras();
                if (extras == null) {
                    return;
                }
                String cmd = extras.getString("cmd", "");
                String dta = extras.getString("dta");

                Log.d("PPP", "RECEIVED: " + cmd + " WITH: " + dta);

                if (cmd.equals("connectionDenied")) {
                    connectionDenied(dta);
                }

                if (cmd.equals("wrongFileType")) {
                    wrongFileType(dta);
                }

                if (cmd.equals("permissionDenied")) {
                    permissionDenied();
                }

                if (cmd.equals("raceImported")) {
                    raceImported();
                }

                if (cmd.equals("pilotsImported")) {
                    pilotsImported();
                }

            }
        }
    };

    protected void importRaceJSON(String data) {
        // Parse json and add to database
        try {
            JSONObject racedata = new JSONObject(data);
            JSONObject race = racedata.optJSONObject("race");
            JSONArray racepilots = racedata.optJSONArray("racepilots");
            JSONArray racetimes = racedata.optJSONArray("racetimes");
            JSONArray racegroups = racedata.optJSONArray("racegroups");

            RaceData datasource = new RaceData(mContext);
            datasource.open();
            // Import Race
            Race r = new Race(race);
            // Check race name for conflicts
            ArrayList<Race> allRaces = datasource.getAllRaces();
            int count = 2;
            String oname = r.name;
            for (int i = 0; i < allRaces.size(); i++) {
                Race rcheck = allRaces.get(i);
                if (rcheck.name.equals(r.name)) {
                    r.name = String.format("%s (%d)", oname, count++);

                    i = -1;
                }
            }
            int race_id = (int) datasource.saveRace(r);

            // Import Groups
            for (int i = 0; i < racegroups.length(); i++) {
                Object val = racegroups.get(i);
                int num_groups = 1, start_pilot = 1;
                // Backwards compat check for older version files
                // (Groups were an array of integers without the start pilot recorded)
                if (val instanceof Integer) {
                    num_groups = (Integer) val;
                }

                // Format #2
                // Groups are a json object - keys: group (int), start_pilot (int)
                if (val instanceof JSONObject) {
                    JSONObject roundgroup = (JSONObject) val;
                    num_groups = roundgroup.getInt("groups");
                    start_pilot = roundgroup.getInt("start_pilot");
                }
                datasource.setGroups(race_id, i + 1, num_groups, start_pilot);
            }
            datasource.close();

            RacePilotData datasource2 = new RacePilotData(mContext);
            datasource2.open();
            // Import Pilots
            ArrayList<Integer> pilot_new_ids = new ArrayList<>();

            for (int i = 0; i < racepilots.length(); i++) {
                JSONObject p = racepilots.optJSONObject(i);
                Pilot pilot = new Pilot(p);
                int new_id = (int) datasource2.addPilot(pilot, race_id);
                pilot_new_ids.add(new_id);
                Log.i("BT", pilot.toString());
            }
            // Import Times
            for (int i = 0; i < racetimes.length(); i++) {
                JSONArray roundtimes = racetimes.optJSONArray(i);
                for (int j = 0; j < roundtimes.length(); j++) {
                    int pilot_id = pilot_new_ids.get(j);

                    JSONObject pilottime = roundtimes.optJSONObject(j);

                    float time = Float.parseFloat(pilottime.optString("time"));
                    int flown = pilottime.optInt("flown");
                    int penalty = pilottime.optInt("penalty");

                    if (flown == Pilot.STATUS_FLOWN
                            || flown == Pilot.STATUS_NORMAL) {
                        datasource2.setPilotTimeInRound(race_id, pilot_id, i + 1, time);
                        if (penalty > 0)
                            datasource2.setPenalty(race_id, pilot_id, i + 1, penalty);
                    }
                }
            }

            datasource2.close();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected JSONObject parseRaceCSV(String data) {
        JSONObject race_data = new JSONObject();
        JSONObject race = new JSONObject();
        JSONArray race_pilots = new JSONArray();

        try {
            String tmpfile = "csv.txt";
            File file;
            int line_no = 0;
            int bib_no = 1;
            try {
                file = File.createTempFile(tmpfile, null, mContext.getCacheDir());
                OutputStream os = new FileOutputStream(file);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(os);
                outputStreamWriter.write(data);
                outputStreamWriter.close();

                CSVReader reader = new CSVReader(new FileReader(file.getAbsolutePath()));
                String[] fields;
                JSONObject pilot;
                while ((fields = reader.readNext()) != null) {
                    switch (line_no++) {
                        case 0:
                            // Race Data
                            race.put("race_id", fields[0]);
                            race.put("name", fields[1]);
                            race.put("round", (fields.length < 8) ? "1" : fields[7]);
                            race.put("type", (fields.length < 9) ? "1" : fields[8]);
                            race.put("offset", (fields.length < 10) ? "0" : fields[9]);
                            race.put("status", (fields.length < 11) ? "0" : fields[10]);
                            race.put("rounds_per_flight", (fields.length < 12) ? "1" : fields[11]);
                            race.put("start_number", (fields.length < 13) ? "1" : fields[12]);
                            Log.d("PPP", race.toString());

                            break;
                        case 1:
                            // Pilot headers - ignore
                            break;
                        default:
                            if (fields.length >= 7) {
                                // Pilots
                                int pilot_bib_number = Integer.parseInt(fields[1]);
                                // Fill in gaps where bib numbers are missing
                                while (bib_no++ < pilot_bib_number && bib_no < 200) {
                                    pilot = new JSONObject();
                                    race_pilots.put(pilot);

                                }
                                pilot = new JSONObject();
                                pilot.put("pilot_id", fields[0]);
                                pilot.put("firstname", fields[2]);
                                pilot.put("lastname", fields[3]);
                                //pilot.put("class", fields[4]); // Not required
                                pilot.put("nac_no", fields[5]);
                                //pilot.put("fai_designation", fields[6]);
                                pilot.put("fai_id", fields[6]);
                                pilot.put("team", fields[8]);
                                pilot.put("status", (fields.length < 10) ? "1" : fields[9]);
                                pilot.put("email", (fields.length < 11) ? "" : fields[10]);
                                pilot.put("frequency", (fields.length < 12) ? "" : fields[11]);
                                pilot.put("models", (fields.length < 13) ? "" : fields[12]);
                                pilot.put("nationality", (fields.length < 14) ? "" : fields[13]);
                                pilot.put("language", (fields.length < 15) ? "" : fields[14]);
                                race_pilots.put(pilot);
                            }
                            break;

                    }
                }
                race_data.put("race", race);
                race_data.put("racepilots", race_pilots);

                // TODO add groups and times for full race data when imported from file
                race_data.put("racetimes", new JSONArray());
                race_data.put("racegroups", new JSONArray());

            } catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
                return null;
            }

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return race_data;
    }

    protected void importPilotsJSON(String data) {
        try {
            JSONArray pilots = new JSONArray(data);

            PilotData datasource = new PilotData(mContext);
            datasource.open();

            for (int i = 0; i < pilots.length(); i++) {
                JSONObject p = pilots.optJSONObject(i);
                Pilot pilot = new Pilot(p);
                datasource.savePilot(pilot);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
