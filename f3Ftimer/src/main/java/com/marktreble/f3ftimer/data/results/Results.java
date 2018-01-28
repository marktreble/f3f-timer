package com.marktreble.f3ftimer.data.results;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by marktreble on 11/06/2017.
 */

public class Results {

    public ArrayList<String> mArrNames;
    public ArrayList<String> mArrNumbers; // Position number in round or race
    public ArrayList<String> mArrBibNumbers;
    public ArrayList<Pilot> mArrPilots;
    public ArrayList<Integer> mArrGroups; // Holds the group number for the associated pilot
    public ArrayList<Boolean> mFirstInGroup; // Tells the UI to add the group header
    public ArrayList<Float> mArrScores;
    public ArrayList<ArrayList<RaceData.Time>> mArrTimes;

    public ArrayList<RaceData.Group> mArrGroupings; // Holds the number of groups + start pilot for each round

    public float mFTD;
    public String mFTDName;
    public int mFTDRound;

    public RaceData.Group mGroupScoring;

    /* getRoundInProgress(context, race ID)
     *
     * Populates: mArrNames, mArrPilots, mArrNumbers, mArrBibNumbers, mArrGroups, mFirstInGroup & mGroupScoring
     *
     */
    public void getRoundInProgress(Context context, int mRid){

        // Get Race Information
        RaceData datasource = new RaceData(context);
        datasource.open();
        Race race = datasource.getRace(mRid);

        // Number of Groups
        mGroupScoring = datasource.getGroups(mRid, race.round);

        datasource.close();

        RacePilotData datasource2 = new RacePilotData(context);
        datasource2.open();
        ArrayList<Pilot> allPilots = datasource2.getAllPilotsForRace(mRid, race.round);


        datasource2.close();

        // Initialise the output arrays
        mArrNames = new ArrayList<>();
        mArrPilots = new ArrayList<>();
        mArrNumbers = new ArrayList<>();
        mArrBibNumbers = new ArrayList<>();
        mArrGroups = new ArrayList<>();
        mFirstInGroup = new ArrayList<>();

        // Just calculate the scores. Results are shown in flying order
        calcScores(race, allPilots);
    }

    /* getOrderedRoundInProgress(context, race ID)
     *
     * Populates: mArrNames, mArrPilots, mArrNumbers, mArrBibNumbers, mArrGroups, mFirstInGroup & mGroupScoring
     *
     */
    public void getOrderedRoundInProgress(Context context, int mRid){

        // Get Race Information
        RaceData datasource = new RaceData(context);
        datasource.open();
        Race race = datasource.getRace(mRid);

        // Number of Groups
        mGroupScoring = datasource.getGroups(mRid, race.round);

        datasource.close();

        RacePilotData datasource2 = new RacePilotData(context);
        datasource2.open();
        ArrayList<Pilot> allPilots = datasource2.getAllPilotsForRace(mRid, race.round);


        datasource2.close();

        // Initialise the output arrays
        mArrNames = new ArrayList<>();
        mArrPilots = new ArrayList<>();
        mArrNumbers = new ArrayList<>();
        mArrBibNumbers = new ArrayList<>();
        mArrGroups = new ArrayList<>();
        mFirstInGroup = new ArrayList<>();

        // Just calculate the scores. Results are shown in flying order
        calcScores(race, allPilots);

        // Now sort in order of points
        Collections.sort(mArrPilots, new Comparator<Pilot>() {
            @Override
            public int compare(Pilot p1, Pilot p2) {
                return (p1.points < p2.points)? 1 : ((p1.points > p2.points) ? -1 : 0);
            }
        });

        // Reinitialise
        mArrNames = new ArrayList<>();
        mArrNumbers = new ArrayList<>();

        int pos=1;
        int c=1;
        float previousscore = 1000.0f;
        for (Pilot p : mArrPilots){
            if (p.points>0) {
                mArrNames.add(String.format("%s %s", p.firstname, p.lastname));

                // Check for tied scores - use the same position qualifier
                if (p.points < previousscore)
                    pos = c;
                previousscore = p.points;
                p.position = pos;
                mArrNumbers.add(String.format("%d", p.position));
                c++;
            }
        }

    }


