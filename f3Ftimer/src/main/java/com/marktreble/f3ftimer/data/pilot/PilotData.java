package com.marktreble.f3ftimer.data.pilot;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class PilotData {

	// Database fields
	private SQLiteDatabase database;
	private PilotDatabase db;
	private String[] allColumns = { "id", "firstname", "lastname", "email", "frequency", "models", "nationality", "language" };

	public PilotData(Context context) {
		db = new PilotDatabase(context);
	}

	public void open() throws SQLException {
		database = db.getWritableDatabase();
	}

	public void close() {
		db.close();
	}

	public Pilot getPilot(int id) {
		Cursor cursor = database.query("pilots", allColumns, "id = '" + Integer.toString(id) + "'", null, null, null, null);
		if (cursor.getCount() == 0) return new Pilot(); // Return a blank object if not found
		cursor.moveToFirst();
		Pilot p = cursorToPilot(cursor);
		cursor.close();
		return p;
	}


	public void savePilot(Pilot p) {
		ContentValues values = new ContentValues();
		values.putNull("id");
		values.put("firstname", p.firstname);
		values.put("lastname", p.lastname);
		values.put("email", p.email);
		values.put("frequency", p.frequency);
		values.put("models", p.models);
		values.put("nationality", p.nationality);
		values.put("language", p.language);
		database.insert("pilots", null, values);
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
		database.update("pilots", values, "id="+Integer.toString(p.id), null);
	}

	public void deletePilot(int id) {
		database.delete("pilots", "id = '" + Integer.toString(id) + "'", null);
	}

	public ArrayList<Pilot> getAllPilots() {
		ArrayList<Pilot> allPilots = new ArrayList<>();
		Cursor cursor = database.query("pilots", allColumns, null, null, null, null, "lastname");
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Pilot p = cursorToPilot(cursor);
			allPilots.add(p);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return allPilots;
	}

	private Pilot cursorToPilot(Cursor cursor) {
		Pilot p = new Pilot();
		p.id = cursor.getInt(0);
		p.firstname = cursor.getString(1);
		p.lastname = cursor.getString(2);
		p.email = cursor.getString(3);
		p.frequency = cursor.getString(4);
		p.models = cursor.getString(5);
		p.nationality = cursor.getString(6);
		p.language = cursor.getString(7);
		return p;
	}

	public ArrayList<Pilot> getAllPilotsExcept(ArrayList<Pilot> arrPilots) {

		ArrayList<Pilot> allPilots = new ArrayList<>();
		Cursor cursor = database.query("pilots", allColumns, null, null, null, null, "lastname,firstname");
		cursor.moveToFirst();
		boolean excepted;
		while (!cursor.isAfterLast()) {
			// Compare names as names may be imported and ids won't match
			Pilot p = cursorToPilot(cursor);
			excepted = false;
			for (Pilot pe : arrPilots) { // pe excepted pilot
				if ((pe.firstname.equals(p.firstname) && pe.lastname.equals(p.lastname))) {
					excepted = true;
				}
			}
			if (!excepted) allPilots.add(p);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();

		return allPilots;
	}
	
}
