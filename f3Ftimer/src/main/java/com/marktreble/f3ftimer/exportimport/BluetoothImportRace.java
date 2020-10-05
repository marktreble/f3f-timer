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

package com.marktreble.f3ftimer.exportimport;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import androidx.fragment.app.FragmentTransaction;
import android.util.Log;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.dialog.GenericListPicker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Created by marktreble on 27/12/14.
 */
public class BluetoothImportRace extends BaseImport {


    static final String DIALOG = "dialog";

    GenericListPicker mDLG2;

    public BluetoothAdapter mBluetoothAdapter;
    ArrayList<String> mDiscoveredDeviceNames;
    ArrayList<BluetoothDevice> mDiscoveredDevices;

    ArrayList<String> mPairedDeviceNames;
    ArrayList<BluetoothDevice> mPairedDevices;

    ArrayList<String> mPairedAndDiscoveredDeviceNames;
    ArrayList<BluetoothDevice> mPairedAndDiscoveredDevices;

    CharSequence[] mDevices;

    private BluetoothSocket mmSocket;
    private InputStream mmInStream;
    private OutputStream mmOutStream;

    private boolean mIsListening = false;

    private String mCommandSent;
    private ArrayList<String> mAvailableRaceIds;

    public static int REQUEST_ENABLE_BT = 100;

    private static String CMD_LS = "LS";
    private static String CMD_GET = "GET";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support bluetooth

