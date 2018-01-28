package com.marktreble.f3ftimer.exportimport;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.marktreble.f3ftimer.data.data.CountryCodes;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.pilot.PilotData;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by marktreble on 09/12/2015.
 */
public class BaseImport extends Activity {

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
                datasource.setGroups(race_id, i + 1, num_groups);
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

                    Log.i("BT", String.format("FLOWN == %d", flown));
                    if (flown == 1) {
                        Log.i("BT", String.format("RACE %d, PILOT %d, ROUND %d, TIME %s", race_id, pilot_id, i+1, time));
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

    protected void importRaceJSONExt(String data){
        // Parse json and add to database
        Log.i("IMPORT", "JSON RACE DATA: "+ data);

        try {
            JSONObject racedata = new JSONObject(data);
            JSONObject race_json = racedata.optJSONObject("race");
            JSONArray racepilots_json = racedata.optJSONArray("racepilots");
            JSONArray racetimes_json = racedata.optJSONArray("racetimes");
            JSONArray racegroups_json = racedata.optJSONArray("racegroups");

            RaceData datasource1 = new RaceData(mContext);
            datasource1.open();
            // Import Race
            Race race = new Race(race_json);
            int race_id = (int)datasource1.saveRace(race);

            // Import Groups
            for (int i=0; i<racegroups_json.length(); i++){
                Object val = racegroups_json.get(i);
                int num_groups = 1;
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
                }
                datasource1.setGroups(race_id, i + 1, num_groups);
            }
            datasource1.close();

            RacePilotData datasource2 = new RacePilotData(mContext);
            datasource2.open();

            // Import Pilots
            ArrayList<Integer> pilot_new_ids = new ArrayList<>();
            for (int i=0; i<racepilots_json.length(); i++){
                JSONObject p = racepilots_json.optJSONObject(i);
                Pilot pilot = new Pilot(p);
                pilot.id = pilot.pilot_id;
                int new_id = (int)datasource2.addPilot(pilot, race_id);
                pilot_new_ids.add(new_id);
            }

            // Import Race Pilot times
            for (int i=0; i<racetimes_json.length(); i++){
                JSONArray rounds_json = racetimes_json.optJSONArray(i);
                for (int j=0; rounds_json != null && j<rounds_json.length(); j++) {
                    JSONArray pilots_json = rounds_json.optJSONArray(j);
                    for (int k=0; k<pilots_json.length(); k++) {
                        JSONObject pilot_json = pilots_json.optJSONObject(k);
                        int p_id = pilot_json.optInt("id");
                        p_id = pilot_new_ids.get(p_id - 1);
                        Pilot p = datasource2.getPilot(p_id, race_id);
                        p.race_id = race_id;
                        p.round = i+1;
                        p.group = pilot_json.optInt("group");
                        p.start_pos = pilot_json.optInt("start_pos");
                        p.status = pilot_json.optInt("status");
                        p.time = (float)pilot_json.optDouble("time");
                        p.penalty = pilot_json.optInt("penalty");
                        p.points = (float)pilot_json.optDouble("points");
                        datasource2.addRaceTime(p);
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
        // TODO
        // Should use openCSV for parsing, and is not currently compliant with the database structure
        /*
        Log.i("IMPORT", "CSV RACE DATA: "+ data);
        try {
            String[] lines = data.split("\r\n|\n");
            RaceData datasource1 = new RaceData(mContext);
            datasource1.open();

            int colMode = lines[0].split(";")[0].equals("results") ? 5 : 2;

            String racename = lines[0].split(";")[1];
            Race race = datasource1.getRace(racename);
            if (race == null) {
                race = new Race();
                race.name = racename;
                datasource1.saveRace(race);
                race = datasource1.getRace(racename);
            }
            int race_id = race.id;

            RacePilotData datasource2 = new RacePilotData(mContext);
            datasource2.open();

            PilotData datasource3 = new PilotData(mContext);
            datasource3.open();

            int group_count = 0;

            for (int i = 2; i < lines.length; i++) {
                String[] values = lines[i].split(";");
                int pilot_id = Integer.parseInt(values[0]);
                Pilot p = datasource3.getPilot(pilot_id);
                int round = 1;
                for (int j = 1; j < values.length; j+=colMode) {
                    int group = Integer.parseInt(values[j]);
                    int start_pos = Integer.parseInt(values[j + 1].trim());
                    p.status = Pilot.STATUS_NORMAL;
                    p.group = group;
                    if (p.group > group_count) group_count = p.group;
                    p.start_pos = start_pos;
                    p.round = round++;
                    if (colMode > 2) {
                        p.time = Float.parseFloat(values[j + 2].trim());
                        p.penalty = Integer.parseInt(values[j + 3].trim());
                        p.points = Float.parseFloat(values[j + 4].trim());
                    }
                    datasource2.importPilot(p, race_id);
                }
                datasource1.setGroups(race_id, i - 1, group_count);
            }
            datasource1.close();
            datasource2.close();
            datasource3.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        */
    }

    protected void importPilotsCSV(String data){
        //TODO
        // Should use openCSV for parsing

        Log.i("IMPORT", "CSV PILOTS DATA: "+ data);
        try {
            PilotData datasource = new PilotData(mContext);
            datasource.open();

            CountryCodes countryCodes = CountryCodes.sharedCountryCodes(mContext);

            String[] lines = data.split("\r\n|\n");
            for (int i = 1; i < lines.length; i++) {
                String[] values = lines[i].split(";");
                Pilot pilot = new Pilot();
                if (values.length>0) pilot.id = Integer.parseInt(values[0]);
                if (values.length>1) pilot.firstname = values[1];
                if (values.length>2) pilot.lastname = values[2];
                if (values.length>3) {
                    pilot.nationality = countryCodes.findIsoCountryCode(values[3]);
                }
                if (values.length>4) pilot.language = values[4];
                if (values.length>5) pilot.team = values[5];
                if (values.length>6) pilot.frequency = values[6];
                if (values.length>7) pilot.models = values[7];
                if (values.length>8) pilot.email = values[8];
                datasource.savePilot(pilot);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
