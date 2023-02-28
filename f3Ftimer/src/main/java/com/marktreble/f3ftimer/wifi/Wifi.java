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

package com.marktreble.f3ftimer.wifi;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class Wifi {

    final private static String TAG = "WIFIMANAGER";

    public static boolean canEnableWifiHotspot(Activity context) {
        WifiManager wifiManager = (WifiManager) context.getBaseContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        WifiApControl apControl = WifiApControl.getApControl(wifiManager);
        return (apControl != null);
    }

    @SuppressWarnings("deprecation")
    public static boolean enableWifiHotspot(Activity context) {
        boolean wifiwasenabled = false;
        WifiManager wifiManager = (WifiManager) context.getBaseContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        WifiApControl apControl = WifiApControl.getApControl(wifiManager);
        if (apControl != null && wifiManager != null) {

            if (wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(false);
                wifiwasenabled = true;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WifiNetworkSpecifier wifiNetworkSpecifier = new WifiNetworkSpecifier.Builder()
                        .setSsid("f3f")
                        .build();
                NetworkRequest networkRequest = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .setNetworkSpecifier(wifiNetworkSpecifier)
                        .build();
                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                connectivityManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback());
            } else {
                android.net.wifi.WifiConfiguration config = apControl.getWifiApConfiguration();
                // Set the network name
                config.SSID = "f3f";
                // Remove all authentication!
                config.allowedKeyManagement.clear();

                apControl.setWifiApEnabled(config, true);
            }
            Log.i(TAG, "Wifi Hotspot Enabled");
        }

        return wifiwasenabled;
    }

    @SuppressWarnings("deprecation")
    public static void disableWifiHotspot(Activity context, boolean enableWifi) {
        WifiManager wifiManager = (WifiManager) context
                .getBaseContext()
                .getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        WifiApControl apControl = WifiApControl
                .getApControl(wifiManager);

        if (apControl != null) {
            android.net.wifi.WifiConfiguration config = apControl.getWifiApConfiguration();
            apControl.setWifiApEnabled(config, false);
            Log.i(TAG, "Wifi Hotspot Disabled");

            if (enableWifi && wifiManager != null) {
                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q) {
                    // Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
                    // context.startActivityForResult(panelIntent,1);
                } else {
                    wifiManager.setWifiEnabled(false);
                }
            }

            Log.i(TAG, "Wifi Restored to " + ((enableWifi) ? "On" : "Off"));
        }
    }

    /**
     * Get IP address of the first network interface found (IPv4 prior to IPv6).
     */
    public static String getIPAddress(boolean useIPv4) {
        Log.d("PPP", "getIPAddress");
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = (sAddr.trim().length() > 0) && (sAddr.indexOf(':') < 0);

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } // for now eat exceptions
        return "???";
    }


}