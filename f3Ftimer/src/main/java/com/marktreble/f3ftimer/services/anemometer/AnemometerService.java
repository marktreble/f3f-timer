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

package com.marktreble.f3ftimer.services.anemometer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.marktreble.f3ftimer.constants.IComm;
import com.marktreble.f3ftimer.helpers.bluetooth.BluetoothHelper;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

/**
 * Created by marktreble on 03/10/22.
 */
public class AnemometerService extends Service {

    private static final String TAG = "AnemometerService";

    public static final String BT_DEVICE = "btdevice";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothDevice mDevice;
    public static Handler mHandler = null;
    private Context mContext;

    private String mChosenDeviceName = null;
    private Boolean mDeviceConnecting = false;

    final private UUID GATT_CHARACTERISTIC_WIND_SPEED = UUID.fromString("D7D9DB8F-EBA9-49BD-B0D1-44A0ACEE48D7");
    final private UUID GATT_CHARACTERISTIC_DIRECTION = UUID.fromString("D9E73850-0684-424D-B877-41353014166B");
    final private UUID GATT_CHARACTERISTIC_VOLTAGE = UUID.fromString("D656B22C-63BE-4DD9-8F38-91BB38F585F7");
    final private UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private Double speed = 0.0;
    private Double direction = 0.0;
    private Double voltage = 0.0;

    private long mConditionsOutTime;
    private long secondsElapsed;

    private Map<Long, Double> speedSamples;
    private Map<Long, Double> directionSamples;

    private Double avgSpeedSum = 0.0;
    private Double avgDirectionSum  = 0.0;

    private Double avgSpeed = 0.0;
    private Double avgDirection = 0.0;

    private final Integer AVERAGING_PERIOD = 20; // Seconds

    private Queue<BluetoothGattCharacteristic> writeQueue = new LinkedList<>();

    private Boolean conditionsIllegal = false;

    enum Wind {
        GOOD,
        SPEED,
        DIRECTION
    }

    static ServiceConnection anemometerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d("BOUND -> Anemom", iBinder.toString());

