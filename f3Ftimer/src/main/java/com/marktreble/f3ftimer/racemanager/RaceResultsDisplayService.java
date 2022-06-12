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

package com.marktreble.f3ftimer.racemanager;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.constants.IComm;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

/**
 * Created by marktreble on 04/08/15.
 */
public class RaceResultsDisplayService extends Service {

    private static final String TAG = "RaceResultsDisplaySrvc";

    private BluetoothAdapter mBluetoothAdapter;
    public static final String BT_DEVICE = "btdevice";
    public static final int STATE_NONE = 0; // we're doing nothing
    // connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing
    // connection
    public static final int STATE_CONNECTED = 3; // now connected to a remote
    // device
    private ConnectThread mConnectThread;
    private static ConnectedThread mConnectedThread;

    public static Handler mHandler = null;
    public static int mState = STATE_NONE;
    public static String deviceName;
    public static BluetoothDevice device = null;

    private String mMacAddress;

    static final String ENCODING = "US-ASCII";

    static final long PING_INTERVAL = 10000000000L;

    public static void startRDService(Context context, String prefExternalDisplay) {
        if (prefExternalDisplay == null || prefExternalDisplay.equals("")) return;

        Intent serviceIntent = new Intent(context, RaceResultsDisplayService.class);
        serviceIntent.putExtra(BT_DEVICE, prefExternalDisplay);
        context.startService(serviceIntent);
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "Service started");
        this.registerReceiver(onBroadcast, new IntentFilter("com.marktreble.f3ftimer.onExternalUpdate"));

        mHandler = new Handler(Looper.getMainLooper());

