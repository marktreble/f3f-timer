package com.marktreble.f3ftimer.bluetooth;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by marktreble on 27/12/14.
 */
public class BluetoothExport extends Activity {

    private Context mContext;
    private Activity mActivity;

    private AlertDialog mDlg;
    private AlertDialog.Builder mDlgb;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothServerSocket mmServerSocket;

    private BluetoothSocket mmSocket;
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    
    private boolean mIsListening = false;

    public static int REQUEST_ENABLE_BT = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.bt_export);

        mContext = this;
        mActivity = this;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support bluetooth

            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            startListening();
        }
    }

    @Override
    public void onBackPressed(){
        closeSocket();
        Log.i("BT", "CANCELLED");
        super.onBackPressed();
    }
    
    @Override
    public void onDestroy(){
        super.onDestroy();
        if (mDlg != null) mDlg = null;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothExport.REQUEST_ENABLE_BT){
            if(resultCode==RESULT_OK){
                startListening();
            } else {
                mActivity.finish();
            }
        }
    }

    private void startListening() {
        
        
        BluetoothServerSocket tmp = null;
        UUID uuid = UUID.fromString(getResources().getString(R.string.app_uuid));
        try {
            // MY_UUID is the app's UUID string, also used by the client code
            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("f3f bluetooth export", uuid);
            Log.i("BT", "SOCKET OBTAINED");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Thread acceptThread = new Thread(new Runnable()
        {
            public void run()
            {
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

            }
        });
        if (tmp!=null) {
            mmServerSocket = tmp;

            acceptThread.start();
        } else {
            mActivity.finish();
        }

    }

    public void manageConnectedSocket(BluetoothSocket socket){
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
                    String cmd = new String(data, "UTF-8");

                    Log.i("BT", "COMMAND = " + cmd);

                    // Respond to commands
                    if (cmd.equals("LS")){
                        sendRaceList();
                    }

                    Log.i("BT", "COMMAND = " + cmd.substring(0,2));
                    if (cmd.length()>=3 && cmd.substring(0,3).equals("GET")){
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
    
    private void sendRaceList(){
        String response = "[";

        RaceData datasource = new RaceData(this);
        datasource.open();
        ArrayList<Race> allRaces = datasource.getAllRaces();
        datasource.close();

        for (Race r: allRaces){
            if (response.length()>1) response+=",";
            response+= String.format("{\"id\":\"%s\", \"name\":\"%s\"}", Integer.toString(r.id), r.name);
        }
        
        response+="]\n\n";
        
        byte[] bytes = response.getBytes();

        try {
            mmOutStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    
    
    private void sendRaceData(String query){
        // Serialize all race data, pilots, times + groups
        int race_id = Integer.parseInt(query);
        
        RaceData datasource = new RaceData(mContext);
        datasource.open();
        Race r = datasource.getRace(race_id);
        String race = datasource.getSerialized(race_id);
        String racegroups = datasource.getGroupsSerialized(race_id, r.round);
        datasource.close();

        RacePilotData datasource2 = new RacePilotData(mContext);
        datasource2.open();
        String racepilots = datasource2.getPilotsSerialized(race_id);
        String racetimes = datasource2.getTimesSerialized(race_id, r.round);
        datasource2.close();
        
        String data = String.format("{\"race\":%s, \"racepilots\":%s,\"racetimes\":%s,\"racegroups\":%s}\n\n", race, racepilots, racetimes, racegroups);

        Log.d("BLUETOOTH::EXPORT", data);

        byte[] bytes = data.getBytes(Charset.forName("UTF-8"));

        try {
            mmOutStream.write(bytes, 0, bytes.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
}