    /* getResultsForCompletedRound(context, race ID, round number)
     *
     * Populates: mArrNames, mArrPilots, mArrNumbers, mArrGroups
     *
     */
    public void getResultsForCompletedRound(Context context, int mRid, int mRound){

        RaceData datasource = new RaceData(context);
        datasource.open();
        Race race = datasource.getRace(mRid);

        mGroupScoring = datasource.getGroups(mRid, mRound);

        datasource.close();

        RacePilotData datasource2 = new RacePilotData(context);
        datasource2.open();
        ArrayList<Pilot> allPilots = datasource2.getAllPilotsForRace(mRid, mRound);

        datasource2.close();

        Log.i("ROUND", "R="+mRound);
        Log.i("GROUP SCORING", mGroupScoring.num_groups+"");

        // Initialise the output arrays
        mArrNames = new ArrayList<>();
        mArrPilots = new ArrayList<>();
        mArrNumbers = new ArrayList<>();
        mArrBibNumbers = new ArrayList<>();
        mArrGroups = new ArrayList<>();
        mFirstInGroup = new ArrayList<>();

        // Calculate the scores
        calcScores(race, allPilots);

        // Now sort in order of points
        Collections.sort(mArrPilots, new Comparator<Pilot>() {
            @Override
            public int compare(Pilot p1, Pilot p2) {
                return (p1.points < p2.points)? 1 : ((p1.points > p2.points) ? -1 : 0);
            }
        });

        // Reinitialise
        mArrNames = new ArrayList<>();
        mArrNumbers = new ArrayList<>();
        mArrGroups = new ArrayList<>();

        int pos=1;
        int c=1;
        float previousscore = 1000.0f;
        for (Pilot p : mArrPilots){
            mArrNames.add(String.format("%s %s", p.firstname, p.lastname));

            // Check for tied scores - use the same position qualifier
            if (p.points < previousscore)
                pos = c;
            previousscore = p.points;
            p.position = pos;
            mArrNumbers.add(String.format("%d", p.position));
            mArrGroups.add(p.group);
            c++;
        }

    }

