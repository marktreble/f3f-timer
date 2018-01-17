package com.marktreble.f3ftimer.exportimport;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import com.marktreble.f3ftimer.data.data.CountryCodes;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.pilot.PilotData;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;
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

/**
 * Created by marktreble on 09/12/2015.
 * Base class for data imports
 */
public abstract class BaseImport extends Activity {

    Context mContext;
    public Activity mActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mContext = this;
        mActivity = this;
    }

    protected void importRaceJSON(String data){
        // Parse json and add to database
        Log.i("IMPORT", "DATA: "+ data);

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
            String oname =r.name;
            for (int i=0; i<allRaces.size(); i++){
                Race rcheck = allRaces.get(i);
                if (rcheck.name.equals(r.name)){
                    r.name = String.format("%s (%d)", oname, count++);

                    i = -1;
                }
            }
            int race_id = (int)datasource.saveRace(r);

            // Import Groups
            for (int i=0; i<racegroups.length(); i++){
                Object val = racegroups.get(i);
                int num_groups = 1, start_pilot = 1;
                // Backwards compat check for older version files
                // (Groups were an array of integers without the start pilot recorded)
                if(val instanceof Integer){
                    num_groups = (Integer)val;
                }

                // Format #2
                // Groups are a json object - keys: group (int), start_pilot (int)
                if(val instanceof JSONObject) {
                    JSONObject roundgroup = (JSONObject)val;
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

            for (int i=0; i<racepilots.length(); i++){
                JSONObject p = racepilots.optJSONObject(i);
                Pilot pilot = new Pilot(p);
                int new_id = (int)datasource2.addPilot(pilot, race_id);
                pilot_new_ids.add(new_id);
                Log.i("BT", pilot.toString());
            }
            // Import Times
            for (int i=0; i<racetimes.length(); i++){
                JSONArray roundtimes = racetimes.optJSONArray(i);
                for (int j=0;j<roundtimes.length(); j++) {
                    int pilot_id = pilot_new_ids.get(j);

                    JSONObject pilottime = roundtimes.optJSONObject(j);

                    float time = Float.parseFloat(pilottime.optString("time"));
                    int flown = pilottime.optInt("flown");
                    int penalty = pilottime.optInt("penalty");

                    if (flown == Pilot.STATUS_FLOWN
                            || flown == Pilot.STATUS_NORMAL) {
                        datasource2.setPilotTimeInRound(race_id, pilot_id, i+1, time);
                        if (penalty>0)
                            datasource2.setPenalty(race_id, pilot_id, i+1, penalty);


                    }


                }
            }

            datasource2.close();

        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    protected void importPilotsJSON(String data){
        try {
            JSONArray pilots = new JSONArray(data);

            PilotData datasource = new PilotData(mContext);
            datasource.open();

            for (int i = 0; i < pilots.length(); i++) {
                JSONObject p = pilots.optJSONObject(i);
                Pilot pilot = new Pilot(p);
                datasource.savePilot(pilot);
            }
        } catch (JSONException e){
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
            new AlertDialog.Builder(mContext)
                    .setTitle("Import Failed")
                    .setMessage("Sorry, something went wrong!")
                    .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mActivity.finish();
                        }
                    })
                    .show();
        }
    }

    protected JSONObject parseRaceCSV(String data){
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
                String [] fields;
                JSONObject pilot;
                while ((fields = reader.readNext()) != null) {
                    switch (line_no++) {
                        case 0:
                            // Race Data
                            race.put("name", fields[1]);
                            race.put("type", "1");
                            race.put("offset", "0");
                            race.put("status", "0");
                            race.put("round", "1");
                            race.put("rounds_per_flight", "1");
                            race.put("start_number", "1");
                            race.put("race_id", fields[0]);
                            break;
                        case 1:
                            // Pilot headers - ignore
                            break;
                        default:
                            // Pilots
                            int pilot_bib_number = Integer.parseInt(fields[1]);
                            while (bib_no++<pilot_bib_number && bib_no<200){
                                pilot = new JSONObject();
                                race_pilots.put(pilot);

                            }
                            pilot = new JSONObject();
                            pilot.put("pilot_id", fields[0]);
                            pilot.put("status", "1");
                            pilot.put("firstname", fields[2]);
                            pilot.put("lastname", fields[3]);
                            pilot.put("email", "");
                            pilot.put("frequency", "");
                            pilot.put("models", "");
                            pilot.put("nationality", "");
                            pilot.put("language", "");
                            pilot.put("team", fields[7]);
                            race_pilots.put(pilot);
                            break;

                    }
                }
                race_data.put("race", race);
                race_data.put("racepilots", race_pilots);
                race_data.put("racetimes", new JSONArray());
                race_data.put("racegroups", new JSONArray());

            }
            catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
                return null;
            }

        } catch (JSONException  e) {
            e.printStackTrace();
            return null;
        }

        return race_data;
    }

    protected void importPilotsCSV(String data) {
        //TODO
        // Should use openCSV for parsing
        JSONArray pilot_data = parsePilotsCSV(data);

        if (pilot_data != null) {
            importPilotsJSON(pilot_data.toString());
            mActivity.setResult(RESULT_OK);
            mActivity.finish();

        } else {
            new AlertDialog.Builder(mContext)
                    .setTitle("Import Failed")
                    .setMessage("Sorry, something went wrong!")
                    .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mActivity.finish();
                        }
                    })
                    .show();
        }

    }

    protected JSONArray parsePilotsCSV(String data){
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
                String [] fields;
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
            }
            catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
                return null;
            }

        } catch (JSONException  e) {
            e.printStackTrace();
            return null;
        }

        return pilot_data;
    }

}
