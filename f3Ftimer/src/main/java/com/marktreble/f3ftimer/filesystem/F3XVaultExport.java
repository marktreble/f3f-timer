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

import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.results.Results;

import java.io.File;
import java.util.ArrayList;

public class F3XVaultExport extends FileExport {

    /**
     * Write the results [race.name].f3xv.txt file for the given Race
     *
     * @param context Context
     * @param race Race
     * @return boolean if successfully written
     */
    public boolean writeResultsFile(Context context, Race race) {

        // Update the race (.f3f) file
        if (isExternalStorageWritable()) {
            StringBuilder data = new StringBuilder();

            Results r = new Results();
            r.getResultsForRace(context, race.id, false);

            for (int i = 0; i < r.mArrPilots.size(); i++) {
                Pilot p = r.mArrPilots.get(i);

                // Start new row (pilot name, frequency)
                StringBuilder row = new StringBuilder();
                row.append(String.format("0,%s %s,Open,%s", p.firstname, p.lastname, (p.frequency.equals("")) ? 0 : p.frequency));

                for (int rnd = 0; rnd < race.round - 1; rnd++) {
                    ArrayList<RaceData.Time> times = r.mArrTimes.get(i);

                    int pen = times.get(rnd).penalty;

                    String s_penalty = (pen > 0) ? String.format("%d", pen * 100) : "";
                    String s_group = Character.toString((char) (times.get(rnd).group + 48));
                    String s_time = String.format("%.2f", times.get(rnd).time).replace(",", ".");

                    row.append(String.format(",%s,%s,%s", s_penalty, s_group, s_time));
                }

                row.append("\r\n");


                data.append(row.toString());
            }

            String start_date = "[ENTER START DATE]";
            String end_date = "[ENTER END DATE]";
            String meta_data = String.format("0, \"%s\",\"%s\",\"%s\",f3f_group\r\n", race.name, start_date, end_date);

            String output = meta_data + data.toString();

            this.writeExportFile(context, output, race.name + ".f3xv.txt");
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
