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
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import androidx.preference.PreferenceManager;

import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.results.Results;

import java.io.File;

public class F3FGearExport extends FileExport {

    public F3FGearExportInterface callbackInterface = null;

    /**
     * Write the results pilots.txt & flights.txt file for the given Race
     *
     * @param context Context
     * @param race Race
     * @return boolean
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public void writeResultsFile(Context context, Race race) {

        boolean success = false;

        // Update the pilots.txt and flights.txt files
        if (isExternalStorageWritable()) {

            StringBuilder pilots = new StringBuilder();
            StringBuilder flights = new StringBuilder();

            Results r = new Results();
            r.getResultsForRace(context, race.id, false);

            for (int i = 0; i < r.mArrPilots.size(); i++) {
                // Write row to pilots.txt
                Pilot p = r.mArrPilots.get(i);
                pilots.append(String.format("d%s %s %s", p.number, p.firstname, p.lastname).trim());
                pilots.append("\r\n");
            }


            Results r2 = new Results();
            for (int rnd = 0; rnd < race.round - 1; rnd++) {

                r2.getResultsForCompletedRound(context, race.id, rnd + 1);
                RaceData.Group g = r.mArrGroupings.get(rnd);

                // Write the round number - m is Spanish Manga (Round)
                StringBuilder row = new StringBuilder();
                row.append(String.format("m%d", rnd + 1));

                int mGroup = 0;
                for (int i = 0; i < r2.mArrPilots.size(); i++) {
                    Pilot p = r2.mArrPilots.get(i);

                    if (g.num_groups > 1) {
                        if (p.group != mGroup) {
                            mGroup = p.group;
                            row.append(String.format(" g%d", p.group));
                        }
                    }

                    // Write the pilot number - d is Spanish Dorsal (Back)
                    row.append(String.format(" d%s %.2f p%d", p.number, p.time, p.penalty * 100));
                }

                row.append("\r\n");


                flights.append(row);
            }

            this.writeExportFile(context, pilots.toString(), "pilots.txt");
            this.writeExportFile(context, flights.toString(),"flights.txt");

            success = true;
        }

        if (callbackInterface != null) {
            callbackInterface.onF3FGearFileWritten(success);
        }
    }

    /**
     * Override the base class method write to the file path set from settings
     *
     * @param context Context
     * @param name String
     * @return File
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected File getDataStorageDir(Context context, String name) {
        SharedPreferences path = PreferenceManager.getDefaultSharedPreferences(context);
        String strPath = path.getString("pref_results_F3Fgear_path", "");
        Uri uri = Uri.parse(strPath);
        String[] strArrPath = uri.getPath().split(":");
        File base = Environment.getExternalStoragePublicDirectory("");
        if (strArrPath.length > 1) {
            base = new File(base, strArrPath[1]);
            base.mkdirs();
        }
        return new File(base.getAbsolutePath() + String.format("/%s", sanitise(name)));
    }
}

