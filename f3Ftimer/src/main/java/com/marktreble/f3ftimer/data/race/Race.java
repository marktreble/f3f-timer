package com.marktreble.f3ftimer.data.race;

import org.json.JSONObject;

public class Race {

	public static final int TYPE_RACE = 1;
	public static final int TYPE_PRACTICE = 2;

	public static final int STATUS_PENDING = 0;
	public static final int STATUS_IN_PROGRESS = 1;
	public static final int STATUS_COMPLETE = 2;

	public Integer id = 0;	
	public String name = "";
	public Integer type = TYPE_RACE; 
	public Integer offset = 0;
	public Integer status = STATUS_PENDING;
    public Integer round = 1;
    public Integer rounds_per_flight = 1;
	public Integer start_number = 0;

	public final static String translateStatus(int status){
		String result = "";
		switch (status){
			case STATUS_PENDING:
				result = "Pending";
			break;
			case STATUS_IN_PROGRESS:
				result = "In Progress";
			break;
			case STATUS_COMPLETE:
				result = "Complete";
			break;
		}
		return result;
	}
    
    public String toString(){
        return String.format("{\"name\":\"%s\",\"type\":\"%d\",\"offset\":\"%d\",\"status\":\"%d\",\"round\":\"%d\",\"rounds_per_flight\":\"%d\",\"start_number\":\"%d\"}", name, type, offset, status, round, rounds_per_flight, start_number);
    }
    
    public Race(){
         // Default Constructor
    }
    
    public Race(JSONObject o){
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

    }
}
