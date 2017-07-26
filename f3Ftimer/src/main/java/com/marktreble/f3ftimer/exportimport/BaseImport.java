package com.marktreble.f3ftimer.exportimport;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

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
    Activity mActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mContext = this;
        mActivity = this;
    }

    protected void importRace(String data){
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

    protected void importPilots(String data){
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

}
