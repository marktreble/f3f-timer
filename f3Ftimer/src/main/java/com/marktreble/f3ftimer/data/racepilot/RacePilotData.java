package com.marktreble.f3ftimer.data.racepilot;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.RaceDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class RacePilotData {
	// Database fields
	private SQLiteDatabase database;
	//private SQLiteDatabase databasep;
	private RaceDatabase db;
	//private PilotDatabase dbp;

	public RacePilotData(Context context) {
		db = new RaceDatabase(context);
		//dbp = new PilotDatabase(context);
	}

	public void open() throws SQLException {
		database = db.getWritableDatabase();
		//databasep = dbp.getReadableDatabase();
		//database.execSQL("ATTACH DATABASE '" + databasep.getAttachedDbs().get(0).second + "' AS pilots");
	}

	public void close()	{
		db.close();
		//dbp.close();
	}

	public boolean isRoundComplete(int race_id, int round) {
		int number_of_pilots_to_fly = 0;
		String sql = "select count(*) " +
				"from racepilots rp " +
				"left join racetimes rt " +
				"on (rt.race_id=rp.race_id and rt.pilot_id=rp.id and rt.round=?) " +
				"where rp.race_id=? and rp.status!=? and rt.id is null";
		String[] data = {Integer.toString(round), Integer.toString(race_id), Integer.toString(Pilot.STATUS_RETIRED)};
		Cursor cursor = database.rawQuery(sql, data);
		cursor.moveToFirst();
		if (!cursor.isAfterLast()){
			number_of_pilots_to_fly = cursor.getInt(0);
		}
		cursor.close();
		return number_of_pilots_to_fly == 0;
	}

	public int getMaxRound(int race_id) {
		String[] cols = {"max(round)"};
		String where = "race_id=?";
		String[] whereArgs = {Integer.toString(race_id)};
		Cursor cursor = database.query("racetimes", cols, where, whereArgs, null, null, null);
		cursor.moveToFirst();
		int maxRound = 0;
		if (!cursor.isAfterLast()){
			maxRound = cursor.getInt(0);
		}
		cursor.close();
		return maxRound;
	}

	public Pilot getPilot(int id, int race){
		String sql = "select id, pilot_id, race_id, status, firstname, lastname, email, frequency, models, nationality, language, team " +
					 "from racepilots " +
					 "where id=? and race_id=?";
		String[] data = {Integer.toString(id), Integer.toString(race)};
		Cursor cursor = database.rawQuery(sql, data);
		cursor.moveToFirst();
		Pilot p;
		if (!cursor.isAfterLast()) {
			cursor.moveToFirst();
			p = cursorToPilot(cursor, false);
		} else {
			p = new Pilot(); // Return a blank object if not found
		}
		cursor.close();
		return p;
	}

	public float getPilotTimeInRound(int race_id, int pilot_id, int round){
		float time = 0;
		String where = "pilot_id=" + Integer.toString(pilot_id) + " and race_id=" + Integer.toString(race_id) + " and round=" + Integer.toString(round) + " and (reflight is null or reflight=0)";
		String[] cols = {"time"};
		Cursor cursor = database.query("racetimes", cols, where, null, null, null, null);
		cursor.moveToFirst();
		if (!cursor.isAfterLast()) {
			time = cursor.getFloat(0);
		}
		cursor.close();
		return time;
	}

	public void scoreZero(int race_id, int round, int pilot_id) {
		ContentValues values = new ContentValues();
		String[] cols = {"*"};
		String where = "race_id=? and pilot_id=? and round=?";
		String[] data = {Integer.toString(race_id), Integer.toString(pilot_id), Integer.toString(round)};
		Cursor cursor = database.query("racetimes", cols, where, data, null, null, null);
		cursor.moveToFirst();
		values.put("time", 0.0f);
		values.put("penalty", 0);
		values.put("points", 0.0f);
		values.put("reflight", 0);
		if (!cursor.isAfterLast()) {
			database.update("racetimes", values, where, data);
		} else {
			values.putNull("id");			// this is the id in the race - also the running order
			values.put("pilot_id", pilot_id); 	// passed in id becomes pilot_id
			values.put("race_id", race_id);
			values.put("round", round);
			database.insert("racetimes", null, values);
		}
		cursor.close();

		String sql = "update racepilots set status=? where race_id=? and id=?";
		String[] data2 = {Integer.toString(Pilot.STATUS_NORMAL), Integer.toString(race_id), Integer.toString(pilot_id)};
		database.execSQL(sql, data2);
	}

	public void setPilotTimeInRound(int race_id, int pilot_id, int round, float time){
		Pilot p = getPilot(pilot_id, race_id);
		String[] cols = {"*"};
		String where = "race_id=? and pilot_id=? and round=?";
		String[] data = {Integer.toString(race_id), Integer.toString(p.id), Integer.toString(round)};
		Cursor cursor = database.query("racetimes", cols, where, data, null, null, null);
		cursor.moveToFirst();
		ContentValues values = new ContentValues();
		values.put("time", time);
		values.put("reflight", 0);
		if (!cursor.isAfterLast()) {
			database.update("racetimes", values, where, data);
		} else {
			values.putNull("id");			// this is the id id in the race - also the running order
			values.put("pilot_id", pilot_id); 	// passed in id becomes pilot_id
			values.put("race_id", race_id);
			values.put("round", round);
			values.put("penalty", 0);
			values.put("group_nr", 0);
			values.put("points", Float.NaN);
			database.insert("racetimes", null, values);
		}
		cursor.close();

		String sql = "update racepilots set status=? where race_id=? and id=?";
		String[] data2 = {Integer.toString(Pilot.STATUS_NORMAL), Integer.toString(race_id), Integer.toString(pilot_id)};
		database.execSQL(sql, data2);
	}

	public void setReflightInRound(int race_id, int pilot_id, int round){
		String sql;
		
		sql = "update racetimes set reflight=1 where race_id=? and pilot_id=? and round=?";
		String[] data = {Integer.toString(race_id), Integer.toString(pilot_id), Integer.toString(round)};
		database.execSQL(sql, data);
	}
	
	public void setPenalty(int race_id, int pilot_id, int round, int penalty){
        String sql;

        sql = "update racetimes set penalty=? where race_id=? and pilot_id=? and round=?";
        String[] data2 = { Integer.toString(penalty), Integer.toString(race_id), Integer.toString(pilot_id), Integer.toString(round)};
        database.execSQL(sql, data2);
    }
    
    public void incPenalty(int race_id, int pilot_id, int round){
		String sql;
		
		sql = "update racetimes set penalty=penalty+1 where race_id=? and pilot_id=? and round=?";
		String[] data2 = {Integer.toString(race_id), Integer.toString(pilot_id), Integer.toString(round)};
		database.execSQL(sql, data2);
	}

	public void decPenalty(int race_id, int pilot_id, int round){
		String sql;
		
		sql = "update racetimes set penalty=penalty-1 where race_id=? and pilot_id=? and round=?";
		String[] data2 = {Integer.toString(race_id), Integer.toString(pilot_id), Integer.toString(round)};
		database.execSQL(sql, data2);
	}

	public void setRetired(boolean toggle, int race_id, int round, int pilot_id){
		String sql;
		
		sql = "update racepilots set status=? where race_id=? and id=?";
		int status = toggle ? Pilot.STATUS_RETIRED : Pilot.STATUS_NORMAL;
		String[] data = {Integer.toString(status), Integer.toString(race_id), Integer.toString(pilot_id)};
		database.execSQL(sql, data);
	}

	public long addPilot(Pilot p, int race_id) {
		ContentValues values = new ContentValues();
		values.putNull("id");			// this is the id id in the race - also the running order
		values.put("pilot_id", p.id); 	// passed in id becomes pilot_id
		values.put("race_id", race_id);
		values.put("status", p.status);
		values.put("firstname", p.firstname);
		values.put("lastname", p.lastname);
		values.put("email", p.email);
		values.put("frequency", p.frequency);
		values.put("models", p.models);
		values.put("nationality", p.nationality);
		values.put("language", p.language);
		if (p.team != null) values.put("team", p.team);
		long insert_id = database.replace("racepilots", null, values);

		return insert_id;
	}

	public void importPilot(Pilot p, int race_id) {
		p.race_id = race_id;

		ContentValues values = new ContentValues();
		values.put("pilot_id", p.pilot_id);
		values.put("race_id", p.race_id);
		values.put("status", p.status);
		values.put("firstname", p.firstname);
		values.put("lastname", p.lastname);
		values.put("email", p.email);
		values.put("frequency", p.frequency);
		values.put("models", p.models);
		values.put("nationality", p.nationality);
		values.put("language", p.language);
		if (p.team != null) values.put("team", p.team);

		String[] cols = {"id"};
		String where = "race_id=? and pilot_id=?";
		String[] data = {Integer.toString(p.race_id), Integer.toString(p.pilot_id)};
		Cursor cursor = database.query("racepilots", cols, where, data, null, null, null);
		cursor.moveToFirst();
		// set the id in the race - also the running order
		if (!cursor.isAfterLast()) {
			values.put("id", cursor.getInt(0));
		} else {
			values.putNull("id");
		}
		cursor.close();
		long insert_id = database.replace("racepilots", null, values);

		if (-1 != insert_id) {
			p.id = (int) insert_id;
			addRaceTime(p);
		}
	}

	public void addRaceTime(Pilot p) {
		ContentValues values1 = new ContentValues();
		values1.putNull("id");
		values1.put("pilot_id", p.id);
		values1.put("race_id", p.race_id);
		values1.put("round", p.round);
		values1.put("group_nr", p.group);
		values1.put("start_pos", p.start_pos);
		if (!Float.isNaN(p.time)) values1.put("time", p.time);
		else values1.putNull("time");
		values1.put("reflight", (p.status &Pilot.STATUS_REFLIGHT) == Pilot.STATUS_REFLIGHT);
		values1.put("points", p.points);
		values1.put("penalty", p.penalty);
		database.insert("racetimes", null, values1);
	}

	public void updateRacePilotPos(Pilot p, int race_id, int round){
		ContentValues values = new ContentValues();
		values.put("start_pos", p.start_pos);
		values.put("group_nr", p.group);
		String[] data = {Integer.toString(race_id), Integer.toString(round), Integer.toString(p.id)};
		if (0 == database.update("racetimes", values, "race_id=? and round=? and pilot_id=?", data)) {
			// insert new row, if update couldn't change anything
			values.putNull("id");
			values.put("pilot_id", p.id);
			values.put("race_id", race_id);
			values.put("round", round);
			values.put("penalty", p.penalty);
			values.put("reflight", (p.status &Pilot.STATUS_REFLIGHT) == Pilot.STATUS_REFLIGHT);
			database.insert("racetimes", null, values);
		}
	}

	public void updateStartPos(int pilot_id, int race_id, int round, int start_pos){
		String sql = "update racetimes set start_pos=? where race_id=? and round=? and pilot_id=?";
		String[] data = {Integer.toString(start_pos), Integer.toString(race_id), Integer.toString(round), Integer.toString(pilot_id)};
		database.execSQL(sql, data);
	}

	public void updatePoints(int pilot_id, int race_id, int round, float points){
		String sql = "update racetimes set points=? where race_id=? and round=? and pilot_id=?";
		String[] data = {Float.toString(points), Integer.toString(race_id), Integer.toString(round), Integer.toString(pilot_id)};
		database.execSQL(sql, data);
	}

	public void updateGroup(int pilot_id, int race_id, int round, int group){
		String sql = "update racetimes set group_nr=? where race_id=? and round=? and pilot_id=?";
		String[] data = {Integer.toString(group), Integer.toString(race_id), Integer.toString(round), Integer.toString(pilot_id)};
		database.execSQL(sql, data);
	}

	/**
	 * On next round start_pos is not yet set. So the pilots are returned ordered by racepilots.id.
	 * Now the initial order for this new round has to be set according to race offset and start_number values.
	 **/
	public void setStartPos(int race_id, int round, int offset, int start_number, boolean forceOverwrite) {
		// Shuffle according to offset and round or start_plot (start_number takes priority!)
		ArrayList<Pilot> allPilots = getAllPilotsForRace(race_id, round);
		int numPilots = allPilots.size();
		if (numPilots > 0) {
			/* sort by racepilots.id */
			Collections.sort(allPilots);

			/* now calculate the offset into that list */
			int rotate_count;
			if (start_number > 0) {
				rotate_count = start_number - 1;
			} else {
				rotate_count = (((round - 1) * offset) % numPilots);
			}
			
			/* overwrite existing start positions if not valid */
			if (!forceOverwrite) {
				for (int i = 0; i < numPilots; i++) {
					if (allPilots.get(i).start_pos == 0 || allPilots.get(i).start_pos > numPilots) {
						forceOverwrite = true;
					}
				}
			}
			
			/* Commit the pilots list only, if start order is changed systematically.
			 * Don't touch it, if it was predefined by import. */
			if (start_number != 0 || offset != 0 || forceOverwrite) {
				// move all pilots before start number to end or rotate offset times
				for (int i=0; i<rotate_count; i++){
					allPilots.add(allPilots.get(0));
					allPilots.remove(0);
				}

				for (int i = 0; i < numPilots; i++) {
					allPilots.get(i).start_pos = i + 1;
					updateRacePilotPos(allPilots.get(i), race_id, round);
				}
			}
		}
	}

	public void updatePilot(Pilot p) {
		ContentValues values = new ContentValues();
		values.put("firstname", p.firstname);
		values.put("lastname", p.lastname);
		values.put("email", p.email);
		values.put("frequency", p.frequency);
		values.put("models", p.models);
		values.put("nationality", p.nationality);
		values.put("language", p.language);
		if (p.team != null) values.put("team", p.team);
		database.update("racepilots", values, "id="+Integer.toString(p.id), null);
	}

	public void deletePilot(int id) {
		database.delete("racepilots", "id = '" + Integer.toString(id) + "'", null);
	}

    public void deleteAllPilots(int race_id) {
        database.delete("racepilots", "race_id = '" + Integer.toString(race_id) + "'", null);
    }

	public void deleteRound(int race_id, int round){
		database.delete("racetimes", "race_id = '" + Integer.toString(race_id) + "' and round='" + Integer.toString(round) + "'", null);
	}

    public void deleteGroup(int race_id, int round, int position, ArrayList<Integer> groups, ArrayList<Pilot> pilots){
		if (position>=0) { // Safety net to prevent crash
			int group = groups.get(position);
			while (position>=0 && group == groups.get(position)) {
				int pilot_id = pilots.get(position).id;
				database.delete("racetimes", "race_id = '" + Integer.toString(race_id) + "' and round='" + Integer.toString(round) + "' and pilot_id='" + Integer.toString(pilot_id) + "'", null);
				position--;
			}
		}
    }

	public ArrayList<Pilot> getAllPilotsForRace(int race_id, int round) {
		ArrayList<Pilot> allPilots = new ArrayList<>();
		String sql = "select rp.id, rp.pilot_id, rp.race_id, rp.status," +
					 " rp.firstname, rp.lastname, rp.email, rp.frequency, rp.models, rp.nationality, rp.language," +
					 //" p.firstname, p.lastname, p.email, rp.frequency, rp.models, p.nationality, p.language," +
					 " rp.team," +
					 " rt.time, rt.penalty, rt.reflight, rt.group_nr, rt.points, rt.start_pos " +
		 			 "from racepilots rp " +
					 "left join racetimes rt " +
					 "on (rt.race_id=rp.race_id and rt.pilot_id=rp.id and rt.round=?) " +
					 //"left join pilots p " +
					 //"on (rp.pilot_id=p.id) " +
					 "where rp.race_id=? " +
					 "order by rt.start_pos, rp.id";
					 //"order by case rt.start_pos when 0 then 65535 else rt.start_pos end, rp.id";
		String[] data = {Integer.toString(round), Integer.toString(race_id)};
		Cursor cursor = database.rawQuery(sql, data);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Pilot p = cursorToPilot(cursor, true);
			p.round = round;
			//Log.d("RACEPILOTDATA", "PILOT:"+p.toString());
			allPilots.add(p);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return allPilots;
	}

	public String[] getTeams(int race_id){
		ArrayList<String> allTeams = new ArrayList<>();
		String sql = "select team from racepilots p  where p.race_id=? order by id";
		String[] data = {Integer.toString(race_id)};
		Cursor cursor = database.rawQuery(sql, data);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			String team = cursor.getString(0);
			if (!allTeams.contains(team))
				allTeams.add(team);

			cursor.moveToNext();
		}
		cursor.close();

		Collections.sort(allTeams, new Comparator<String>() {
			@Override
			public int compare(String a, String b) {
				return a.compareTo(b);
			}
		});

		return allTeams.toArray(new String[0]);
	}

	private Pilot cursorToPilot(Cursor cursor, boolean full) {
		Pilot p = new Pilot();
		p.id = cursor.getInt(0);
		p.pilot_id = cursor.getInt(1);
		p.race_id = cursor.getInt(2);
		p.status = cursor.getInt(3);
		p.firstname = cursor.getString(4);
		p.lastname = cursor.getString(5);
		p.email = cursor.getString(6);
		p.frequency = cursor.getString(7);
		p.models = cursor.getString(8);
		p.nationality = cursor.getString(9);
		p.language = cursor.getString(10);
		p.team = cursor.getString(11);
		if (full){
			String timeStr = cursor.getString(12);
			if (timeStr == null || timeStr.equals("")) p.time = Float.NaN;
			else p.time = Float.valueOf(timeStr);
			p.flown = !Float.isNaN(p.time);
			p.penalty = cursor.getInt(13);
            if (cursor.getInt(14) == 1){
                p.status = Pilot.STATUS_REFLIGHT;
                p.time = Float.NaN;
                p.flown = false;
            }
			p.group = cursor.getInt(15);
			p.points = cursor.getFloat(16);
			p.start_pos = cursor.getInt(17);
		}
		return p;
	}

    public String getPilotsSerialized(int race_id){
        ArrayList<Pilot> pilots = getAllPilotsForRace(race_id, 0);
        StringBuilder array = new StringBuilder("[");
        for(int i=0;i<pilots.size(); i++){
            if (i>0) array.append(",");
            Pilot p = pilots.get(i);
            String str_pilot = String.format("{\"id\":\"%d\", \"pilot_id\":\"%d\", \"status\":\"%d\", \"firstname\":\"%s\", \"lastname\":\"%s\", \"email\":\"%s\", \"frequency\":\"%s\", \"models\":\"%s\", \"nationality\":\"%s\", \"language\":\"%s\", \"team\":\"%s\"}", p.id, p.pilot_id, p.status, p.firstname, p.lastname, p.email, p.frequency, p.models, p.nationality, p.language, p.team);
            array.append(str_pilot);
        }
        array.append("]");
        
        return array.toString();
    }

    public String getTimesSerialized(int id, int round){
        String array = "[";
        for (int i=0;i<round; i++){
            if (i>0) array+=",";
            array+="[";
            ArrayList<Pilot> pilots = getAllPilotsForRace(id, i+1);
            for(int j=0;j<pilots.size(); j++) {
                if (j > 0) array += ",";
                Pilot p = pilots.get(j);
                String str_pilot = String.format("{\"time\":\"%s\", \"flown\":\"%d\", \"penalty\":\"%d\"}", p.time, (p.flown)?1:0, p.penalty);
                array+= str_pilot;
            }
            array+="]";
        }
        array+="]";
        return array;

    }

	public String getTimesSerializedExt(int race_id, int round){
		StringBuilder array = new StringBuilder("[");
		for (int i=0;i<round; i++){
			ArrayList<Pilot> allPilots = getAllPilotsForRace(race_id, i+1);

			/* split into groups */
			ArrayList<ArrayList<Pilot>> groups = new ArrayList<>();
			ArrayList<Pilot> group_pilots = new ArrayList<>();
			groups.add(group_pilots);
			int last_group = 1;
			for(int j=0;j<allPilots.size(); j++) {
				Pilot p = allPilots.get(j);
				if (p.group > 0 && p.group != last_group) {
					while (groups.size() < p.group) {
						groups.add(new ArrayList<Pilot>());
					}
					group_pilots = groups.get(p.group - 1);
					last_group = p.group;
				}
				group_pilots.add(p);
			}

			/* generate extended strings for all pilots per group */
			if (i>0) array.append(",");
			array.append("[");
			for(int j=0; j<groups.size(); j++) {
				if (j > 0) array.append(",");
				array.append("[");
				group_pilots = groups.get(j);
				for(int k=0; k<group_pilots.size(); k++) {
					if (k > 0) array.append(",");
					Pilot p = group_pilots.get(k);
					String str_pilot = String.format("{\"id\":\"%d\", \"group\":\"%d\", \"start_pos\":\"%d\", \"flown\":\"%d\", \"time\":\"%s\", \"penalty\":\"%d\", \"points\":\"%s\"}", p.id, p.group, p.start_pos, (p.flown) ? 1 : 0, String.format("%.2f",p.time).replace(",","."), p.penalty, String.format("%f",p.points).replace(",","."));
					array.append(str_pilot);
				}
				array.append("]");
			}
			array.append("]");
		}
		array.append("]");
		return array.toString();
	}
}
