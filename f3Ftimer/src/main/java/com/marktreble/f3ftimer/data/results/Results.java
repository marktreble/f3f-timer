package com.marktreble.f3ftimer.data.results;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;
import com.marktreble.f3ftimer.resultsmanager.ResultsLeaderBoardActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by marktreble on 11/06/2017.
 */

public class Results {

    public ArrayList<String> mArrNames;
    public ArrayList<String> mArrNumbers; // Position number in round or race
    public ArrayList<String> mArrBibNumbers;
    public ArrayList<Pilot> mArrPilots;
    public ArrayList<Integer> mArrGroups;
    public ArrayList<Boolean> mFirstInGroup; // Tells the UI to add the group header
    public ArrayList<Float> mArrScores;
    public ArrayList<ArrayList<Float>> mArrTimes;

    public float mFTD;
    public String mFTDName;
    public int mFTDRound;

    public int mGroupScoring;

    /* getRoundInProgress(context, race ID)
     *
     * Populates: mArrNames, mArrPilots, mArrNumbers, mArrGroups, mFirstInGroup & mGroupScoring
     *
     */
    public void getRoundInProgress(Context context, int mRid){

        RaceData datasource = new RaceData(context);
        datasource.open();
        Race race = datasource.getRace(mRid);

        RacePilotData datasource2 = new RacePilotData(context);
        datasource2.open();
        ArrayList<Pilot> allPilots = datasource2.getAllPilotsForRace(mRid, race.round, 0, 0);

        mArrNames = new ArrayList<>();
        mArrPilots = new ArrayList<>();
        mArrNumbers = new ArrayList<>();
        mArrGroups = new ArrayList<>();
        mFirstInGroup = new ArrayList<>();

        int g = 0; // Current group we are calculating

        mGroupScoring = datasource.getGroups(mRid, race.round);

        float[] ftg = new float[mGroupScoring+1]; // Fastest time in group (used for calculating normalised scores)
        for (int i=0; i<mGroupScoring+1; i++)
            ftg[i]= 9999;

        // Find actual number of pilots
        int num_pilots = 0;
        for (int i=0;i<allPilots.size();i++){
            Pilot p = allPilots.get(i);
            if (p.pilot_id>0) {
                num_pilots++;
            }
        }

        int group_size = (int)Math.floor(num_pilots/mGroupScoring);
        int remainder = allPilots.size() - (mGroupScoring * group_size);

        boolean first = true;

        // Find ftr
        int c=0, i = 0;
        for (int j=0;j<allPilots.size();j++){
            i = mArrPilots.size();
            if (g<remainder){
                if (i>= (group_size+1)*(g+1)) {
                    g++;
                    first = true;
                }
            } else {
                if (i>= ((group_size+1)*remainder) + (group_size*((g+1)-remainder))) {
                    g++;
                    first = true;
                }
            }
            Pilot p = allPilots.get(j);
            if (p.pilot_id>0) {
                mArrNames.add(String.format("%s %s", p.firstname, p.lastname));
                mArrNumbers.add(String.format("%d", c + 1));
                mArrGroups.add(g);
                mFirstInGroup.add(first);
                mArrPilots.add(p);
                first = false;

                String t_str = String.format("%.2f", p.time).trim().replace(",", ".");
                float time = Float.parseFloat(t_str);

                ftg[g] = (time > 0) ? Math.min(ftg[g], time) : ftg[g];
            }
            c++;
        }

        // Set points for each pilot
        g=0;

        // Set points for each pilot
        for (i=0;i<allPilots.size();i++){
            if (g<remainder){
                if (i>= (group_size+1)*(g+1)) {
                    g++;
                }
            } else {
                if (i>= ((group_size+1)*remainder) + (group_size*((g+1)-remainder))) {
                    g++;
                }
            }
            Pilot p = allPilots.get(i);
            String t_str = String.format("%.2f", p.time).trim().replace(",", ".");
            float time = Float.parseFloat(t_str);

            if (time>0)
                p.points = round2Fixed((ftg[g]/time) * 1000, 2);

            if (time==0 && p.flown) // Avoid division by 0
                p.points = 0f;

            p.points-= p.penalty * 100;

            if (time==0 && p.status==Pilot.STATUS_RETIRED) // Avoid division by 0
                p.points = 0f;

        }

        datasource2.close();
        datasource.close();
    }

