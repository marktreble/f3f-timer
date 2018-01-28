package com.marktreble.f3ftimer.exportimport;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.marktreble.f3ftimer.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Created by marktreble on 27/12/14.
 */
public class BluetoothImportRace extends BaseImport {


    private AlertDialog mDlg;
    private AlertDialog.Builder mDlgb;

    private BluetoothAdapter mBluetoothAdapter;
    ArrayList<String> mDiscoveredDeviceNames;
    ArrayList<BluetoothDevice> mDiscoveredDevices;

    ArrayList<String> mPairedDeviceNames;
    ArrayList<BluetoothDevice> mPairedDevices;

    ArrayList<String> mPairedAndDiscoveredDeviceNames;
    ArrayList<BluetoothDevice> mPairedAndDiscoveredDevices;

    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
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

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            getBluetoothDevices();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothImportRace.REQUEST_ENABLE_BT){
            if(resultCode==RESULT_OK){
                getBluetoothDevices();
            } else {
                Log.i("BT", "FINISHING ACTIVITY");
                mActivity.finish();
            }
        }
    }

    @Override
    public void onBackPressed(){
        closeSocket();
        Log.i("BT", "CANCELLED");
        super.onBackPressed();
    }

    @Override
    public void onResume(){
        super.onResume();
        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }

    @Override
    public void onPause(){
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    public void getBluetoothDevices(){
        mDiscoveredDeviceNames = new ArrayList<>();
        mDiscoveredDevices = new ArrayList<>();
        
        mPairedDeviceNames = new ArrayList<>();
        mPairedDevices = new ArrayList<>();
        
        boolean discovery = mBluetoothAdapter.startDiscovery();

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

        CharSequence[] devices = mPairedAndDiscoveredDeviceNames.toArray(new CharSequence[mPairedAndDiscoveredDeviceNames.size()]);
        
        mDlgb = new AlertDialog.Builder(mContext)
                .setTitle("Searching for Devices...")
                .setCancelable(true)
                .setItems(devices, deviceClickListener);
        
        mDlg = mDlgb.create();

        mDlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mBluetoothAdapter.cancelDiscovery();
                if (mmSocket == null){
                    Log.i("BT", "FINISHING ACTIVITY IN ONDISMISS");
                    mActivity.finish();
                }

            }
        });
        mDlg.show();

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                Log.i("FOUND BT DEVICE", device.getName() + "::" + device.getAddress());
                mDiscoveredDeviceNames.add(device.getName());
                mDiscoveredDevices.add(device);

                mPairedAndDiscoveredDeviceNames = new ArrayList<>();
                mPairedAndDiscoveredDeviceNames.addAll(mPairedDeviceNames);
                mPairedAndDiscoveredDeviceNames.addAll(mDiscoveredDeviceNames);

                mPairedAndDiscoveredDevices = new ArrayList<>();
                mPairedAndDiscoveredDevices.addAll(mPairedDevices);
                mPairedAndDiscoveredDevices.addAll(mDiscoveredDevices);

                CharSequence[] devices = mPairedAndDiscoveredDeviceNames.toArray(new CharSequence[mPairedAndDiscoveredDeviceNames.size()]);

                mDlg.cancel();
                mDlg = null;
                mDlgb.setItems(devices, deviceClickListener);
                mDlg = mDlgb.create();
                mDlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mBluetoothAdapter.cancelDiscovery();
                    }
                });
                mDlg.show();
            }
        }
    };

    private final DialogInterface.OnClickListener deviceClickListener = new DialogInterface.OnClickListener(){
        @Override
        public void onClick(DialogInterface dialog, int which) {
            Log.i("BT", "DEVICE CLICKED");
            mBluetoothAdapter.cancelDiscovery();
            mmDevice = mPairedAndDiscoveredDevices.get(which);

            BluetoothSocket tmp = null;
            // uuid is the app's UUID string, also used by the server code
            UUID uuid = UUID.fromString(getResources().getString(R.string.app_uuid));
            try {
                tmp = mmDevice.createRfcommSocketToServiceRecord(uuid);
                Log.i("BT", "connected to "+mmDevice.getName());
            } catch (IOException e) {
                Log.i("BT", "Failed to connect to device");
                return;
            }
            mmSocket = tmp;

            Thread connectThread = new Thread(new Runnable()
            {
                @Override
                public void run() {
                    Log.i("BT", "Starting Runnable");
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

                    try {
                        // Connect the device through the socket. This will block
                        // until it succeeds or throws an exception
                        mmSocket.connect();
                    } catch (IOException connectException) {
                        // Unable to connect; close the socket and get out
                        connectException.printStackTrace();
                        try {
                            mmSocket.close();
                        } catch (IOException closeException) {
                        
                        }
                        return;
                    }

                    // Do work to manage the connection (in a separate thread)
                    manageConnectedSocket();
                }
            });
            
            new Handler().postDelayed(connectThread, 100);
            dialog.dismiss();
        }
    };
    
    private void manageConnectedSocket() {
        Log.i("BT", "GET IO STREAMS");
        try {
            mmInStream = mmSocket.getInputStream();
            mmOutStream = mmSocket.getOutputStream();
        } catch (IOException e) {
        }

        sendRequest(CMD_LS);
        listen();
    }
    
    private void listen(){
        // Listen
        byte[] buffer = new byte[1024];  // 32K buffer store for the stream
        int bufferLength; // bytes returned from read()
        mIsListening = true;
        
        while (mIsListening) {
            try {
                // Read from the InputStream
                StringBuilder builder = new StringBuilder();
                boolean eof = false;
                while (!eof) {
                    bufferLength = mmInStream.read(buffer);

                    byte[] data = new byte[bufferLength];
                    System.arraycopy(buffer, 0, data, 0, bufferLength);
                    builder.append(new String(data, "UTF-8"));
                    if (builder.toString().contains("\n\n")) eof = true;
                }
                final String response = builder.toString();

                if (mCommandSent.equals(CMD_LS)){
                    mIsListening = false;
                    runOnUiThread(new Runnable(){
                        @Override
                        public void run(){
                           
                            showRaceNamesDialog(response);
                        }
                    });
                }

                if (mCommandSent.equals(CMD_GET)){
                    mIsListening = false;
                    runOnUiThread(new Runnable(){
                        @Override
                        public void run(){
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

    public void closeSocket(){
        Log.i("BT", "CLEANING UP");
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

    private void sendRequest(String cmd){
        sendRequest(cmd, "");
    }
    
    private void sendRequest(String cmd, String data){
        mCommandSent = cmd;
        cmd = (data.equals("")) ? cmd : cmd + " " + data;
        Log.i("BT", cmd);
        byte[] msg = cmd.getBytes();
        try {
            mmOutStream.write(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void showRaceNamesDialog(String response){
        Log.i("BT", "RESPONSE: "+response);

        JSONArray racenames;
        try {
            racenames = new JSONArray(response);

            mAvailableRaceIds = new ArrayList<>();
            ArrayList<String> racelist = new ArrayList<>();
            for (int i = 0; i < racenames.length(); i++) {
                JSONObject r = racenames.optJSONObject(i);
                String id =r.optString("id");
                String name =r.optString("name");
                mAvailableRaceIds.add(id);
                racelist.add(name);
            }

            CharSequence[] list = racelist.toArray(new CharSequence[racelist.size()]);
            mDlgb = new AlertDialog.Builder(mContext)
                    .setTitle("Races Available for Import")
                    .setCancelable(true)
                    .setItems(list, raceClickListener);

            mDlg = mDlgb.create();
            mDlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mBluetoothAdapter.cancelDiscovery();
                    if (!mCommandSent.equals(CMD_GET)) {
                        Log.i("BT", "FINISHING ACTIVITY IN ONDISMISS");
                        mActivity.finish();
                    }

                }
            });
            mDlg.show();
        } catch (JSONException e){
            e.printStackTrace();
        }

    }

    private final DialogInterface.OnClickListener raceClickListener = new DialogInterface.OnClickListener(){
        @Override
        public void onClick(DialogInterface dialog, int which) {
            sendRequest(CMD_GET, mAvailableRaceIds.get(which));
            new Handler().postDelayed(new Runnable(){
                @Override
                public void run(){
                    listen();
                }
            }, 100);
            dialog.dismiss();
        }
    };
    
    protected void importRace(String data){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean extended_json_format = sharedPref.getBoolean("pref_extended_json_format", false);
        if (extended_json_format) {
            super.importRaceJSONExt(data);
        } else {
            super.importRaceJSON(data);
        }

        closeSocket();
        mActivity.setResult(RESULT_OK);
        mActivity.finish();
    }
}