        super.onCreate();
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "ONBIND");
        return mBinder;
    }

    public class LocalBinder extends Binder {
        RaceResultsDisplayService getService() {
            return RaceResultsDisplayService.this;
        }
    }


    private final IBinder mBinder = new LocalBinder();

    public static boolean stop(RaceActivity context) {
        if (context.isServiceRunning("com.marktreble.f3ftimer.racemanager.RaceResultsDisplayService")) {
            Intent serviceIntent = new Intent(context, RaceResultsDisplayService.class);
            context.stopService(serviceIntent);
            return true;
        }
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Onstart Command");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (intent == null) return START_NOT_STICKY;

        String chosenDevice = intent.getStringExtra(BT_DEVICE);
        if (mBluetoothAdapter != null && !chosenDevice.equals("")) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice d : pairedDevices) {
                    if (d.getAddress().equals(chosenDevice))
                        device = d;
                }
            }
            if (device == null) {
                Log.d(TAG, "No device... stopping");
                return START_NOT_STICKY;
            }
            deviceName = device.getName();
            mMacAddress = device.getAddress();
            if (mMacAddress != null && mMacAddress.length() > 0) {
                Log.d(TAG, "Connecting to: " + deviceName);
                connectToDevice(mMacAddress);
            } else {
                Log.d(TAG, "No macAddress... stopping");
                stopSelf();
                return START_NOT_STICKY;
            }
        }

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
        Log.d("CallBackToUI", cmd);
        this.sendBroadcast(i);
    }

    public void displayConnected() {
        HashMap<String, String> params = new HashMap<>();
        params.put("icon", "on_display");
        callbackToUI("external_display_connected", params);
    }

    public void displayDisconnected() {
        HashMap<String, String> params = new HashMap<>();
        params.put("icon", "off_display");
        callbackToUI("external_display_disconnected", params);

    }

    private synchronized void connectToDevice(String macAddress) {
        Log.d(TAG, "Connecting... ");
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(macAddress);
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    private void setState(int state) {
        RaceResultsDisplayService.mState = state;
    }

    public synchronized void stop() {
        setState(STATE_NONE);
        mHandler.removeCallbacks(reconnect);
        displayDisconnected();

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
        stopSelf();
    }

    @Override
    public boolean stopService(Intent name) {

        setState(STATE_NONE);
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mBluetoothAdapter.cancelDiscovery();
        return super.stopService(name);
    }

    private void connectionFailed() {
        Log.d(TAG, "Connection Failed");
        //RaceResultsDisplayService.this.stop();
        // Post to UI that connection is off
        displayDisconnected();
        if (mState == STATE_NONE) return;
        reconnect();
    }

    public void connectionLost() {
        Log.d(TAG, "Connection Lost");
        displayDisconnected();

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        //RaceResultsDisplayService.this.stop();
        // Post to UI that connection is off
        reconnect();
    }

    public Runnable reconnect = new Runnable() {
        @Override
        public void run() {
            connectToDevice(mMacAddress);
        }
    };

    public void reconnect() {
        Log.d(TAG, "Reconnecting in 3 seconds...");
        mHandler.postDelayed(reconnect, 3000);
    }

    private final static Object obj = new Object();

    public static void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (obj) {
            if (mState != STATE_CONNECTED)
                return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    private synchronized void connected(BluetoothSocket mmSocket) {
        Log.d(TAG, "Connected");
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();

        // Post to UI that connection is on!
        setState(STATE_CONNECTED);
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;

            UUID hc05_uuid = UUID.fromString(getString(R.string.HC05_uuid));
            UUID app_uuid = UUID.fromString(getString(R.string.external_display_uuid));
            try {
                ParcelUuid[] Uuids = device.getUuids();

                if (Uuids.length > 0) {
                    for (ParcelUuid test : Uuids) {
                        if (test.equals(new ParcelUuid(hc05_uuid))) {
                            tmp = device.createRfcommSocketToServiceRecord(hc05_uuid);
                        }
                        if (test.equals(new ParcelUuid(app_uuid))) {
                            tmp = device.createRfcommSocketToServiceRecord(app_uuid);
                        }
                    }
                }
            } catch (IOException e) {
                Log.i(TAG, "Failed to connect to device " + device.getName());
            }


            mmSocket = tmp;
        }

        @Override
        public void run() {
            setName("ConnectThread");
            if (mmSocket == null) {
                connectionFailed();
                return;
            }
            mBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                connectionFailed();
                return;

            }
            synchronized (RaceResultsDisplayService.this) {
                mConnectThread = null;
            }
            connected(mmSocket);
        }

        public void cancel() {
            try {
                if (mmSocket != null)
                    mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final OutputStream mmOutStream;
        private final InputStream mmInStream;

        byte[] buffer = new byte[256];
        int bufferLength;

        private long last_time = 0;
        private boolean ping_sent = false;

        ConnectedThread(BluetoothSocket socket) {
            displayConnected();

            mmSocket = socket;
            OutputStream tmpOut = null;
            InputStream tmpIn = null;
            try {
                tmpOut = socket.getOutputStream();
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }
            mmOutStream = tmpOut;
            mmInStream = tmpIn;
        }

        @Override
        public void run() {

            while (mState == STATE_CONNECTED) {


                long time = System.nanoTime();
                if (time - last_time > PING_INTERVAL) {
                    if (ping_sent) {
                        Log.d(TAG, "PING NOT RETURNED");
                        connectionLost();
                    } else {
                        last_time = time;
                        String ping = String.format("{\"type\":\"ping\",\"time\":%d}\n", time);
                        Log.d(TAG, ping);
                        write(ping.getBytes(Charset.forName(ENCODING)));
                        ping_sent = true;
                    }
                }

                try {
                    String str_last_time = String.format("%d", last_time);
                    if (mmInStream.available() == str_last_time.length()) {
                        bufferLength = mmInStream.read(buffer);

                        byte[] data = new byte[bufferLength];
                        System.arraycopy(buffer, 0, data, 0, bufferLength);
                        final String response = new String(data, StandardCharsets.UTF_8);
                        Log.d(TAG, "R:" + response);
                        if (response.equals(str_last_time)) {
                            ping_sent = false;
                            displayConnected();
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        public void write(byte[] buffer) {

            try {
                mmOutStream.write(buffer);
                mmOutStream.flush();
                Log.d(TAG, "Chars written to output: " + new String(buffer, ENCODING));
            } catch (IOException e) {
                Log.d(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
                setState(STATE_NONE);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        this.unregisterReceiver(onBroadcast);


        setState(STATE_NONE);

        stop();
        super.onDestroy();
    }

    // Binding for Service->UI Communication
    private BroadcastReceiver onBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.hasExtra("com.marktreble.f3ftimer.external_results_callback")) {
                Log.i(TAG, "Callback received");
                if (mState != STATE_CONNECTED) return;

                Log.i(TAG, "Connected");
                Bundle extras = intent.getExtras();
                if (extras == null) return;

                String data = extras.getString("com.marktreble.f3ftimer.external_results_callback");
                if (data == null) {
                    Log.i(TAG, "No data");
                    return;
                }

                Log.i(TAG, "Data: " + data);
                if (data.equals("run_finalised")) {
                    String name = extras.getString("com.marktreble.f3ftimer.pilot_name");
                    String nationality = extras.getString("com.marktreble.f3ftimer.pilot_nationality");
                    nationality = (nationality != null) ? nationality.toLowerCase() : "";
                    String time = extras.getString("com.marktreble.f3ftimer.pilot_time");

                    String round = extras.getString("com.marktreble.f3ftimer.current_round");
                    String results = extras.getString("com.marktreble.f3ftimer.current_round_results");

                    JSONObject json = new JSONObject();
                    try {
                        json.put("type", "data");
                        json.put("name", StringUtils.stripAccents(name));
                        json.put("nationality", nationality);
                        json.put("time", time);
                        json.put("round", round);
                        json.put("results", results);
                    } catch (JSONException | NullPointerException e) {
                        e.printStackTrace();
                    }
                    String str_json = json.toString() + "\n";
                    Log.d(TAG, str_json);
                    byte[] bytes = str_json.getBytes(Charset.forName(ENCODING));

                    // Reset ping timer to prevent ping being sent
                    //mConnectedThread.resetPing();

                    mConnectedThread.write(bytes);

                }
            }
        }
    };

}
