package com.marktreble.f3ftimer.data.racepilot;

import java.util.ArrayList;

import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceDatabase;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class RacePilotData {

	// Database fields
	private SQLiteDatabase database;
	private RaceDatabase db;

	public RacePilotData(Context context) {
		db = new RaceDatabase(context);
	}

	public void open() throws SQLException {
		database = db.getWritableDatabase();
	}

	public void close() {
		db.close();
	}

	public Pilot getPilot(int id, int race){
		String sql = "select * from racepilots where id=? and race_id=?";
		String[] data = {Integer.toString(id), Integer.toString(race)};
		Cursor cursor = database.rawQuery(sql, data);
		if (cursor.getCount() == 0){
			cursor.close();
			return new Pilot(); // Return a blank object if not found
		}
		cursor.moveToFirst();
		Pilot p = cursorToPilot(cursor, false);
		cursor.close();
		return p;
	}

    
	public float getPilotTimeInRound(int race_id, int pilot_id, int round){
		String where = "pilot_id=" + Integer.toString(pilot_id) + " and race_id=" + Integer.toString(race_id) + " and round=" + Integer.toString(round) + " and reflight is null or reflight=0";
		String[] cols = {"time"};
		Cursor cursor = database.query("racetimes", cols, where, null, null, null, null);
		if (cursor.getCount() == 0){
			cursor.close();
			return 0;
		}
		cursor.moveToFirst();
		float time = cursor.getFloat(0);

		cursor.close();
		return time;
	}

	public void setPilotTimeInRound(int race_id, int pilot_id, int round, float time){
		database.delete("racetimes", "race_id = '" + Integer.toString(race_id) + "' and pilot_id='" + Integer.toString(pilot_id) + "' and round='" + Integer.toString(round) + "'", null);
		
		String sql;
		
		ContentValues values = new ContentValues();
		values.putNull("id");			// this is the id id in the race - also the running order
		values.put("pilot_id", pilot_id); 	// passed in id becomes pilot_id
		values.put("race_id", race_id);
		values.put("round", round);
		values.put("time", time);
        values.put("penalty", 0);
		database.insert("racetimes", null, values);

		sql = "update racepilots set status=? where race_id=? and id=?";
		String[] data2 = {Integer.toString(Pilot.STATUS_NORMAL), Integer.toString(race_id), Integer.toString(pilot_id)};
		database.execSQL(sql, data2);

	}

	public void setReflightInRound(int race_id, int pilot_id, int round){
		String sql;
		
		sql = "update racetimes set reflight=1 where race_id=? and pilot_id=? and round=?";
		String[] data = {Integer.toString(race_id), Integer.toString(pilot_id), Integer.toString(round)};
		database.execSQL(sql, data);
		
        /*
		sql = "update racepilots set status=? where race_id=? and id=?";
		String[] data2 = {Integer.toString(Pilot.STATUS_REFLIGHT), Integer.toString(race_id), Integer.toString(pilot_id)};
		database.execSQL(sql, data2);
		*/
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

	public void setRetired(boolean toggle, int race_id, int pilot_id){
		String sql;
		
		sql = "update racepilots set status=? where race_id=? and id=?";
		String status = (toggle) ? Integer.toString(Pilot.STATUS_RETIRED) : Integer.toString(Pilot.STATUS_NORMAL);
		String[] data2 = {status, Integer.toString(race_id), Integer.toString(pilot_id)};
		database.execSQL(sql, data2);
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
		return database.insert("racepilots", null, values);
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
        int group = groups.get(position);
        while (group == groups.get(position)) {
            int pilot_id = pilots.get(position).id;
            database.delete("racetimes", "race_id = '" + Integer.toString(race_id) + "' and round='" + Integer.toString(round) + "' and pilot_id='" + Integer.toString(pilot_id) + "'", null);
            position--;
        }
    }

    public ArrayList<Pilot> getAllPilotsForRace(int race_id, int round, int offset) {
		ArrayList<Pilot> allPilots = new ArrayList<>();
		String sql = "select *," +
					 "(select time from racetimes rt where rt.pilot_id=p.id and rt.round=? and rt.race_id=?) as time, " +
					 "(select count(id) from racetimes rt where rt.pilot_id=p.id and rt.round=? and rt.race_id=?) as flown, " +
                     "(select penalty from racetimes rt where rt.pilot_id=p.id and rt.round=? and rt.race_id=?) as penalty, " +
                     "(select reflight from racetimes rt where rt.pilot_id=p.id and rt.round=? and rt.race_id=?) as reflight " +
					 "from racepilots p  where p.race_id=?";
		String[] data = {Integer.toString(round), Integer.toString(race_id), Integer.toString(round), Integer.toString(race_id), Integer.toString(round), Integer.toString(race_id), Integer.toString(round), Integer.toString(race_id), Integer.toString(race_id)};
		Cursor cursor = database.rawQuery(sql, data);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Pilot p = cursorToPilot(cursor, true);

			allPilots.add(p);
			cursor.moveToNext();
		}
		// Shuffle according to offset and round
		int numPilots = allPilots.size();
		int start = 0;
		if (numPilots>0)
			start = ((round-1) * offset) % numPilots;
		
		// move all pilots before start number to end
		for (int i=0; i<start; i++){
			allPilots.add(allPilots.get(0));
			allPilots.remove(0);
		}
		
		// Make sure to close the cursor
		cursor.close();
		return allPilots;
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
		if (full){
			p.time = cursor.getFloat(11);
			p.flown = (cursor.getInt(12) == 1);
            p.penalty = cursor.getInt(13);
            if (cursor.getInt(14) == 1){
                p.status = Pilot.STATUS_REFLIGHT;
                p.time = 0;
                p.flown = false;
            }
		}
		return p;
	}

    public String getPilotsSerialized(int id){
        String array = "[";
        ArrayList<Pilot> pilots = getAllPilotsForRace(id, 0, 0);
        for(int i=0;i<pilots.size(); i++){
            if (i>0) array+=",";
            Pilot p = pilots.get(i);
            String str_pilot = String.format("{\"pilot_id\":\"%d\", \"status\":\"%d\", \"firstname\":\"%s\", \"lastname\":\"%s\", \"email\":\"%s\", \"frequency\":\"%s\", \"models\":\"%s\", \"nationality\":\"%s\", \"language\":\"%s\"}", p.pilot_id, p.status, p.firstname, p.lastname, p.email, p.frequency, p.models, p.nationality, p.language);
            array+= str_pilot;
        }
        array+= "]";
        
        return array;
    }

    public String getTimesSerialized(int id, int round){
        String array = "[";
        for (int i=0;i<round; i++){
            if (i>0) array+=",";
            array+="[";
            ArrayList<Pilot> pilots = getAllPilotsForRace(id, i+1, 0);
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

}
