package com.marktreble.f3ftimer.helpers.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;

@SuppressWarnings("deprecation")
public class BluetoothHelper {

    /**
     * Compatibility wrapper for getting instance of BlueToothAdapter.
     *
     * This provides a normalised function across ALL API levels.
     *
     * @param context current context
     * @return BluetoothAdapter
     */
    static public BluetoothAdapter getAdapter(Context context) {
        BluetoothAdapter adapter = null;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                adapter = bluetoothManager.getAdapter();
            }
        }else{
            adapter = BluetoothAdapter.getDefaultAdapter();
        }
        return adapter;
    }

    /**
     * Compatibility wrapper for writing a BLE descriptor
     *
     * @param gatt BluetoothGatt
     * @param descriptor BluetoothGattDescriptor
     * @param value byte array to write
     */
    @SuppressWarnings("MissingPermission")
    static public void writeDescriptor (BluetoothGatt gatt, BluetoothGattDescriptor descriptor, byte[] value) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, value);
        } else {
            descriptor.setValue(value);
            gatt.writeDescriptor(descriptor);
        }
    }
}
