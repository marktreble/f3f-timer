package com.marktreble.f3ftimer.filesystem;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by marktreble on 08/02/15.
 */
public class SpreadsheetExport {

    public void writeExportFile(Context context, String output, String filename){
        writeExportFile(context, output, filename, "");
    }

    public void writeExportFile(Context context, String output, String filename, String path){
        File file;
        if (path.equals("")) {
            file = this.getDataStorageDir(filename);
        } else {
            file = new File(path+String.format("/%s", filename));
        }
        Log.d("EXPORT", "WRITING FILE TO: "+file.getPath());

        if (file != null){
            FileOutputStream stream = null;
            try {
                stream = new FileOutputStream(file);
                try {
                    stream.write(output.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        stream.flush();
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            if (stream != null){
                try {
                    stream.flush();
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public void writeResultsFile(Context context, Race race){

        int MAX_ROUNDS = 10;

        if (race.round>10) MAX_ROUNDS = 20;
        if (race.round>20) MAX_ROUNDS = 30;

        // Update the race (.f3f) file
        if (this.isExternalStorageWritable()){

            int extra_discards = 0;
            RaceData datasource = new RaceData(context);
            datasource.open();

            RacePilotData datasource2 = new RacePilotData(context);
            datasource2.open();


            String data = "";

            ArrayList<Pilot> allPilots = datasource2.getAllPilotsForRace(race.id, 0, 0, 0);

            if (allPilots != null){
                ArrayList<int[]> p_penalty = new ArrayList<>();
                ArrayList<Integer> groups = new ArrayList<>();
                // Get penalties first
                for (int rnd=0; rnd<race.round; rnd++){
                    ArrayList<Pilot> pilots_in_round = datasource2.getAllPilotsForRace(race.id, rnd+1, 0, 0);
                    int[] penalty = new int[allPilots.size()];
                    for (int i=0; i<allPilots.size(); i++){
                        penalty[i] = (pilots_in_round.get(i).penalty != null) ? pilots_in_round.get(i).penalty : 0;
                    }
                    p_penalty.add(penalty);

                    groups.add(datasource.getGroups(race.id, rnd+1));
                }

                // Get all times for pilots in all rounds
                int num_pilots = allPilots.size();
                for (int i=0; i<num_pilots; i++) {
                    Pilot p = allPilots.get(i);
                    if (p.pilot_id > 0) { // Skipped bib no if pilot_id=0
                        int penalties_total = 0;
                        String penalties_rounds = "";

                        // Start new row (pilot name, frequency)
                        data += String.format("%s %s , %s", p.firstname, p.lastname, (p.frequency.equals("")) ? 0 : p.frequency);
                        // Rounds should always be MAX_ROUNDS
                        // Group scoring adds extra rounds and adds discards, so the extra discards must be deducted from the 30 rounds
                        for (int rnd = 0; rnd < MAX_ROUNDS - extra_discards; rnd++) {
                            // Set the correct pilot group from pilot position (i) and num groups (groups.get(rnd))
                            int num_groups = (rnd < race.round) ? groups.get(rnd) : 1;
                            extra_discards += num_groups - 1;
                            int group_size = (int) Math.floor(num_pilots / num_groups);
                            int remainder = num_pilots - (num_groups * group_size);
                            int pilot_group = 0, eog = 0;
                            for (int j = 1; j <= num_groups; j++) {
                                eog += (j * group_size) + ((remainder-- > 0) ? 1 : 0);
                                if (i < eog) {
                                    j = num_groups;
                                } else {
                                    pilot_group++;
                                }
                            }

                            for (int g = 0; g < num_groups; g++) {
                                if (g == pilot_group) {
                                    // Add pilot's time for round/group
                                    String time = "0";
                                    if (rnd < race.round) {
                                        float ftime = datasource2.getPilotTimeInRound(race.id, p.id, rnd + 1);
                                        if (ftime > 0)
                                            time = String.format("%.2f", ftime);
                                    }
                                    data += String.format(" , %s", time.replace(",", "."));

                                } else {
                                    // Not in this group - add 0 (discard)
                                    data += " , 0";
                                }
                                // Add up penalties, and record the rounds string
                                if (rnd < race.round) {
                                    int num_penalties = p_penalty.get(rnd)[i];
                                    if (num_penalties > 0) {
                                        penalties_total += num_penalties * 100;
                                        for (int j = 0; j < num_penalties; j++) {
                                            penalties_rounds += String.format("%02d", rnd + 1);
                                        }
                                    }
                                }
                            }
                        }
                        if (penalties_rounds.equals("")) penalties_rounds = "0";
                        data += String.format(" , %d , 0 , %s\r\n", penalties_total, penalties_rounds);
                    }
                }
            }

            String meta_data = String.format("%d , \"%s\", 0, %d, %d\r\n", race.round+1+extra_discards, race.name, MAX_ROUNDS, extra_discards);
            String output = meta_data + data;

            this.writeExportFile(context, output, race.name+".txt");

            datasource2.close();
            datasource.close();
        } else {
            // External storage is not writable
            // Not sure how to handle this at the moment!

        }

    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public File getDataStorageDir(String name) {
        // Get the directory for the user's public pictures directory.
        File base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        base = new File(base, "F3F");
        base.mkdirs();
        File file = new File(base.getAbsolutePath() + String.format("/%s", name));

        return file;
    }

}
