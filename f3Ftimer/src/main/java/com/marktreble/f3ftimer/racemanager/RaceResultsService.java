package com.marktreble.f3ftimer.racemanager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetManager;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class RaceResultsService extends Service {
	
	static boolean DEBUG = true;
	
	ServerSocket mServerSocket;
	Listener mListener;
	Integer mRid;
	
	@Override
    public void onCreate() {
    }
		
	@Override
	public void onDestroy() {
		if (mListener != null)
			mListener.cancel(false);
		
		if (mServerSocket != null){
			try {
				mServerSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    }

    public static boolean stop(RaceActivity context){
        if (context.isServiceRunning("com.marktreble.f3ftimer.racemanager.RaceResultsService")) {
            Intent serviceIntent = new Intent(context, RaceResultsService.class);
            context.stopService(serviceIntent);
            return true;
        }
        return false;
    }
    
    public int onStartCommand(Intent intent, int flags, int startId){
		if (intent == null) return 0;
    	if (intent.hasExtra("com.marktreble.f3ftimer.race_id")){

			Bundle extras = intent.getExtras();
			mRid = extras.getInt("com.marktreble.f3ftimer.race_id");
    	} else {
    		return 0;
    	}
    	
    	mServerSocket = null;
    	mListener = null;
    	try {
    	    mServerSocket = new ServerSocket(8080);
    	} 
    	catch (IOException e) {

    	    System.out.println("Could not listen on port: 8080 " + e.getMessage() + "::" + e.getCause());
    	    return 0;
    	}
    	
    	if (mServerSocket!=null){
   			mListener = new Listener();
   			mListener.execute(mServerSocket);
    	}
       	return (START_STICKY);    	
    }
       
    private class Listener extends AsyncTask<ServerSocket, Integer, Long> {

    	ServerSocket ss;
    	
		@Override
		protected Long doInBackground(ServerSocket... serverSocket) {
			ss = serverSocket[0];
    		Socket clientSocket = null;
	    	try {
	    	    clientSocket = ss.accept();
	    	} 
	    	catch (IOException e) {
	    	    return null;
	    	}
	    	if (clientSocket!=null){
	    		InputStream input = null;
	            OutputStream output = null;
				try {
					byte[] buffer = new byte[1024];
					input = clientSocket.getInputStream();
					input.read(buffer);
					String[] request_headers = parseHeaders(buffer);

					if (request_headers != null){
						String[] req = request_headers[0].split(" ");
					
						if (req.length!=3){
							Log.e("F3fHTTPServerRequest", "Malformed Request");
						} else {
							String request_type = req[0];
							String request_path = req[1];
							
							if (request_path.equals("/")) request_path = "/index.html"; // Default page


							output = clientSocket.getOutputStream();
							
							String ext = ""; 
							String[] parts = request_path.split("\\?");
							String path = parts[0];
							String query = "";
							if (parts.length>1) query = parts[1];
							
				            int i = path.lastIndexOf('.');
				            if (i>0) ext = path.substring(i+1);
							
				            byte[] out;

				            if (ext.equals("jsp")){
				            	out = getDynamicPage(request_type, path, ext, query);
				            } else {
				            	out = getStaticPage(path, ext);
				            }

				            output.write(out);
				            output.close();
						}
					}
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return (long) 1;
	    	}
			return null;
		}
		
		private String[] parseHeaders(byte[] buffer){
			String s = null;
			try {
				s = new String(buffer,"ASCII");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			if (s !=null){
				String[] lines = s.split("\n");
				String line;
				int l = 0;
				String[] headers = new String[lines.length];
				do {
					line = lines[l].trim();
					Log.i("F3fHTTPServerRequest", line);
					headers[l++] = line;
				} while (line.length()>0 && l<lines.length);
			
				return headers;
			}
			return null;
		}
		
		private byte[] getStaticPage(String path, String ext){
			byte[] response = null;
						
			String resourcefile = "public_html"+path;
			Log.i("HTTP REQUEST", resourcefile);
			
			AssetManager am = getAssets();

            String mime_type = this.getMimeTypeForExtension(ext);

            try {       
                InputStream in_s = am.open(resourcefile);
                byte[] contentBytes;

                contentBytes = new byte[in_s.available()];
               	in_s.read(contentBytes);
                String len = Integer.toString(contentBytes.length);
                
               	
                String header;
    	        header = "HTTP/1.1 200 OK\n";
    	        header+= "Content-Type: "+mime_type+"\n";
    	        header+= "Content-Length: "+len+"\n";
    	        header+= "\r\n";

    	        byte[] headerBytes = header.getBytes();
    	        response = new byte[headerBytes.length + contentBytes.length];

    	        System.arraycopy(headerBytes,0,response,0         ,headerBytes.length);
    	        System.arraycopy(contentBytes,0,response,headerBytes.length,contentBytes.length);
                                
            } catch (Exception e) {
            	return this.get404Page();
            }			
			return response;
		}
		
		private byte[] getDynamicPage(String type, String path, String ext, String query){		

			byte[] response = null;
            Method method;
            JSPPages jsp = new JSPPages();
            
            String script = path.replaceAll("/", "_").substring(0, path.length()-(ext.length()+1)).toLowerCase();
                
            try {
            	  Log.i("TRYING METHOD", script);
              	  method = jsp.getClass().getMethod(script, String.class);
              	  
                  try {
                	  response = (byte[])method.invoke(jsp, query);
                  } catch (IllegalArgumentException e) {
                   	  return this.get500Page();
                  } catch (IllegalAccessException e) {
                   	  return this.get500Page();
                  } catch (InvocationTargetException e) {		
                   	  return this.get500Page();
                  }

            } catch (SecurityException e) {
            		Log.i("ERROR", e.getMessage());
             	  return this.get500Page();
            } catch (NoSuchMethodException e) {
               	  return this.get404Page();
            }                
            
			return response;
		}
		private byte[] get404Page(){
			
			String html = "";
	  		
	  		html = "<!DOCTYPE html>\n";
	  		html+= "<head>\n";
	  		html+= "<title>HTTP/1.1 404 Not Found</title>";
	  		html+= "</head>\n";
	  		html+= "<body>\n";
	  		html+= "<h1>HTTP/1.1 404 Not Found</h1>";
	  		html+= "</body>\n";
	  		html+= "</html>\n";
	  		
	  		String header;
            header = "HTTP/1.1 404 Not Found\n";
            header+= "Content-Type: text/html; charset=utf-8\n";
            header+= "Content-Length: "+html.length()+"\n";
            header+= "\r\n";

            return (header + html).getBytes();
		}
		
			private byte[] get500Page(){
			
			String html = "";
	  		
	  		html = "<!DOCTYPE html>\n";
	  		html+= "<head>\n";
	  		html+= "<title>HTTP/1.1 500 Internal Server Error</title>";
	  		html+= "</head>\n";
	  		html+= "<body>\n";
	  		html+= "<h1>HTTP/1.1 500 Internal Server Error</h1>";
	  		html+= "</body>\n";
	  		html+= "</html>\n";
	  		
	  		String header;
            header = "HTTP/1.1 500 Internal Server Error\n";
            header+= "Content-Type: text/html; charset=utf-8\n";
            header+= "Content-Length: "+html.length()+"\n";
            header+= "\r\n";

            return (header + html).getBytes();
		}
		private String getMimeTypeForExtension(String ext){
			String type = "text/plain";
			
			if (ext.equals("html")){
				type = "text/html; charset=utf-8";
			}
			
			if (ext.equals("css")){
				type = "text/css; charset=utf-8";
			}

			if (ext.equals("js")){
				type = "text/javascript";
			}

			if (ext.equals("png")){
				type = "image/png";
			}

			if (ext.equals("jpg")){
				type = "image/jpeg";
			}
			
			if (ext.equals("gif")){
				type = "image/gif";
			}

			return type;
		}
		
		@Override
		protected void onPostExecute(Long result){
			super.onPostExecute(result);
			if (result == 1){
				mListener = new Listener();
				mListener.execute(ss);
			}
		}
    }
    
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	public class JSPPages {
		public byte[] _api_getracedata(String query){
			RaceData datasource = new RaceData(RaceResultsService.this);
	  		datasource.open();
	  		Race race = datasource.getRace(mRid);

			RacePilotData datasource2 = new RacePilotData(RaceResultsService.this);
	  		datasource2.open();
	  		ArrayList<Pilot> allPilots = datasource2.getAllPilotsForRace(mRid, 0, 0, 0);
	  		
	  		long unixTime = System.currentTimeMillis() / 1000L;

	  		ArrayList<String> p_names = new ArrayList<>();
	  		for (Pilot p : allPilots){
  				p_names.add(String.format("\"%s %s\"", p.firstname, p.lastname));
  			}
	  		String pilots_array = p_names.toString();

            ArrayList<ArrayList<String>> p_times = new ArrayList<>();
            ArrayList<Integer> groups = new ArrayList<>();

			for (int rnd=0; rnd<race.round; rnd++){
                RaceData.Group group = datasource.getGroups(mRid, rnd+1);
                groups.add(group.num_groups);

				ArrayList<String> round = new ArrayList<>();
				for (Pilot p : allPilots){
					float time = datasource2.getPilotTimeInRound(race.id, p.id, rnd+1);
                    Log.i("RRS", p.toString());
                    Log.i("RRS", Float.toString(time));
					if (time>0){
						round.add(String.format("\"%.2f\"",time));
					} else {
						if (rnd == race.round-1){ // is the round in progress
							if (!p.flown){
								// Not yet flown ("")
								round.add("\"\"");
							} else {
								// Has flown so time was a zero
								round.add("\"0\"");
							}
						} else {
							round.add("\"0\"");							
						}
					}
  				}
  				p_times.add(round);
  			}
	  		String times_array = p_times.toString();
	  		
	  		ArrayList<ArrayList<String>> p_penalties = new ArrayList<>();
	  		for (int rnd=0; rnd<race.round; rnd++){
	  			ArrayList<Pilot> pilots_in_round = datasource2.getAllPilotsForRace(mRid, rnd+1, 0, 0);
				ArrayList<String> round = new ArrayList<>();
				for (int i=0; i<p_names.size(); i++){
					
  					round.add(String.format("\"%d\"", pilots_in_round.get(i).penalty *100));
  				}
  				p_penalties.add(round);
  			}
	  		String penalties_array = p_penalties.toString();
	  		String groups_array = groups.toString();

	  		datasource2.close();
	  		datasource.close();

            String data = "[{";
            data += this.addParam("time", String.valueOf(unixTime)) + ",";
            data += this.addParam("race_name", race.name) + ",";
            data += this.addParam("race_status", String.valueOf(race.status)) + ",";
            data += this.addParam("current_round", String.valueOf(race.round)) + ",";
            data += this.addParam("ftd", "{}", false) + ",";
            data += this.addParam("round_winners", "[]", false) + ",";
            data += this.addParam("pilots", pilots_array, false) + ",";
            data += this.addParam("times", times_array, false) + ",";
            data += this.addParam("penalties", penalties_array, false) + ",";
            data += this.addParam("groups", groups_array, false);
            data += "}]";
            
            String header;
            header = "HTTP/1.1 200 OK\n";
            header+= "Content-Type: application/json; charset=utf-8\n";
            header+= "Content-Length: "+data.length()+"\n";
            header+= "\r\n";
            
            return (header + data).getBytes();

		}
		
		private String addParam(String name, String value){
			return addParam(name, value, true);
		}
		
		private String addParam(String name, String value, boolean quotes){
			if (!quotes) return "\""+name+"\":"+value;
			return "\""+name+"\":\""+value+"\"";
		}
	}
}

