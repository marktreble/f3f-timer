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

package com.marktreble.f3ftimer.data.race;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class RaceDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "races.db";
    private static final int DATABASE_VERSION = 11;

    private static final String RACE_TABLE_CREATE = "create table races " +
            "(id integer primary key," +
            " name tinytext," +
            " type integer," +
            " `offset` integer," +
            " status integer," +
            " round integer," +
            " rounds_per_flight integer," +
            " start_number integer," +
            " race_id integer);";

    private static final String RACEPILOTS_TABLE_CREATE = "create table racepilots " +
            "(id integer primary key," +
            " pilot_id integer," +
            " race_id integer," +
            " status integer," +
            " firstname tinytext," +
            " lastname tinytext," +
            " email tinytext," +
            " frequency tinytext," +
            " models tinytext," +
            " nationality tinytext," +
            " language tinytext, " +
            " team tinytext, " +
            " nac_no tinytext, " +
            " fai_id tinytext);";

    private static final String RACETIMES_TABLE_CREATE = "create table racetimes " +
            "(id integer primary key," +
            " pilot_id integer," +
            " race_id integer," +
            " round integer," +
            " penalty integer," +
            " time float," +
            " reflight integer);";

    private static final String RACEGROUPS_TABLE_CREATE = "create table racegroups " +
            "(id integer primary key," +
            " race_id integer," +
            " round integer," +
            " groups integer," +
            " start_pilot integer);";

    private static final String FASTESTLEGTIMES_TABLE_CREATE = "create table if not exists fastestLegTimes " +
            "(race_id integer," +
            " round integer," +
            " pilot_id integer," +
            " leg0 long," +
            " leg1 long," +
            " leg2 long," +
            " leg3 long," +
            " leg4 long," +
            " leg5 long," +
            " leg6 long," +
            " leg7 long," +
            " leg8 long," +
            " leg9 long," +
            " primary key (race_id, round, pilot_id));";

    private static final String FASTESTFLIGHTTIME_TABLE_CREATE = "create table if not exists fastestFlightTime " +
            "(race_id integer," +
            " round integer," +
            " pilot_id integer," +
            " time float," +
            " primary key (race_id, round, pilot_id));";

    public RaceDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(RACE_TABLE_CREATE);
        db.execSQL(RACEPILOTS_TABLE_CREATE);
        db.execSQL(RACETIMES_TABLE_CREATE);
        db.execSQL(RACEGROUPS_TABLE_CREATE);
        db.execSQL(FASTESTLEGTIMES_TABLE_CREATE);
        db.execSQL(FASTESTFLIGHTTIME_TABLE_CREATE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(RaceDatabase.class.getName(),
                "Upgrading database from version " + oldVersion + " to " + newVersion);

        String sql;
        if (newVersion > 1 && oldVersion <= 1) {
            sql = "alter table racepilots add nationality tinytext";
            db.execSQL(sql);
            sql = "alter table racepilots add language tinytext";
            db.execSQL(sql);
        }

        if (newVersion > 2 && oldVersion <= 2) {
            db.execSQL(RACEGROUPS_TABLE_CREATE);
        }

        if (newVersion > 3 && oldVersion <= 3) {
            sql = "alter table races add rounds_per_flight integer";
            db.execSQL(sql);
        }

        if (newVersion > 4 && oldVersion <= 4) {
            sql = "alter table racetimes add reflight integer";
            db.execSQL(sql);
        }

        if (newVersion > 5 && oldVersion <= 5) {
            sql = "alter table races add start_number integer";
            db.execSQL(sql);
        }
        if (newVersion > 6 && oldVersion <= 6) {
            sql = "alter table racepilots add team tinytext";
            db.execSQL(sql);
        }

        if (newVersion > 7 && oldVersion <= 7) {
            sql = "alter table racegroups add start_pilot integer";
            db.execSQL(sql);
        }

        if (newVersion > 8 && oldVersion <= 8) {
            sql = "alter table races add race_id integer";
            db.execSQL(sql);
        }

        if (newVersion > 9 && oldVersion <= 9) {
            db.execSQL(FASTESTLEGTIMES_TABLE_CREATE);
            db.execSQL(FASTESTFLIGHTTIME_TABLE_CREATE);

        }

        if (newVersion > 10 && oldVersion <= 10) {
            sql = "alter table racepilots add nac_no tinytext";
            db.execSQL(sql);
            sql = "alter table racepilots add fai_id tinytext";
            db.execSQL(sql);
        }


    }
}