    /* getResultsForRace(context, race ID)
     *
     * Populates: mArrNames, mArrNumbers, mArrPilots, mArrScores, mArrTimes, mFTD, mFTDName, mFTDRound
     *
     */
    @SuppressWarnings("unchecked")
    public void getResultsForRace(Context context, int mRid, boolean ordered){

        RaceData datasource = new RaceData(context);
        datasource.open();
        Race race = datasource.getRace(mRid);

        RacePilotData datasource2 = new RacePilotData(context);
        datasource2.open();

        // Initialise the output arrays
        mArrTimes = new ArrayList<>();
        mArrScores = new ArrayList<>();
        mArrNames = new ArrayList<>();
        mArrPilots = new ArrayList<>();
        mArrNumbers = new ArrayList<>();
        mArrBibNumbers = new ArrayList<>();
        mArrGroups = new ArrayList<>();
        mFirstInGroup = new ArrayList<>();

        mArrGroupings = new ArrayList<>();

        int completed_rounds = race.round;
        if (race.status != Race.STATUS_COMPLETE && !datasource2.isRoundComplete(mRid, race.round))
            completed_rounds--;

        if (completed_rounds<1) return;

        SparseArray<ArrayList<RaceData.Time>> map_pilots= new SparseArray();
        SparseArray<Float> map_totals= new SparseArray<>();

        mFTD = 9999;
        mFTDName = "";
        mFTDRound = 0;

        if (completed_rounds>0) {
            // Loop through each round to find the winner, then populate the scores
            for (int rnd = 1; rnd <= completed_rounds; rnd++) {
                mGroupScoring = datasource.getGroups(mRid, rnd);
                Log.i("ROUND", "R="+rnd);
                Log.i("GROUP SCORING", mGroupScoring.num_groups+"");

                // Add the group params to the array
                mArrGroupings.add(mGroupScoring);

                ArrayList<Pilot> allPilots = datasource2.getAllPilotsForRace(mRid, rnd);

                // Reinitialise the output arrays
                mArrNames = new ArrayList<>();
                mArrPilots = new ArrayList<>();
                mArrNumbers = new ArrayList<>();
                mArrBibNumbers = new ArrayList<>();
                mArrGroups = new ArrayList<>();
                mFirstInGroup = new ArrayList<>();

                // Calculate the scores
                calcScores(race, allPilots);

                for (Pilot p : mArrPilots){
                    RaceData.Time pilot_time = new RaceData(null).new Time();
                    pilot_time.time = p.time;
                    pilot_time.raw_points = p.raw_points;
                    pilot_time.points = p.points;
                    pilot_time.penalty = p.penalty;
                    pilot_time.round = rnd;
                    pilot_time.group = p.group;

                    ArrayList<RaceData.Time> arr_times;
                    Object o = map_pilots.get(p.id);

                    if (o != null) {
                        arr_times =(ArrayList<RaceData.Time>)o;
                    } else {
                        arr_times = new ArrayList<>();
                    }
                    arr_times.add(pilot_time);

                    map_pilots.put(p.id, arr_times);

                    // Check FTD
                    if (p.time>0) {
                        mFTD = Math.min(mFTD, p.time);
                        if (mFTD == p.time) {
                            mFTDRound = rnd;
                            mFTDName = String.format("%s %s", p.firstname, p.lastname);
                        }
                    }
                }

            }
        }
        datasource.close();
        datasource2.close();

        int numdiscards = (completed_rounds>4) ? ((completed_rounds>15) ? 2 : 1) : 0;
        for(int i = 0; i < map_pilots.size(); i++) {
            int key = map_pilots.keyAt(i);

            // The times list
            Object o = map_pilots.get(key);
            ArrayList<RaceData.Time> arr_times = new ArrayList<>((ArrayList<RaceData.Time>)o);

            // Sort the times by points in the round
            Collections.sort(arr_times, new Comparator<RaceData.Time>() {
                @Override
                public int compare(RaceData.Time t1, RaceData.Time t2) {
                    // Sort on raw points (not penalised points)
                    Float p1 = t1.raw_points;
                    Float p2 = t2.raw_points;
                    return  p1.compareTo(p2);
                }
            });

            // Tot up the points from each round ignoring the first {numdiscards} rounds
            Float tot = 0f;
            for (int j=numdiscards; j<completed_rounds; j++) {
                tot += arr_times.get(j).raw_points;
                Log.i("TOT", key+":"+tot+":"+arr_times.get(j).raw_points);
            }

            // Deduct penalties from all rounds from total
            for (int j=0; j<completed_rounds; j++)
                tot -= arr_times.get(j).penalty * 100;

            map_totals.put(key, round2FixedRounded(tot, 2));
        }

        // Make an array from the totals and sort them in ascending order
        Float[] totals = new Float[map_totals.size()];
        for (int i = 0; i < map_totals.size(); i++)
            totals[i] = map_totals.valueAt(i);

        Arrays.sort(totals, Collections.reverseOrder());

        // Set the positions according to the sorted order
        for (int i=0; i<mArrPilots.size(); i++){
            Pilot p = mArrPilots.get(i);
            float total = map_totals.get(p.id);
            for (int j=0; j<totals.length; j++){
                float sorted_total = totals[j];
                if (total == sorted_total)
                    p.position = j + 1;

            }

        }

        // Sort the pilots by position (if set)
        if (ordered) {
            Collections.sort(mArrPilots, new Comparator<Pilot>() {
                @Override
                public int compare(Pilot p1, Pilot p2) {
                    return p1.position.compareTo(p2.position);
                }
            });
        } else {
            // Otherwise by the original flying order
            Collections.sort(mArrPilots, new Comparator<Pilot>() {
                @Override
                public int compare(Pilot p1, Pilot p2) {
                    return p1.id<p2.id ? -1 : p1.id == p2.id ? 0 : 1;
                }
            });
        }

        mArrNames = new ArrayList<>();
        mArrNumbers = new ArrayList<>();
        mArrScores = new ArrayList<>();
        mArrTimes = new ArrayList<>();

        float score = 0, best = 0;
        for (int i=0; i<mArrPilots.size(); i++) {
            Pilot p = mArrPilots.get(i);

            // Calc final normalised points
            if (best == 0) best = map_totals.get(p.id);
            score = map_totals.get(p.id);

            p.points = round2Fixed((score/best)*1000, 2);

            mArrNames.add(String.format("%s %s", p.firstname, p.lastname));
            mArrNumbers.add(Integer.toString(p.position));
            mArrScores.add(map_totals.get(p.id));
            mArrTimes.add(map_pilots.get(p.id));

        }
    }

    /* getResultsForRace(context, race ID)
     *
     * Populates: mArrNames, mArrNumbers, mArrPilots, mArrScores, mArrTimes, mFTD, mFTDName, mFTDRound
     *
     */
    public void getTeamResultsForRace(Context context, int mRid){
        this.getResultsForRace(context, mRid, false);

        mArrNames = new ArrayList<>();
        mArrNumbers = new ArrayList<>();
        HashMap<String, Float> totals = new HashMap<>();

        for (int i=0; i<mArrPilots.size(); i++) {
            Pilot p = mArrPilots.get(i);

            if (!p.team.trim().equals("")) { // Ignore blank entries
                Float score = mArrScores.get(i);

                if (totals.containsKey(p.team)) {
                    Float total = totals.get(p.team);
                    total += score;
                    totals.put(p.team, total);
                } else {
                    totals.put(p.team, score);

                }
            }
        }

        Float fTotals[] = new Float[totals.size()];
        Iterator<String> itr = totals.keySet().iterator();
        int i = 0;
        while(itr.hasNext()) {
            String name = itr.next();
            fTotals[i++] = totals.get(name);
        }

        Arrays.sort(fTotals, Collections.reverseOrder());
        mArrScores = new ArrayList<>();

        // Set the positions according to the sorted order
        for (int j=0; j<fTotals.length; j++){
            float sorted_total = fTotals[j];
            Iterator<String> itr2 = totals.keySet().iterator();
            while(itr2.hasNext()) {
                String name = itr2.next();
                float total = totals.get(name);

                if (total == sorted_total) {
                    mArrNumbers.add(Integer.toString(j + 1));
                    mArrScores.add(total);
                    mArrNames.add(name);
                }
            }
        }
    }

