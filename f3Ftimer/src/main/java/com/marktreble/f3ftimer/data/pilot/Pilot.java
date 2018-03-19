package com.marktreble.f3ftimer.data.pilot;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

import org.json.JSONObject;

public class Pilot {
	
	public static Integer STATUS_NORMAL = 1;
	public static Integer STATUS_REFLIGHT = 2;
	public static Integer STATUS_RETIRED = 4;
	public static Integer STATUS_FLOWN = 8;
	
	public int id = 0;
	public int pilot_id;
	public int race_id;
	public int status = Pilot.STATUS_NORMAL;
	public String firstname = "";
	public String lastname = "";
	public String email = "";
	public String frequency = "";
	public String models = "";
	public String nationality = "";
	public String language = "";
	public float time=-1;
	public boolean flown;
	public Integer penalty = 0;
	public float raw_points = -1; // Points before penalty is applied (Used in discards calculation)
	public float points = -1;
	public Integer position = 0;
	public Integer round = 0;
	public String number = "";
	public String team = "";
	public String nac_no = "";
	public String fai_id = "";
	public Integer group = 0;

    public Pilot(){
        // Default constructor
    }

    public Pilot(JSONObject o){
        if (o.has("pilot_id")) id = Integer.parseInt(o.optString("pilot_id"));
        if (o.has("status")) status = Integer.parseInt(o.optString("status"));
        firstname = o.optString("firstname");
        lastname = o.optString("lastname");
        email = o.optString("email");
        frequency = o.optString("frequency");
        models = o.optString("models");
        nationality = o.optString("nationality");
		language = o.optString("language");
		team = o.optString("team");
		nac_no = o.optString("nac_no", "");
		fai_id = o.optString("fai_id", "");
    }
    
	public String get(){
		String str = "";
		str+=firstname+" ";
		str+=Integer.toString(penalty) + " ";
		return str;
	}
	
	public Drawable getFlag(Context context){
		if (nationality == null) return null;
		String uri = "@drawable/" + nationality.toLowerCase();
		int imageResource = context.getResources().getIdentifier(uri, null, context.getPackageName());
		if (imageResource == 0)	return null;
		return ContextCompat.getDrawable(context, imageResource);
	}
    
    public String toString(){
        return String.format("{\"id\":\"%s\", \"race_id\":\"%s\", \"pilot_id\":\"%d\", \"status\":\"%d\", \"firstname\":\"%s\", \"lastname\":\"%s\", \"nationality\":\"%s\", \"language\":\"%s\", \"nac_no\":\"%s\", \"fai_id\":\"%s\", \"team\":\"%s\"}", id, race_id, pilot_id, status, firstname, lastname, nationality, language, nac_no, fai_id, team );
        
    }

	public String toExtendedString(){
		return String.format("{\"id\":\"%s\", \"pilot_id\":\"%d\", \"firstname\":\"%s\", \"lastname\":\"%s\", \"time\":\"%s\", \"points\":\"%s\", \"penalty\":\"%s\"}", id, pilot_id, firstname, lastname, time, points, penalty );

	}

}