            return;
        }

        if (savedInstanceState == null) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                getBluetoothDevices();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothImportRace.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                getBluetoothDevices();
            } else {
                Log.i("BT", "FINISHING ACTIVITY");
                mActivity.finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        closeSocket();
        Log.i("BT", "CANCELLED");
        super.onBackPressed();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    public void getBluetoothDevices() {
        mDiscoveredDeviceNames = new ArrayList<>();
        mDiscoveredDevices = new ArrayList<>();

        mPairedDeviceNames = new ArrayList<>();
        mPairedDevices = new ArrayList<>();

        mBluetoothAdapter.startDiscovery();

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                mPairedDeviceNames.add(device.getName());
                mPairedDevices.add(device);
            }
        }

        mPairedAndDiscoveredDeviceNames = new ArrayList<>();
        mPairedAndDiscoveredDeviceNames.addAll(mPairedDeviceNames);

        mPairedAndDiscoveredDevices = new ArrayList<>();
        mPairedAndDiscoveredDevices.addAll(mPairedDevices);

        mDevices = mPairedAndDiscoveredDeviceNames.toArray(new CharSequence[0]);

        String[] buttons_array = new String[1];
        buttons_array[0] = getString(android.R.string.cancel);

        mDLG2 = GenericListPicker.newInstance(
                getString(R.string.select_device),
                mPairedAndDiscoveredDeviceNames,
                buttons_array,
                new ResultReceiver(new Handler()) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        super.onReceiveResult(resultCode, resultData);
                        if (resultCode == 0) {
                            mBluetoothAdapter.cancelDiscovery();

                            mPairedAndDiscoveredDeviceNames = null;

                            if (mmSocket == null) {
                                mActivity.finish();
                            }
                        } else if (resultCode >= 100) {
                            deviceClicked(resultCode - 100);
                        }
                    }
                }
        );

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(mDLG2, DIALOG);
        ft.commit();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                String name = device.getName();
                if (name == null) name = "Unknown";
                Log.i("FOUND BT DEVICE", name + "::" + device.getAddress());

                if (mDiscoveredDevices.contains(device)) return;

                mDiscoveredDeviceNames.add(name);
                mDiscoveredDevices.add(device);

                mPairedAndDiscoveredDeviceNames.add(name);
                mPairedAndDiscoveredDevices.add(device);



            }
        }
    };


    public void deviceClicked(int which) {
        showProgress(getString(R.string.connecting));

        mBluetoothAdapter.cancelDiscovery();
        final BluetoothDevice mmDevice = mPairedAndDiscoveredDevices.get(which);
        final String deviceName = mPairedAndDiscoveredDeviceNames.get(which);

        Thread createSocketThread = new Thread(new Runnable() {
            @Override
            public void run() {
                BluetoothSocket tmp;
                // uuid is the app's UUID string, also used by the server code
                UUID uuid = UUID.fromString(getResources().getString(R.string.app_uuid));
                try {
                    tmp = mmDevice.createRfcommSocketToServiceRecord(uuid);
                    Log.i("BT", "connected to " + mmDevice.getName());
                } catch (IOException e) {
                    Log.i("BT", "Failed to connect to device");
                    return;
                }
                mmSocket = tmp;

                Log.i("BT", "Starting Runnable");

                try {
                    // Connect the device through the socket. This will block
                    // until it succeeds or throws an exception
                    mmSocket.connect();
                } catch (IOException connectException) {
                    // Unable to connect; close the socket and get out
                    connectException.printStackTrace();

                    call("connectionDenied", deviceName);

                    try {
                        mmSocket.close();
                    } catch (IOException closeException) {
                        closeException.printStackTrace();
                    }
                    return;
                }

                // Do work to manage the connection (in a separate thread)
                manageConnectedSocket();

            }
        });
        createSocketThread.start();

    }

    private void manageConnectedSocket() {
        try {
            mmInStream = mmSocket.getInputStream();
            mmOutStream = mmSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        sendRequest(CMD_LS);
        listen();
    }

    private void listen() {
        // Listen
        byte[] buffer = new byte[1024];  // 32K buffer store for the stream
        int bufferLength; // bytes returned from read()
        mIsListening = true;

        while (mIsListening) {
            try {
                // Read from the InputStream
                final StringBuilder builder = new StringBuilder();
                boolean eof = false;
                while (!eof) {
                    bufferLength = mmInStream.read(buffer);

                    byte[] data = new byte[bufferLength];
                    System.arraycopy(buffer, 0, data, 0, bufferLength);
                    builder.append(new String(data, StandardCharsets.UTF_8));
                    if (builder.toString().contains("\n\n")) eof = true;
                }
                final String response = builder.toString();

                if (mCommandSent.equals(CMD_LS)) {
                    mIsListening = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            showRaceNamesDialog(response);
                        }
                    });
                }

                if (mCommandSent.equals(CMD_GET)) {
                    mIsListening = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            importRace(response);
                        }
                    });
                }

            } catch (IOException e) {
                e.printStackTrace();
                mIsListening = false;
                break;
            }
        }
    }

    public void closeSocket() {
        mIsListening = false;

        if (mmInStream != null) {
            try {
                mmInStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mmOutStream != null) {
            try {
                mmOutStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mmSocket != null) {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendRequest(String cmd) {
        sendRequest(cmd, "");
    }

    private void sendRequest(String cmd, String data) {
        mCommandSent = cmd;
        cmd = (data.equals("")) ? cmd : cmd + " " + data;
        byte[] msg = cmd.getBytes();
        try {
            mmOutStream.write(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showRaceNamesDialog(String response) {

        JSONArray racenames;
        try {
            racenames = new JSONArray(response);

            mAvailableRaceIds = new ArrayList<>();
            ArrayList<String> racelist = new ArrayList<>();
            for (int i = 0; i < racenames.length(); i++) {
                JSONObject r = racenames.optJSONObject(i);
                String id = r.optString("id");
                String name = r.optString("name");
                mAvailableRaceIds.add(id);
                racelist.add(name);
            }

            String[] buttons_array = new String[1];
            buttons_array[0] = getString(android.R.string.cancel);

            mDLG2 = GenericListPicker.newInstance(
                    getString(R.string.ttl_available_races),
                    racelist,
                    buttons_array,
                    new ResultReceiver(new Handler()) {
                        @Override
                        protected void onReceiveResult(int resultCode, Bundle resultData) {
                            super.onReceiveResult(resultCode, resultData);
                            if (resultCode == 0) {
                                mBluetoothAdapter.cancelDiscovery();
                                if (!mCommandSent.equals(CMD_GET)) {
                                    mActivity.finish();
                                }
                            } else if (resultCode >= 100) {
                                raceClicked(resultCode - 100);
                            }
                        }
                    }
            );

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(mDLG2, DIALOG);
            ft.commit();

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void raceClicked(int which) {
        sendRequest(CMD_GET, mAvailableRaceIds.get(which));
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                listen();
            }
        }, 100);
        mDLG2.dismiss();
    }

    protected void importRace(String data) {
        super.importRaceJSON(data);

        closeSocket();
        mActivity.setResult(RESULT_OK);
        mActivity.finish();
    }
}