    public static float round2Fixed(float value, double places){

        double multiplier = Math.pow(10, places);
        double integer = Math.floor(value);
        double precision = Math.floor((value-integer) * multiplier);

        return (float)(integer + (precision/multiplier));
    }

    private float round2FixedRounded(float value, double places){

        double multiplier = Math.pow(10, places);
        double integer = Math.floor(value);
        double precision = Math.floor((value-integer) * multiplier);

        double rounded = (integer + (precision/multiplier));
        double remainder = (value-rounded)*multiplier;
        if (remainder>0.5) rounded+=Math.pow(10, -places);

        return (float)rounded;
    }

    private float[] initFTG(){
        float[] ftg = new float[mGroupScoring.num_groups+1]; // Fastest time in group (used for calculating normalised scores)
        for (int i=0; i<mGroupScoring.num_groups+1; i++)
            ftg[i]= 9999;

        return ftg;
    }

    private int getActualPilotsCount(ArrayList<Pilot> allPilots){
        int num_pilots = 0;
        for (int i=0;i<allPilots.size();i++){
            Pilot p = allPilots.get(i);
            if (p.pilot_id>0) {
                num_pilots++;
            }
        }
        return num_pilots;
    }

    private void calcScores(Race race, ArrayList<Pilot> allPilots){
        int g = 0; // Current group we are calculating

        // The start pilot number
        int start;
        if (race.start_number > 0) {
            start = race.start_number - 1;
        } else {
            start = ((race.round - 1) * race.offset) % allPilots.size();
        }

        // Init array for Fastest time in group
        float[] ftg = initFTG();

        // Find actual number of pilots
        int num_pilots = getActualPilotsCount(allPilots);

        boolean first = true;
        int group_size = (int)Math.floor(num_pilots/mGroupScoring.num_groups);
        int remainder = num_pilots - (mGroupScoring.num_groups * group_size);

        // Find ftr
        int c = 0; // Flying order number
        int apn = 0; // Actual Pilot number (Used for calculating groups);

        for (int j=0;j<allPilots.size();j++){
            if (g<remainder){
                if (apn>= (group_size+1)*(g+1)) {
                    g++;
                    first = true;
                }
            } else {
                if (apn>= ((group_size+1)*remainder) + (group_size*((g+1)-remainder))) {
                    g++;
                    first = true;
                }
            }
            Pilot p = allPilots.get(j);
            int bib_number = ((c + start) % allPilots.size())+1;
            if (p.pilot_id>0) {
                // Pilot is Actual (not skipped)

                mArrNames.add(String.format("%s %s", p.firstname, p.lastname));
                mArrBibNumbers.add(String.format("%d", bib_number));
                mArrGroups.add(g);
                mFirstInGroup.add(first);
                mArrPilots.add(p);
                first = false;

                String t_str = String.format("%.2f", p.time).replace(",", ".");
                float time = Float.parseFloat(t_str);

                ftg[g] = (time > 0) ? Math.min(ftg[g], time) : ftg[g];
                apn++;
            }
            c++;
        }

        // Set points for each pilot
        g=0; apn=0;

        // Set points for each pilot
        for (int i=0;i<allPilots.size();i++){
            if (g<remainder){
                if (apn>= (group_size+1)*(g+1)) {
                    g++;
                }
            } else {
                if (apn>= ((group_size+1)*remainder) + (group_size*((g+1)-remainder))) {
                    g++;
                }
            }
            Pilot p = allPilots.get(i);


            String t_str = String.format("%.2f", p.time).replace(",", ".");
            float time = Float.parseFloat(t_str);

            if (time>0)
                p.points = round2Fixed((ftg[g]/time) * 1000, 2);

            if ((p.time==0 || Float.isNaN(p.time)) && p.flown) // Avoid division by 0
                p.points = 0f;

            p.raw_points = Math.max(0, p.points);

            p.points-= p.penalty * 100;

            if ((p.time==0 || Float.isNaN(p.time)) && p.status==Pilot.STATUS_RETIRED) // Avoid division by 0
                p.points = 0f;

            if (p.pilot_id> 0) {
                // Pilot is Actual (not skipped)
                apn++;
            }
        }
    }

}
