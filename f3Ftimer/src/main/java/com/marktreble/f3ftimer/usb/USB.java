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

package com.marktreble.f3ftimer.usb;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class USB {
    public static boolean setupUsbTethering(Context context) {
        IntentFilter extraFilterToGetBatteryInfo = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent extraIntentToGetBatteryInfo = context.registerReceiver(null, extraFilterToGetBatteryInfo);
        if (extraIntentToGetBatteryInfo == null) {
            return false;
        }
        int chargePlug = extraIntentToGetBatteryInfo.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
        if (usbCharge) {
            try {
                boolean found = false;
                Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
                if (ifs != null) {
                    while (ifs.hasMoreElements()) {
                        NetworkInterface iface = ifs.nextElement();
                        System.out.println(iface.getName());
                        if (!iface.getName().contains("usb") && !iface.getName().contains("rndis")) {
                            continue;
                        }
                        found = true;
                        Enumeration<InetAddress> en = iface.getInetAddresses();
                        while (en.hasMoreElements()) {
                            InetAddress addr = en.nextElement();
                            String s = addr.getHostAddress();
                            int end = s.lastIndexOf("%");
                            if (end > 0)
                                System.out.println("\t" + s.substring(0, end));
                            else
                                System.out.println("\t" + s);
                        }
                        break;
                    }
                }
                if (!found)
                    return false;

            } catch (SocketException ex) {
                // error handling appropriate for your application
            }
        }

        return true;
    }
}