            AnemometerService.LocalBinder binder = (AnemometerService.LocalBinder)iBinder;
            AnemometerServiceInstance.setInstance(binder.getService());
            Log.d("BOUND", "BOUND=true " + binder.getService());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            AnemometerServiceInstance.getInstance().isBound = false;
            Log.d("BOUND", "BOUND=false");
        }
    };

    public static void startAnemometerService(Context context, String prefAnemometer) {
        if (prefAnemometer == null || prefAnemometer.equals("")) return;

        Intent serviceIntent = new Intent(context, AnemometerService.class);
        serviceIntent.putExtra(BT_DEVICE, prefAnemometer);
        context.startService(serviceIntent);
        context.bindService(serviceIntent, anemometerServiceConnection, Context.BIND_AUTO_CREATE);
    }


    @Override
    public void onCreate() {
        Log.i(TAG, "Service started");
        mHandler = new Handler(Looper.getMainLooper());

        super.onCreate();
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "ONBIND");
        return mBinder;
    }

    private class LocalBinder extends Binder {
        AnemometerService getService() {
            return AnemometerService.this;
        }
    }


    private final IBinder mBinder = new LocalBinder();

    public static boolean stop(RaceActivity context) {
        if (context.isServiceRunning("com.marktreble.f3ftimer.services.anemometer.AnemometerService")) {
            Intent serviceIntent = new Intent(context, AnemometerService.class);
            context.stopService(serviceIntent);
            return true;
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Onstart Command");
        if (intent == null) return START_NOT_STICKY;

        String chosenDevice = intent.getStringExtra(BT_DEVICE);
        scanForDevice(chosenDevice);

        speedSamples = new HashMap<>();
        directionSamples = new HashMap<>();
        avgSpeedSum = 0.0;
        avgDirectionSum = 0.0;

        return START_STICKY;
    }

    private void callbackToUI(String cmd, HashMap<String, String> params) {
        Intent i = new Intent(IComm.RCV_UPDATE);
        if (params != null) {
            for (String key : params.keySet()) {
                i.putExtra(key, params.get(key));
            }
        }

        i.putExtra(IComm.MSG_SERVICE_CALLBACK, cmd);
        Log.d(TAG, cmd);
        this.sendBroadcast(i);
    }

    public void anemometerConnected() {
        HashMap<String, String> params = new HashMap<>();
        params.put("icon", "on_anemometer");
        callbackToUI("anemometer_connected", params);
    }

    public void anemometerDisconnected() {
        HashMap<String, String> params = new HashMap<>();
        params.put("icon", "off_anemometer");
        callbackToUI("anemometer_disconnected", params);

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void scanForDevice(String chosenDevice) {
        mChosenDeviceName = chosenDevice;

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "No bluetooth hardware.");
        } else if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Blutooth is off.");
        }

        mContext = this;

        if (ActivityCompat.checkSelfPermission(
                mContext,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                        mContext,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Location Permissions Required.");
            callbackToUI("request_location", null);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    mContext,
                    Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Bluetooth Scan Permissions Required.");
                callbackToUI("request_bluetooth_scan", null);
                return;
            }
        }

        Log.d(TAG, "Scanning for " + chosenDevice);
        mDeviceConnecting = false;
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBluetoothLeScanner.startScan(scanCallBack);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private final ScanCallback scanCallBack = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            mDevice = result.getDevice();
            if (mDevice.getName() != null) {
                Log.d(TAG, "Device found: " + mDevice.getName());
                if (mDevice.getName().equals(mChosenDeviceName) &&
                        !mDeviceConnecting) {
                    Log.d(TAG, "Device Matched!");
                    mDeviceConnecting = true;
                    if (mBluetoothLeScanner != null) {
                        mBluetoothLeScanner.stopScan(scanCallBack);
                    }
                    mBluetoothGatt = mDevice.connectGatt(
                            mContext,
                            true,
                            bluetoothGattCallback
                    );
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(TAG, "FAILED: " + errorCode);
        }
    };

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
                anemometerConnected();
                Log.d(TAG, "Looking for Services");
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
                Log.d(TAG, "DISCONNECTED!");
                anemometerDisconnected();
                mDeviceConnecting = false;
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            boolean isFirst = true;
            int servicesCount = gatt.getServices().size();
            List<BluetoothGattService> services  = gatt.getServices();
            for (int i = 0; i< servicesCount; i++) {
                Log.d(TAG, "Looking for Characteristics");

                BluetoothGattService svc = services.get(i);
                Log.d(TAG, "Found Service: " + svc.toString());
                List<BluetoothGattCharacteristic> gattCharacteristics = svc.getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    Log.d(TAG, "Found: " + gattCharacteristic.getUuid());
                    if (gattCharacteristic.getUuid().equals(GATT_CHARACTERISTIC_WIND_SPEED) ||
                            gattCharacteristic.getUuid().equals(GATT_CHARACTERISTIC_DIRECTION) ||
                            gattCharacteristic.getUuid().equals(GATT_CHARACTERISTIC_VOLTAGE)) {
                        Log.d(TAG, "Found: --> " + gattCharacteristic.getUuid());

                        // Enable notification on Android
                        gatt.setCharacteristicNotification(gattCharacteristic, true);

                        // We need to enable notifications on the device side too
                        // but we cannot send them all at once they must be queued
                        // and daisy chained
                        writeQueue.add(gattCharacteristic);

                        if (isFirst) {
                            isFirst = false;
                            processWriteQueue();
                        }
                   }
               }
           }
        }

        @SuppressWarnings("deprecation")
        public void onCharacteristicChanged(
                BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic
        ) {
            AnemometerService.this.onCharacteristicChanged(
                    gatt,
                    characteristic,
                    characteristic.getValue()
            );
        }

        @Override
        public void onCharacteristicChanged(
                BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                byte[] value
        ) {
            AnemometerService.this.onCharacteristicChanged(
                    gatt,
                    characteristic,
                    value
            );
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            processWriteQueue();
        }
    };

    @SuppressLint("MissingPermission")
    private void processWriteQueue() {
        BluetoothGattCharacteristic c = writeQueue.poll();

        if (c != null) {
            // Enable notification on remote device
            BluetoothGattDescriptor descriptor = c.getDescriptor(
                    CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID
            );

            BluetoothHelper.writeDescriptor(mBluetoothGatt, descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        }
    }

    public void onCharacteristicChanged(
            BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic,
            byte[] data
    ) {
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for(byte byteChar : data) {
                stringBuilder.append((char) byteChar);
            }
            Double value = Double.valueOf(stringBuilder.toString());

            if (characteristic.getUuid().equals(GATT_CHARACTERISTIC_WIND_SPEED)) {
                speed = value;
            }
            if (characteristic.getUuid().equals(GATT_CHARACTERISTIC_DIRECTION)) {
                direction = value;
            }
            if (characteristic.getUuid().equals(GATT_CHARACTERISTIC_VOLTAGE)) {
                voltage = value;
            }

            int conditions = Wind.GOOD.ordinal();
            if (speed < AnemometerParameters.MIN_WINDSPEED || speed > AnemometerParameters.MAX_WINDSPEED) {
                conditions |= Wind.SPEED.ordinal();
            }

            if (direction > AnemometerParameters.MAX_ANGLE && direction < (360 - AnemometerParameters.MAX_ANGLE)) {
                conditions |= Wind.DIRECTION.ordinal();
            }

            if (conditions == Wind.GOOD.ordinal()) {
                stopConditionsTimer();
            } else {
                startConditionsTimer();
            }

            Intent i = new Intent(IComm.RCV_UPDATE);
            WindData wd = new WindData(speed, direction, voltage, secondsElapsed);
            i.putExtra(
                    "com.marktreble.f3ftimer.value.wind_values",
                    wd
            );
            sendBroadcast(i);

            // Add Sample to averaging arrays
            long time = System.currentTimeMillis();
            speedSamples.put(time, speed);
            avgSpeedSum += speed;

            directionSamples.put(time, direction);
            avgDirectionSum += direction;

            // Remove Sample from averaging arrays
            // which are over AVERAGING_PERIOD seconds old
            Iterator<Map.Entry<Long, Double>> iterator;
            iterator = speedSamples.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Long, Double> entry = iterator.next();
                if (entry.getKey() < time - (1000L * AVERAGING_PERIOD)) {
                    avgSpeedSum -= entry.getValue();
                    iterator.remove();
                }
            }

            iterator = directionSamples.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Long, Double> entry = iterator.next();
                if (entry.getKey() < time - (1000L * AVERAGING_PERIOD)) {
                    avgDirectionSum -= entry.getValue();
                    iterator.remove();
                }
            }

            // Calculate the AVERAGING_PERIOD seconds average
            Integer sz = speedSamples.size();
            avgSpeed = avgSpeedSum / sz;
            avgDirection = avgDirectionSum /sz;

            anemometerConnected();
        }
    }

    private final Runnable everySecondTimer = () -> {
        secondsElapsed = (new Date().getTime() - mConditionsOutTime) / 1000;

        Intent i = new Intent(IComm.RCV_UPDATE);
        WindData wd = new WindData(speed, direction, voltage, secondsElapsed);
        i.putExtra(
                "com.marktreble.f3ftimer.value.wind_values",
                wd
        );
        sendBroadcast(i);

        if (secondsElapsed >= AnemometerParameters.CONDITIONS_TIMEOUT) {
            conditionsTimeout();
        }
        continueConditionsTimer();
    };

    private void continueConditionsTimer() {
        mHandler.postDelayed(everySecondTimer, 1000);
    }

    private void startConditionsTimer() {
        if (!conditionsIllegal) {
            conditionsIllegal = true;
            mConditionsOutTime = new Date().getTime();
            mHandler.postDelayed(everySecondTimer, 1000);
        }
    }

    private void stopConditionsTimer() {
        mHandler.removeCallbacks(everySecondTimer);

        if (conditionsIllegal) {
            conditionsIllegal = false;
            Intent i = new Intent(IComm.RCV_UPDATE);
            i.putExtra(IComm.MSG_SERVICE_CALLBACK, "conditions_legal");
            sendBroadcast(i);
        }
    }

    private void conditionsTimeout() {
        Intent i = new Intent(IComm.RCV_UPDATE);
        i.putExtra(IComm.MSG_SERVICE_CALLBACK, "conditions_illegal");
        sendBroadcast(i);
    }

    @SuppressLint("MissingPermission")
    public synchronized void stop() {
        if (mBluetoothGatt != null) {
            Log.d(TAG, "Closing Connection");
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            mBluetoothAdapter = null;
            mBluetoothLeScanner = null;
            mBluetoothManager = null;
        }

        stopSelf();
    }

    @Override
    public boolean stopService(Intent name) {
        return super.stopService(name);
    }

    @Override
    public void onDestroy() {
        stop();
        super.onDestroy();
    }

    public static class WindData implements Parcelable {
        public Double Speed;
        public Double Direction;
        public Double Voltage;
        Long SecondsElapsed;

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeLong(SecondsElapsed);
            out.writeDouble(Voltage);
            out.writeDouble(Direction);
            out.writeDouble(Speed);
        }

        public static final Parcelable.Creator<WindData> CREATOR = new Parcelable.Creator<WindData>() {
            public WindData createFromParcel(Parcel in) {
                return new WindData(in);
            }

            @Override
            public WindData[] newArray(int sz) {
                return new WindData[sz];
            }
        };

        private WindData(Double speed, Double direction, Double voltage, Long secondsElapsed) {
            Speed = speed;
            Direction = direction;
            Voltage = voltage;
            SecondsElapsed = secondsElapsed;
        }

        private WindData(Parcel in) {
            SecondsElapsed = in.readLong();
            Voltage = in.readDouble();
            Direction = in.readDouble();
            Speed = in.readDouble();
        }
    }

    public Double getAvgWindSpeed() {
        return Math.round(avgSpeed*10.0) / 10.0;
    }

    public Double getAvgWindDirection() {
        return Math.round(avgDirection*10.0) / 10.0;

    }
}

