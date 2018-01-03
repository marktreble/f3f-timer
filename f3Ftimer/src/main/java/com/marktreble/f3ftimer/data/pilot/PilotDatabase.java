package com.marktreble.f3ftimer.data.pilot;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class PilotDatabase extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "pilots.db";
	private static final int DATABASE_VERSION = 3;
    
	private static final String PILOTS_TABLE_CREATE = "create table pilots " +
			 "(id integer primary key," +
			 " firstname tinytext," +
			 " lastname tinytext," +
			 " email tinytext," +
			 " frequency tinytext," +
			 " models tinytext," +
			 " nationality tinytext," +
			 " language tinytext," +
			 " team tinytext);";

    public PilotDatabase(Context context){
    	super(context, DATABASE_NAME, null, DATABASE_VERSION);	
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(PILOTS_TABLE_CREATE);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(PilotDatabase.class.getName(),
				"Upgrading database from version " + oldVersion + " to " + newVersion);
		
		String sql;
		if (newVersion>1 && oldVersion<=1){
			sql = "alter table pilots add nationality tinytext";
			db.execSQL(sql);
			sql = "alter table pilots add language tinytext";
			db.execSQL(sql);
		}

		if (newVersion>2 && oldVersion<=2){
			sql = "alter table pilots add team tinytext";
			db.execSQL(sql);
		}
    }
}