    /* getResultsForCompletedRound(context, race ID, round number)
     *
     * Populates: mArrNames, mArrPilots, mArrNumbers, mArrGroups, mFirstInGroup & mGroupScoring
     *
     */
    public void getResultsForCompletedRound(Context context, int mRid, int mRound){

        RaceData datasource = new RaceData(context);
        datasource.open();

        RacePilotData datasource2 = new RacePilotData(context);
        datasource2.open();
        ArrayList<Pilot> allPilots = datasource2.getAllPilotsForRace(mRid, mRound, 0, 0);

        mArrNames = new ArrayList<>();
        mArrPilots = new ArrayList<>();
        mArrNumbers = new ArrayList<>();
        mArrGroups = new ArrayList<>();
        mFirstInGroup = new ArrayList<>();

        int g = 0; // Current group we are calculating

        mGroupScoring = datasource.getGroups(mRid, mRound);

        float[] ftg = new float[mGroupScoring+1]; // Fastest time in group (used for calculating normalised scores)
        for (int i=0; i<mGroupScoring+1; i++)
            ftg[i]= 9999;

        // Find actual number of pilots
        int num_pilots = 0;
        for (int i=0;i<allPilots.size();i++){
            Pilot p = allPilots.get(i);
            if (p.pilot_id>0) {
                num_pilots++;
            }
        }

        int group_size = (int)Math.floor(num_pilots/mGroupScoring);
        int remainder = allPilots.size() - (mGroupScoring * group_size);

        boolean first = true;

        // Find ftg
        int c = 0, i = 0;
        for (int j=0;j<allPilots.size();j++){
            i = mArrPilots.size();
            if (g<remainder){
                if (i>= (group_size+1)*(g+1)) {
                    g++;
                    first = true;
                }
            } else {
                if (i>= ((group_size+1)*remainder) + (group_size*((g+1)-remainder))) {
                    g++;
                    first = true;
                }
            }
            Pilot p = allPilots.get(j);
            if (p.pilot_id>0) {
                mArrPilots.add(p);
                p.position = c+1;
                mArrGroups.add(g);
                mFirstInGroup.add(first);
                first = false;

                String t_str = String.format("%.2f", p.time).trim().replace(",", ".");
                float time = Float.parseFloat(t_str);

                ftg[g] = (time > 0) ? Math.min(ftg[g], time) : ftg[g];
            }
            c++;
        }

        g=0;

        // Set points for each pilot
        for (i=0;i<num_pilots;i++){
            if (g<remainder){
                if (i>= (group_size+1)*(g+1)) {
                    g++;
                }
            } else {
                if (i>= ((group_size+1)*remainder) + (group_size*((g+1)-remainder))) {
                    g++;
                }
            }
            Pilot p = mArrPilots.get(i);

            String t_str = String.format("%.2f", p.time).trim().replace(",", ".");
            float time = Float.parseFloat(t_str);

            if (time>0)
                p.points = this.round2Fixed((ftg[g]/time) * 1000, 2);

            if (time==0 && p.flown) // Avoid division by 0
                p.points = 0f;

            p.points-= p.penalty * 100;

            if (time==0 && p.status==Pilot.STATUS_RETIRED) // Avoid division by 0
                p.points = 0f;

        }

        Collections.sort(mArrPilots, new Comparator<Pilot>() {
            @Override
            public int compare(Pilot p1, Pilot p2) {
                return (p1.points < p2.points)? 1 : ((p1.points > p2.points) ? -1 : 0);
            }
        });

        int pos=1;
        c=1;
        float previousscore = 1000.0f;
        for (Pilot p : mArrPilots){
            mArrNames.add(String.format("%s %s", p.firstname, p.lastname));

            // Check for tied scores - use the same position qualifier
            if (p.points < previousscore)
                pos = c;
            previousscore = p.points;
            p.position = pos;
            mArrNumbers.add(String.format("%d", p.position));
            Log.d("MARRNAMES", p.firstname+":"+p.position);
            c++;
        }

        datasource2.close();
        datasource.close();
    }

