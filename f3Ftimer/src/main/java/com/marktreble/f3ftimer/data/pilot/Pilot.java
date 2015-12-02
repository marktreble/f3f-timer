package com.marktreble.f3ftimer.data.pilot;

import android.content.Context;
import android.graphics.drawable.Drawable;

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
	public float points = -1;
	public Integer position = 0;
	public Integer round = 0;
	public String number = "";

    public Pilot(){
        // Default constructor
    }

    public Pilot(JSONObject o){
        id = Integer.parseInt(o.optString("pilot_id"));
        status = Integer.parseInt(o.optString("status"));
        firstname = o.optString("firstname");
        lastname = o.optString("lastname");
        email = o.optString("email");
        frequency = o.optString("frequency");
        models = o.optString("models");
        nationality = o.optString("nationality");
        language = o.optString("language");
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
		return context.getResources().getDrawable(imageResource);
	}
    
    public String toString(){
        return String.format("{\"id\":\"%s\", \"race_id\":\"%s\", \"pilot_id\":\"%d\", \"status\":\"%d\", \"firstname\":\"%s\", \"lastname\":\"%s\", \"nationality\":\"%s\", \"language\":\"%s\"}", id, race_id, pilot_id, status, firstname, lastname, nationality, language );
        
    }
}
