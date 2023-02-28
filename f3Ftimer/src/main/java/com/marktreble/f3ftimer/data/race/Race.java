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

import androidx.annotation.NonNull;

import org.json.JSONObject;

public class Race {

    private static final int STATUS_PENDING = 0;
    public static final int STATUS_IN_PROGRESS = 1;
    public static final int STATUS_COMPLETE = 2;

    public Integer id = 0;
    public String name = "";
    public Integer type = 1;
    public Integer offset = 0;
    public Integer status = STATUS_PENDING;
    public Integer round = 1;
    public Integer rounds_per_flight = 1;
    public Integer start_number = 0;
    public Integer race_id = 0;

    @NonNull
    public String toString() {
        return String.format("{\"name\":\"%s\",\"type\":\"%d\",\"offset\":\"%d\",\"status\":\"%d\",\"round\":\"%d\",\"rounds_per_flight\":\"%d\",\"start_number\":\"%d\",\"race_id\":\"%d\"}", name, type, offset, status, round, rounds_per_flight, start_number, race_id);
    }

    public Race() {
        // Default Constructor
    }

    public Race(JSONObject o) {
        name = o.optString("name");
        type = Integer.parseInt(o.optString("type"));
        offset = Integer.parseInt(o.optString("offset"));
        status = Integer.parseInt(o.optString("status"));
        round = Integer.parseInt(o.optString("round"));
        String rpf = o.optString("rounds_per_flight");
        if (rpf.equals("")) rpf = "1";
        rounds_per_flight = Integer.parseInt(rpf);
        String sn = o.optString("start_number");
        if (sn.equals("")) sn = "0";
        start_number = Integer.parseInt(sn);
        String rid = o.optString("race_id");
        if (rid.equals("")) rid = "0";
        race_id = Integer.parseInt(rid);

    }
}
