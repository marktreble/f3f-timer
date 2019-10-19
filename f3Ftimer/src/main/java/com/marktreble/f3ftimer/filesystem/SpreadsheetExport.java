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

package com.marktreble.f3ftimer.filesystem;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.results.Results;

import java.io.File;
import java.util.ArrayList;

public class SpreadsheetExport extends FileExport {

    /**
     * Write the results [race.name].txt file for the given Race
     *
     * @param context Context
     * @param race Race
     * @return boolean
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean writeResultsFile(Context context, Race race) {

        int MAX_ROUNDS = 10;

        if (race.round > 10) MAX_ROUNDS = 20;
        if (race.round > 20) MAX_ROUNDS = 30;

        // Update the race (.f3f) file
        if (isExternalStorageWritable()) {
            Log.i("ROW", "WRITING SPREADSHEET FILE");
            StringBuilder data = new StringBuilder();

            Results r = new Results();
            r.getResultsForRace(context, race.id, false);


            int extra_discards = 0;
            for (int i = 0; i < r.mArrGroupings.size(); i++) {
                RaceData.Group g = r.mArrGroupings.get(i);
                extra_discards += g.num_groups - 1;
                Log.i("GROUPS: ", "N:" + g.num_groups + " S:" + g.start_pilot);
            }

            Log.i("ROW", "-----");

            for (int i = 0; i < r.mArrPilots.size(); i++) {
                Pilot p = r.mArrPilots.get(i);

                int penalties_total = 0;
                StringBuilder penalties_rounds = new StringBuilder();

                // Start new row (pilot name, frequency)
                StringBuilder row = new StringBuilder(String.format("%s %s , %s", p.firstname, p.lastname, (p.frequency.equals("")) ? 0 : p.frequency));

                int rnd_in_spreadsheet = 1;
                for (int rnd = 0; rnd < MAX_ROUNDS - extra_discards; rnd++) {
                    int numgroups;
                    if (rnd < r.mArrGroupings.size()) {
                        RaceData.Group grouping = r.mArrGroupings.get(rnd);
                        numgroups = grouping.num_groups;

                        for (int g = 0; g < numgroups; g++) {
                            ArrayList<RaceData.Time> times = r.mArrTimes.get(i);
                            if (g == times.get(rnd).group) {
                                String s_time = String.format("%.2f", times.get(rnd).time);
                                row.append(String.format(" , %s", s_time.replace(",", ".")));

                                int pen = times.get(rnd).penalty;
                                if (pen > 0) {
                                    penalties_total += pen * 100;
                                    penalties_rounds.append(String.format("%02d", rnd_in_spreadsheet));
                                }
                            } else {
                                row.append(" , 0");
                            }
                        }
                        rnd_in_spreadsheet += numgroups;
                    } else {
                        row.append(" , 0");
                    }
                }

                row.append(String.format(" , %d , 0 , %s\r\n", penalties_total, penalties_rounds.toString()));
                data.append(row.toString());
            }


            String meta_data = String.format("%d , \"%s\", 0, %d, %d\r\n", race.round + extra_discards, race.name, MAX_ROUNDS, extra_discards);
            String output = meta_data + data.toString();

            this.writeExportFile(context, output, race.name + ".txt");

            return true;
        } else {
            // External storage is not writable
            return false;
        }
    }

    /**
     * Get storage directory. By default this creates and returns /F3F directory within Documents
     *
     * @param context Context
     * @param name String
     * @return File
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public File getDataStorageDir(Context context, String name) {
        // Get the directory for the user's public pictures directory.
        File base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        base = new File(base, "F3F");
        base.mkdirs();
        return new File(base.getAbsolutePath() + String.format("/%s", sanitise(name)));
    }
}