    /* getLeaderBoard(context, race ID)
     *
     * Populates: mArrNames, mArrPilots, mArrScores, mFTD, mFTDName, mFTDRound
     *
     */
    public void getLeaderBoard(Context context, int mRid){

        RaceData datasource = new RaceData(context);
        datasource.open();
        Race race = datasource.getRace(mRid);

        RacePilotData datasource2 = new RacePilotData(context);
        datasource2.open();
        ArrayList<Pilot> allPilots = datasource2.getAllPilotsForRace(mRid, 0, 0, 0);
        ArrayList<String> p_names = new ArrayList<>();
        ArrayList<String> p_bib_numbers = new ArrayList<>();
        ArrayList<String> p_nationalities = new ArrayList<>();
        ArrayList<float[]> p_times = new ArrayList<>();
        ArrayList<float[]> p_points = new ArrayList<>();
        ArrayList<int[]> p_penalty = new ArrayList<>();
        Float[] p_totals;
        int[] p_positions;

        mFTD = 9999;

        if (allPilots != null){

            // Get all times for pilots in all rounds
            int c=0; // Counter for bib numbers
            for (Pilot p : allPilots){
                if (p.pilot_id>0) {
                    p_names.add(String.format("%s %s", p.firstname, p.lastname));
                    p_bib_numbers.add(Integer.toString(c + 1));
                    p_nationalities.add(p.nationality);
                    float[] sc = new float[race.round];
                    for (int rnd = 0; rnd < race.round; rnd++) {
                        sc[rnd] = datasource2.getPilotTimeInRound(mRid, p.id, rnd + 1);
                    }
                    p_times.add(sc);
                    Log.d("LEADERBOARD", String.format("%s %s %d", p.firstname, p.lastname, c+1));
                }
                c++;
            }

            p_totals = new Float[p_names.size()];
            p_positions = new int[p_names.size()];

            if (race.round>1){
                // Loop through each round to find the winner, then populate the scores
                for (int rnd=0; rnd<race.round-1; rnd++){
                    ArrayList<Pilot> pilots_in_round = datasource2.getAllPilotsForRace(mRid, rnd+1, 0, 0);

                    mGroupScoring = datasource.getGroups(mRid, rnd+1);

                    int g = 0; // Current group we are calculating

                    float[] ftg = new float[mGroupScoring+1]; // Fastest time in group (used for calculating normalised scores)
                    for (int i=0; i<mGroupScoring+1; i++)
                        ftg[i]= 9999;

                    int group_size = (int)Math.floor(p_names.size()/mGroupScoring);
                    int remainder = p_names.size() - (mGroupScoring * group_size);

                    for (int i=0; i<p_names.size(); i++){
                        if (g<remainder){
                            if (i>= (group_size+1)*(g+1)) {
                                g++;
                            }
                        } else {
                            if (i>= ((group_size+1)*remainder) + (group_size*((g+1)-remainder))) {
                                g++;
                            }
                        }

                        String str_t = String.format("%.2f",p_times.get(i)[rnd]).trim().replace(",", ".");
                        float t = Float.parseFloat(str_t);
                        if (t>0)
                            ftg[g] = Math.min( t, ftg[g]);

                        // Update the FTD here too
                        mFTD = Math.min(mFTD,  ftg[g]);
                        if (mFTD == t){
                            mFTDRound = rnd+1;
                            mFTDName = p_names.get(i);
                        }

                    }

                    g = 0; // Current group we are calculating

                    float[] points = new float[p_names.size()];
                    int[] penalty = new int[p_names.size()];
                    for (int i=0; i<p_names.size(); i++){
                        if (g<remainder){
                            if (i>= (group_size+1)*(g+1)) {
                                g++;
                            }
                        } else {
                            if (i>= ((group_size+1)*remainder) + (group_size*((g+1)-remainder))) {
                                g++;
                            }
                        }

                        String str_t = String.format("%.2f",p_times.get(i)[rnd]).trim().replace(",", ".");
                        float time = Float.parseFloat(str_t);
                        float pnts = 0;
                        if (time>0)
                            pnts = round2Fixed((ftg[g]/time) * 1000, 2);


                        points[i] = pnts;
                        penalty[i] = pilots_in_round.get(i).penalty;
                    }
                    p_points.add(points);
                    p_penalty.add(penalty);
                }

                // Loop through each pilot to Find discards + calc totals
                int numdiscards = (race.round>4) ? ((race.round>15) ? 2 : 1) : 0;
                for (int i=0; i<p_names.size(); i++){
                    Float[] totals = new Float[race.round-1];

                    float penalties = 0;
                    for (int rnd=0; rnd<race.round-1; rnd++){
                        totals[rnd] = p_points.get(rnd)[i];
                        penalties+=p_penalty.get(rnd)[i] * 100;
                    }

                    // sort totals in order then lose the lowest according to numdiscards
                    Arrays.sort(totals);
                    float tot = 0;
                    for (int j=numdiscards; j<race.round-1; j++)
                        tot += totals[j];

                    // Now apply penalties
                    p_totals[i] = tot - penalties;
                }

                // Now sort the pilots
                Float[] p_sorted_totals = p_totals.clone();
                Arrays.sort(p_sorted_totals, Collections.reverseOrder());

                // Set the positions according to the sorted order
                for (int i=0; i<p_names.size(); i++){
                    float p_total = p_totals[i];
                    for (int j=0; j<p_names.size(); j++){
                        float p_sorted_total = p_sorted_totals[j];
                        if (p_total == p_sorted_total)
                            p_positions[i] = j + 1;

                    }
                }

                int sz = p_names.size();
                mArrNames = new ArrayList<>(sz);
                mArrNumbers = new ArrayList<>(sz);
                mArrPilots = new ArrayList<>(sz);
                mArrScores = new ArrayList<>(sz);

                // Initialise
                for (int i = 0; i < sz; i++) {
                    mArrNames.add("");
                    mArrNumbers.add("");
                    mArrPilots.add(new Pilot());
                    mArrScores.add(1000f);
                }

                for (int i=0; i<sz; i++){
                    int pos = p_positions[i]-1;
                    mArrNames.set(pos, String.format("%s", p_names.get(i)));
                    //mArrNumbers.set(pos, p_bib_numbers.get(i));
                    mArrNumbers.set(pos, Integer.toString(p_positions[i]));
                    Pilot p = new Pilot();
                    p.points = round2Fixed(p_totals[i].floatValue(), 2);
                    p.nationality = p_nationalities.get(i);
                    mArrPilots.set(pos, p);
                }

                float top_score = mArrPilots.get(0).points;
                float previousscore = 1000.0f;

                int pos = 1, lastpos = 1; // Last pos is for ties
                for (int i=1; i<sz; i++){
                    float pilot_points = mArrPilots.get(i).points;
                    float normalised = round2Fixed(pilot_points/top_score * 1000, 2);

                    previousscore = normalised;

                    mArrScores.set(i, Float.valueOf(normalised));
                }
            } else {
                // No rounds complete yet
                mArrNames = new ArrayList<>(0);
                mArrPilots = new ArrayList<>(0);
                mArrScores = new ArrayList<>(0);
            }
            datasource2.close();
        }
        datasource.close();
    }

    private float round2Fixed(float value, double places){

        double multiplier = Math.pow(10, places);
        double integer = Math.floor(value);
        double precision = Math.floor((value-integer) * multiplier);

        return (float)(integer + (precision/multiplier));
    }
}
