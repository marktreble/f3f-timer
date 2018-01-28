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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

public class Wifi {
    
    static String TAG = "WIFIMANAGER";
    
	public static boolean canEnableWifiHotspot(Activity context){
		WifiManager wifiManager = (WifiManager)context.getBaseContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		
		WifiApControl apControl = WifiApControl.getApControl(wifiManager);
		return (apControl!=null);
	}
	
	public static boolean enableWifiHotspot(Activity context){
		boolean wifiwasenabled = false;
		WifiManager wifiManager = (WifiManager)context.getBaseContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
	
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
		WifiManager wifiManager = (WifiManager)context.getBaseContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
	
	    WifiApControl apControl = WifiApControl.getApControl(wifiManager);
	    if (apControl != null) {

	    	apControl.setWifiApEnabled(apControl.getWifiApConfiguration(), false);
            Log.i(TAG, "Wifi Hotspot Disabled");

	    	if (enablewifi)
	    		wifiManager.setWifiEnabled(true);

            Log.i(TAG, "Wifi Restored to " + ((enablewifi) ? "On":"Off"));

        }
	}

	/**
	 * Get IP address of the first network interface found (IPv4 prior to IPv6).
	 */
	public static String getIPAddress(boolean useIPv4) {
		try {
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces) {
				List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
				for (InetAddress addr : addrs) {
					if (!addr.isLoopbackAddress()) {
						String sAddr = addr.getHostAddress();
						//boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
						boolean isIPv4 = sAddr.indexOf(':')<0;

						if (useIPv4) {
							if (isIPv4)
								return sAddr;
						} else {
							if (!isIPv4) {
								int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
								return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} // for now eat exceptions
		return "";
	}
}