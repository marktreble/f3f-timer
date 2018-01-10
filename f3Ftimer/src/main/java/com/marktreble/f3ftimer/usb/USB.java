package com.marktreble.f3ftimer.usb;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by marktreble on 09/01/2018.
 */

public class USB {
    public static boolean setupUsbTethering(Context context) {
        IntentFilter extraFilterToGetBatteryInfo = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent extraIntentToGetBatteryInfo = context.registerReceiver(null, extraFilterToGetBatteryInfo);
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
