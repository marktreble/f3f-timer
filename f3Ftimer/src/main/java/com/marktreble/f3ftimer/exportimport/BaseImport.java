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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.constants.IComm;
import com.marktreble.f3ftimer.data.data.CountryCodes;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public abstract class BaseImport extends AppCompatActivity {

    Context mContext;
    public Activity mActivity;

    static final String DIALOG = "dialog";

    GenericAlert mDLG;

    String mProgressMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.api);

        mContext = this;
        mActivity = this;

        if (savedInstanceState != null) {
            mProgressMessage = savedInstanceState.getString("progress_message");
            View progress = findViewById(R.id.progress);
            TextView progressLabel = progress.findViewById(R.id.progressLabel);
            progressLabel.setText(mProgressMessage);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("progress_message", mProgressMessage);
    }

    protected void showProgress(final String msg) {
        mProgressMessage = msg;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View progress = findViewById(R.id.progress);
                TextView progressLabel = progress.findViewById(R.id.progressLabel);
                progressLabel.setText(msg);
            }
        });

    }

    protected void hideProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View progress = findViewById(R.id.progress);
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

    protected void importRaceCSV(String data) {
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

    protected void importPilotsCSV(String data) {
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

    protected JSONArray parsePilotsCSV(String data) {
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

    protected void call(String func, @Nullable String data) {
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

                if (cmd.equals("connectionDenied")) {
                    connectionDenied(dta);
                }

            }
        }
    };

}
