/*
 * Languages
 * Utility functions for language settings
 */

package com.marktreble.f3ftimer.wifi;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.BitSet;

public class Wifi {
    
    static String TAG = "WIFIMANAGER";
    
	public static boolean canEnableWifiHotspot(Activity context){
		WifiManager wifiManager = (WifiManager)context.getBaseContext().getSystemService(Context.WIFI_SERVICE);
		
		WifiApControl apControl = WifiApControl.getApControl(wifiManager);
		return (apControl!=null);
	}
	
	public static boolean enableWifiHotspot(Activity context){
		boolean wifiwasenabled = false;
		WifiManager wifiManager = (WifiManager)context.getBaseContext().getSystemService(Context.WIFI_SERVICE);
	
		WifiApControl apControl = WifiApControl.getApControl(wifiManager);
	    if (apControl != null) {
	
	  	    if(wifiManager.isWifiEnabled()){
		        wifiManager.setWifiEnabled(false);   
		        wifiwasenabled = true;
		    }    
	  	    
	  	    WifiConfiguration config = apControl.getWifiApConfiguration();
            // Set the network name
	  	    config.SSID = "f3f";
	  	    // Remove all authentication!
            config.allowedKeyManagement.clear();

	        apControl.setWifiApEnabled(config, true);	     
            Log.i(TAG, "Wifi Hotspot Enabled");
	    }   
	    
	    return wifiwasenabled;
	}
	
	public static void disableWifiHotspot(Activity context, boolean enablewifi){
		WifiManager wifiManager = (WifiManager)context.getBaseContext().getSystemService(Context.WIFI_SERVICE);
	
	    WifiApControl apControl = WifiApControl.getApControl(wifiManager);
	    if (apControl != null) {

	    	apControl.setWifiApEnabled(apControl.getWifiApConfiguration(), false);
            Log.i(TAG, "Wifi Hotspot Disabled");

	    	if (enablewifi)
	    		wifiManager.setWifiEnabled(true);

            Log.i(TAG, "Wifi Restored to " + ((enablewifi) ? "On":"Off"));

        }
	}
	
	
}