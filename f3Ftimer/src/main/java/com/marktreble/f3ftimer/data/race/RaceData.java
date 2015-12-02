package com.marktreble.f3ftimer.data.race;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
//import android.util.Log;

public class RaceData {

	// Database fields
	private SQLiteDatabase database;
	private RaceDatabase db;

	public RaceData(Context context) {
		db = new RaceDatabase(context);
	}

	public void open() throws SQLException {
		database = db.getWritableDatabase();
	}

	public void close() {
		db.close();
	}

	public Race getRace(int id) {
		Cursor cursor = database.query("races", null, "id = '" + Integer.toString(id) + "'", null, null, null, null);
		if (cursor.getCount() == 0){
			cursor.close();
			return new Race(); // Return a blank object if not found
		}
		cursor.moveToFirst();
		Race r = cursorToRace(cursor);
		cursor.close();
		return r;
	}


	public long saveRace(Race r) {
		ContentValues values = new ContentValues();
		values.putNull("id");
		values.put("name", r.name);
		values.put("type", r.type);
		values.put("offset", r.offset);
		values.put("status", r.status);
        values.put("round", r.round);
		values.put("rounds_per_flight", r.rounds_per_flight);
		values.put("start_number", r.start_number);
		return database.insert("races", null, values);
	}

	public Race nextRound(int race_id){
		String sql = "update races set round=round+max(1,rounds_per_flight) where id=?";
		String[] data = {Integer.toString(race_id)};
		database.execSQL(sql, data);
		return  getRace(race_id);
	}

	public void setStatus(int race_id, int status) {
		String sql = "update races set status=? where id=?";
		String[] data = {Integer.toString(status), Integer.toString(race_id)};
		database.execSQL(sql, data);
	}

	public void deleteRace(int id) {
		database.delete("races", "id = '" + Integer.toString(id) + "'", null);
		database.delete("racepilots", "race_id = '" + Integer.toString(id) + "'", null);
        database.delete("racetimes", "race_id = '" + Integer.toString(id) + "'", null);
        database.delete("racegroups", "race_id = '" + Integer.toString(id) + "'", null);
	}

    public void deleteRound(int race_id, int round_id){
        database.delete("racegroups", "race_id = '" + Integer.toString(race_id) + "' and round = '" + Integer.toString(round_id) + "'", null);
    }

    public void setGroups(int race_id, int round_id, int num_groups){
        deleteRound(race_id, round_id);
        ContentValues values = new ContentValues();
        values.putNull("id");
        values.put("race_id", race_id);
        values.put("round", round_id);
        values.put("groups", num_groups);
        database.insert("racegroups", null, values);
    }

	public void setStartNumber(int race_id, int start_number){
		String sql = "update races set start_number=? where id=?";
		String[] data = {Integer.toString(start_number), Integer.toString(race_id)};
		database.execSQL(sql, data);
	}

    public int getGroups(int race_id, int round_id){
        String[] cols = {"groups"};
        Cursor cursor = database.query("racegroups", cols, "race_id = '" + Integer.toString(race_id) + "' and round = '" + Integer.toString(round_id) + "'", null, null, null, null);
        if (cursor.getCount() == 0){
            cursor.close();
            return 1; // Return a single group if no entry (not group scored!)
        }
        cursor.moveToFirst();
        int num_groups = cursor.getInt(0);
        if (num_groups<1) num_groups = 1; // Just in case some numpty puts in 0 or empty value
        cursor.close();
        return num_groups;
    }


	public ArrayList<Race> getAllRaces() {
		ArrayList<Race> allPilots = new ArrayList<Race>();
		Cursor cursor = database.query("races", null, null, null, null, null, "name");
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Race p = cursorToRace(cursor);
			allPilots.add(p);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return allPilots;
	}

	private Race cursorToRace(Cursor cursor) {
		Race r = new Race();
		r.id = cursor.getInt(0);
		r.name = cursor.getString(1);
		r.type = cursor.getInt(2);
		r.offset = cursor.getInt(3);
		r.status = cursor.getInt(4);
        r.round = cursor.getInt(5);
        r.rounds_per_flight = (cursor.getInt(6)>0)?cursor.getInt(6):1;
		r.start_number = cursor.getInt(7);
		return r;
	}
    
    public String getSerialized(int id){
        Race r = getRace(id);
        return r.toString();
    }

    public String getGroupsSerialized(int id, int round){
        String array = "[";
        for (int i=0;i<round; i++){
            if (i>0) array+=",";
            int groups = getGroups(id, i);
            array+=Integer.toString(groups);
        }
        array+="]";
        return array;
    }
	
}
