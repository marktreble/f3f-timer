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

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.helpers.bluetooth.BluetoothHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

public class BluetoothExportRace extends BaseExport {

    //private static String TAG = "BlueToothExportRace";


    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothServerSocket mmServerSocket;

    private BluetoothSocket mmSocket;
    private InputStream mmInStream;
    private OutputStream mmOutStream;

    private boolean mIsListening = false;

    public static int REQUEST_ENABLE_BT = 100;
    private int mRequestCode = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBluetoothAdapter = BluetoothHelper.getAdapter(this);

        if (mBluetoothAdapter == null) {
            // Device does not support bluetooth

            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mRequestCode = REQUEST_ENABLE_BT;
            mStartForResult.launch(enableBtIntent);
            //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            startListening();
        }

        showProgress(getString(R.string.awaiting_connection));
    }

    @Override
    public void onBackPressed() {
        closeSocket();
        Log.i("BT", "CANCELLED");
        super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (mRequestCode == BluetoothExportRace.REQUEST_ENABLE_BT) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        startListening();
                    } else {
                        mActivity.finish();
                    }
                }
            });

    private boolean permissionNotGranted(String permission) {
        if (ContextCompat.checkSelfPermission(mContext, permission) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions((Activity) mContext, new String[]{
                        permission
                }, 2);
                return true;
            }
        }
        return false;
    }

    private void startListening() {


        BluetoothServerSocket tmp = null;
        UUID uuid = UUID.fromString(getResources().getString(R.string.app_uuid));
        try {
            // MY_UUID is the app's UUID string, also used by the client code
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                if (permissionNotGranted(Manifest.permission.BLUETOOTH_CONNECT)) {
                    return;
                }
            }
            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("f3f bluetooth export", uuid);
            Log.i("BT", "SOCKET OBTAINED");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Thread acceptThread = new Thread(() -> {
            BluetoothSocket socket = null;

            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                Log.d("BLUETOOTH", e.getMessage());
            }
            // If a connection was accepted
            if (socket != null) {
                // Do work to manage the connection (in a separate thread)
                manageConnectedSocket(socket);
                try {
                    mmServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });
        if (tmp != null) {
            mmServerSocket = tmp;

            acceptThread.start();
        } else {
            mActivity.finish();
        }

    }

    public void manageConnectedSocket(BluetoothSocket socket) {
        mmSocket = socket;

        // member streams are final
        try {
            mmInStream = mmSocket.getInputStream();
            mmOutStream = mmSocket.getOutputStream();

            byte[] buffer = new byte[256];  // buffer store for the stream
            int bufferLength; // bytes returned from read()

            mIsListening = true;
            Log.i("BT", "LISTENING");

            while (mIsListening) {
                try {
                    // Read from the InputStream
                    bufferLength = mmInStream.read(buffer);

                    byte[] data = new byte[bufferLength];
                    System.arraycopy(buffer, 0, data, 0, bufferLength);
                    String cmd = new String(data, StandardCharsets.UTF_8);

                    Log.i("BT", "COMMAND = " + cmd);

                    // Respond to commands
                    if (cmd.equals("LS")) {
                        sendRaceList();
                    }

                    Log.i("BT", "COMMAND = " + cmd.substring(0, 2));
                    if (cmd.length() >= 3 && cmd.startsWith("GET")) {
                        sendRaceData(cmd.substring(4));
                    }

                } catch (IOException e) {
                    mIsListening = false;
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i("BT", "NOT LISTENING ANYMORE!");
        closeSocket();
        finish();
    }

    public void closeSocket() {
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

    private void sendRaceList() {

        RaceData datasource = new RaceData(this);
        datasource.open();
        ArrayList<Race> allRaces = datasource.getAllRaces();
        datasource.close();

        StringBuilder response = new StringBuilder("[");
        for (Race r : allRaces) {
            if (response.length() > 1) response.append(",");
            response.append(String.format("{\"id\":\"%s\", \"name\":\"%s\"}", r.id, r.name));
        }

        response.append("]\n\n");

        byte[] bytes = response.toString().getBytes();

        try {
            mmOutStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void sendRaceData(String query) {
        // Serialize all race data, pilots, times + groups
        int race_id = Integer.parseInt(query);

        RaceData datasource = new RaceData(this);
        datasource.open();
        Race r = datasource.getRace(race_id);
        datasource.close();

        String data = super.getSerialisedRaceData(r.id, r.round);

        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);

        try {
            mmOutStream.write(bytes, 0, bytes.length);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
