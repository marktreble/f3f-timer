package com.marktreble.f3ftimer.helpers.parcelable;

import android.bluetooth.BluetoothDevice;
import android.hardware.usb.UsbAccessory;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.ResultReceiver;

import com.marktreble.f3ftimer.services.anemometer.AnemometerService;

@SuppressWarnings("deprecation")
public class ParcelableHelper {

    /**
     * Compatibility wrapper for retrieving parcelable from Bundle
     *
     * This provides a normalised function across ALL API levels.
     *
     * @param bundle any bundle
     * @param key key to retrieve value
     * @return ResultReceiver
     */
    public static ResultReceiver getParcelableResultReceiver(Bundle bundle, String key) {
        ResultReceiver value;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            value = bundle.getParcelable(key, ResultReceiver.class);
        } else {
            value = bundle.getParcelable(key);
        }
        return value;
    }

    /**
     * Compatibility wrapper for retrieving parcelable from Bundle
     *
     * This provides a normalised function across ALL API levels.
     *
     * @param bundle any bundle
     * @param key key to retrieve value
     * @return BluetoothDevice
     */
    public static BluetoothDevice getParcelableBluetoothDevice(Bundle bundle, String key) {
        BluetoothDevice value;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            value = bundle.getParcelable(key, BluetoothDevice.class);
        } else {
            value = bundle.getParcelable(key);
        }
        return value;
    }

    /**
     * Compatibility wrapper for retrieving parcelable from Bundle
     *
     * This provides a normalised function across ALL API levels.
     *
     * @param bundle any bundle
     * @param key key to retrieve value
     * @return  AnemometerService.WindData
     */
    public static AnemometerService.WindData getParcelableWindData(Bundle bundle, String key) {
        AnemometerService.WindData value;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            value = bundle.getParcelable(key, AnemometerService.WindData.class);
        } else {
            value = bundle.getParcelable(key);
        }
        return value;
    }

    /**
     * Compatibility wrapper for retrieving parcelable from Bundle
     *
     * This provides a normalised function across ALL API levels.
     *
     * @param bundle any bundle
     * @param key key to retrieve value
     * @return UsbAccessory
     */
    public static UsbAccessory getParcelableUSBAccessory(Bundle bundle, String key) {
        UsbAccessory value;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            value = bundle.getParcelable(key, UsbAccessory.class);
        } else {
            value = bundle.getParcelable(key);
        }
        return value;
    }

    /**
     * Compatibility wrapper for retrieving parcelable from Bundle
     *
     * This provides a normalised function across ALL API levels.
     *
     * @param bundle any bundle
     * @param key key to retrieve value
     * @return UsbAccessory
     */
    public static Parcelable getParcelable(Bundle bundle, String key) {
        Parcelable value;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            value = bundle.getParcelable(key, Parcelable.class);
        } else {
            value = bundle.getParcelable(key);
        }
        return value;
    }
}
