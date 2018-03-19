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
	private String[] allColumns = { "id", "firstname", "lastname", "email", "frequency", "models", "nationality", "language", "nac_no", "fai_id" };

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
		values.put("nac_no", p.nac_no);
		values.put("fai_id", p.fai_id);
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
		values.put("nac_no", p.nac_no);
		values.put("fai_id", p.fai_id);
		database.update("pilots", values, "id=" + Integer.toString(p.id), null);
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
		p.nac_no = cursor.getString(8);
		p.fai_id = cursor.getString(9);
		if (p.nac_no == null) p.nac_no = "";
		if (p.fai_id == null) p.fai_id = "";
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

	public String getSerialized(){
		String array = "[";
		ArrayList<Pilot> pilots = getAllPilots();
		for(int i=0;i<pilots.size(); i++){
			if (i>0) array+=",";
			Pilot p = pilots.get(i);
			String str_pilot = String.format("{\"id\":\"%d\", \"status\":\"%d\", \"firstname\":\"%s\", \"lastname\":\"%s\", \"email\":\"%s\", \"frequency\":\"%s\", \"models\":\"%s\", \"nationality\":\"%s\", \"language\":\"%s\", \"nac_no\":\"%s\", \"fai_id\":\"%s\"}", p.id, p.status, p.firstname, p.lastname, p.email, p.frequency, p.models, p.nationality, p.language, p.nac_no, p.fai_id);
			array+= str_pilot;
		}
		array+= "]";

		return array;
	}

	public String getCSV(){
		String csv = "";
		ArrayList<Pilot> pilots = getAllPilots();
		for(int i=0;i<pilots.size(); i++){
			if (i>0) csv+="\r\n";
			Pilot p = pilots.get(i);
			String str_pilot = String.format("%s, %s, %s, %s, %s, %s, %s, %s, %s, %s", p.firstname, p.lastname, p.nationality, p.language,  p.team, p.frequency, p.models, p.email, p.nac_no, p.fai_id);
			csv+= str_pilot;
		}

		return csv;
	}
	
}
