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

package com.marktreble.f3ftimer.driver;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.constants.IComm;
import com.marktreble.f3ftimer.constants.Pref;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class USBOpenAccessoryService extends Service implements DriverInterface {

    private static final String TAG = "USBOpenAccessoryService";
    private static final String ACTION_USB_PERMISSION = "com.marktreble.f3ftimer.USB_PERMISSION";


    // Commands from timer
    static final String FT_WIND_LEGAL = "C";
    static final String FT_RACE_COMPLETE = "E";
    static final String FT_LEG_COMPLETE = "P";
    static final String FT_READY = "R";
    static final String FT_WIND_ILLEGAL = "W";
    static final String FT_START_BUTTON = "S";

    // Commands to timer
    static final String TT_ABORT = "A";
    static final String TT_ADDITIONAL_BUZZER = "B";
    static final String TT_LAUNCH = "S";
    static final String TT_RESEND_TIME = "T";


    static final String ENCODING = "US-ASCII";

    static final String ICN_CONN = "on_usb";
    static final String ICN_DISCONN = "off_usb";

    private Driver mDriver;
    private String mBuffer = "";
    public int mTimerStatus = 0;

    public boolean mBoardConnected = false;

    Bundle mExtras;

    public ReadThread readThread;
    private byte [] usbdata;

    private Intent mIntent;
    private PendingIntent mPermissionIntent;
    private UsbManager mUsbmanager;
    private UsbAccessory mUsbaccessory;

    private ParcelFileDescriptor filedescriptor = null;
    private FileInputStream inputstream = null;
    private FileOutputStream outputstream = null;
    private boolean mPermissionRequestPending = false;
    private boolean mRequiresConfig = true;

    private boolean isRunning = false;

    static class UsbDeviceDescriptor {
        private String model;
        private String manufacturer;
        private String version;

        public UsbDeviceDescriptor(String mod, String man, String ver) {
            model = mod;
            manufacturer = man;
            version = ver;
        }
    }

    private UsbDeviceDescriptor[] supportedUsbDevices = {
            new UsbDeviceDescriptor("Android Acessory FT312D", "FTDI", "2.0")
    };

    /*
     * General life-cycle function overrides
     */

    @Override
    public void onCreate() {
        super.onCreate();
        mDriver = new Driver(this);

        this.registerReceiver(onBroadcast, new IntentFilter(IComm.RCV_UPDATE_FROM_UI));
        Log.d("DBG", "onCreate");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;

        if (mDriver != null)
            mDriver.destroy();
        mDriver = null;

        try {
            this.unregisterReceiver(onBroadcast);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        try{
            if(filedescriptor != null)
                filedescriptor.close();

        } catch (IOException e){}

        try {
            if(inputstream != null)
                inputstream.close();
        } catch(IOException e){}

        try {
            if(outputstream != null)
                outputstream.close();

        } catch (IOException e){}

        filedescriptor = null;
        inputstream = null;
        outputstream = null;
    }

    public static void startDriver(RaceActivity context, String inputSource, Integer race_id, Bundle params) {
        if (inputSource.equals(context.getString(R.string.USB_OA))) {
            Intent i = new Intent(IComm.RCV_UPDATE);
            i.putExtra("icon", ICN_DISCONN);
            i.putExtra(IComm.MSG_SERVICE_CALLBACK, "driver_stopped");
            context.sendBroadcast(i);

            Intent serviceIntent = new Intent(context, USBOpenAccessoryService.class);
            serviceIntent.putExtras(params);
            serviceIntent.putExtra("com.marktreble.f3ftimer.race_id", race_id);
            context.startService(serviceIntent);
        }
    }

    public static boolean stop(RaceActivity context) {
        if (context.isServiceRunning("com.marktreble.f3ftimer.driver.USBOpenAccessoryService")) {
            Intent serviceIntent = new Intent(context, USBOpenAccessoryService.class);
            context.stopService(serviceIntent);
            return true;
        }
        return false;
    }

    // Binding for UI->Service Communication
    private BroadcastReceiver onBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(IComm.MSG_UI_CALLBACK)) {
                Bundle extras = intent.getExtras();
                if (extras == null) {
                    return;
                }

                String data = extras.getString(IComm.MSG_UI_CALLBACK, "");
                Log.i("USB SERVICE UI->Service", data);

                if (data.equals("get_connection_status")) {
                    if (mBoardConnected) {
                        driverConnected();

                    } else {
                        driverDisconnected();
                    }
                }
            }

            String action = intent.getAction();
            if (action.equals(ACTION_USB_PERMISSION)) {
                mPermissionRequestPending = false;
                UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    Log.d("DBG", "Permission Granted");
                    openAccessory(accessory);
                    return;
                }

                // End the driver
                // User does not have permission
                Intent serviceIntent = new Intent(context, USBOpenAccessoryService.class);
                context.stopService(serviceIntent);
                Log.d("DBG", "Permission declined");
            }

            if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                // Board has become detached... ?
                Intent serviceIntent = new Intent(context, USBOpenAccessoryService.class);
                context.stopService(serviceIntent);
                Log.d("DBG", "Accessory Detached");
            }
        }
    };

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        Log.d("DBG", "onStartCommand");

        super.onStartCommand(intent, flags, startId);

        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras == null) {
                Log.d("DBG", "no Extras sent :(");
                return START_STICKY;
            }
            mIntent = intent;
            mExtras = extras;
        } else {
            Log.d("DBG", "no Intent sent :(");
            if (mIntent != null) {
                Log.d("DBG", "mIntent is good :)");
            }
        }
        final Runnable make_connection = new Runnable() {
            @Override
            public void run() {
                if (!connect() && mDriver != null) {
                    new Handler().postDelayed(this, 100);
                }
            }
        };

        new Handler().postDelayed(make_connection, 100);

        Log.d("DBG", "onStartCommand::Good Startup");

        return (START_STICKY);
    }

    public boolean connect() {
        Log.d("DBG", "Connect");

        mUsbmanager = (UsbManager) getSystemService(Context.USB_SERVICE);

        if (mUsbmanager == null) {
            return false;
        }
        Log.d("DBG", "Got USBManager");

        UsbAccessory[] accessories = mUsbmanager.getAccessoryList();
        if (accessories == null) {
            return false;
        }
        Log.d("DBG", "Got Accessory List");

        mUsbaccessory = accessories[0];

        if (!isSupportedAccessory(mUsbaccessory)) {
            return false;
        }
        Log.d("DBG", "Accessory Supported");

        if (!mUsbmanager.hasPermission(mUsbaccessory)) {
            Log.d("DBG", "Permission Required");
            Log.d("DBG", "Permission Pending: " + mPermissionRequestPending);
            if (!mPermissionRequestPending) {
                mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                mUsbmanager.requestPermission(mUsbaccessory, mPermissionIntent);
                mPermissionRequestPending = true;
                mRequiresConfig = true;
            }
            return true;
        }
        Log.d("DBG", "Permission Granted");
        openAccessory(mUsbaccessory);
        return true;
    }

    private void openAccessory(UsbAccessory accessory) {
        Log.d("DBG", "Opening Accessory");
        if (mUsbaccessory == null) {
            mUsbaccessory = accessory;
        }
        Log.d("DBG", "Opening Accessory");
        Log.d("DBG", accessory.toString());
        filedescriptor = mUsbmanager.openAccessory(accessory);
        if(filedescriptor != null) {
            FileDescriptor fd = filedescriptor.getFileDescriptor();

            Log.d("DBG", "Creating Streams");
            inputstream = new FileInputStream(fd);
            outputstream = new FileOutputStream(fd);
            /*check if any of them are null*/
            if (inputstream == null || outputstream == null) {
                return;
            }

            Log.d("DBG", "Configuring");
            if (mRequiresConfig) {
                setConfig();
                mRequiresConfig = false;
            } else {
                Log.d("DBG", "Config Not Required");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            // Start Read Thread
            readThread = new ReadThread(inputstream);
            readThread.start();

            mBoardConnected = true;
            mDriver.start(mIntent);
            driverConnected();
        } else {
            Log.d("DBG", "Failed to get FileDescriptor");
        }
    }

    private boolean isSupportedAccessory(UsbAccessory accessory) {
        boolean isSupported = false;
        for (UsbDeviceDescriptor dd: supportedUsbDevices) {
            if (accessory.getManufacturer().equals(dd.manufacturer)
                && accessory.getModel().equals(dd.model)
                && accessory.getVersion().equals(dd.version)) {
                isSupported = true;
            }
        }
        return isSupported;
    }

    private void setConfig() {
        if (mExtras != null) {
            Log.d("DBG","Getting Config");

            int baud = 2400;
            String strBaudrate = mExtras.getString(Pref.USB_BAUDRATE, Pref.USB_BAUDRATE_DEFAULT);
            try {
                baud = Integer.parseInt(strBaudrate, 10);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            String strDatabits = mExtras.getString(Pref.USB_DATABITS, Pref.USB_DATABITS_DEFAULT);

            byte dataBits = 8;
            try {
                dataBits = (byte)Integer.parseInt(strDatabits, 10);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            String strStopbits = mExtras.getString(Pref.USB_STOPBITS, Pref.USB_STOPBITS_DEFAULT);

            byte stopBits = 1;
            try {
                stopBits = (byte)Integer.parseInt(strStopbits, 10);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            String strParity = mExtras.getString(Pref.USB_PARITY, Pref.USB_PARITY_DEFAULT);

            byte parity = 0;
            switch (strParity) {
                case "None":
                    parity = 0;
                    break;
                case "Odd":
                    parity = 1;
                    break;
                case "Even":
                    parity = 2;
                    break;
                case "Mark":
                    parity = 3;
                    break;
                case "Space":
                    parity = 4;
                    break;
            }

            byte flowControl = 0;

            byte[] configdata = new byte[8];

            configdata[0] = (byte) baud;
            configdata[1] = (byte) (baud >> 8);
            configdata[2] = (byte) (baud >> 16);
            configdata[3] = (byte) (baud >> 24);

            configdata[4] = dataBits;
            configdata[5] = stopBits;
            configdata[6] = parity;
            configdata[7] = flowControl;

            /*send the UART configuration packet*/
            Log.d("DBG","Sending Config");
            Log.d("DBG", configdata.toString());
            try {
                outputstream.write(configdata, 0, configdata.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.d("DBG", "No Extras?");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "BINDING");

        return null;
    }

    private class ReadThread  extends Thread {
        FileInputStream instream;

        ReadThread(FileInputStream stream){
            instream = stream;
            usbdata = new byte[1024];
            this.setPriority(Thread.MAX_PRIORITY);
        }

        public void run() {
            isRunning = true;
            while(isRunning == true) {
                Log.d("ReadThread", "Running");
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Log.d("ReadThread", "Interrupted Exception");
                    isRunning = false;
                    e.printStackTrace();
                }

                try {
                    if (instream != null) {
                        Log.d("ReadThread", "instream");
                        // int readcount = instream.available();
                        int readcount = instream.read(usbdata,0,1024);
                        Log.d("ReadThread", Integer.toString(readcount));
                        if (readcount > 0) {


                            char[] charArray = (new String(usbdata, 0, readcount)).toCharArray();

                            StringBuilder sb = new StringBuilder(charArray.length);
                            StringBuilder hexString = new StringBuilder();
                            for (char c : charArray) {
                                sb.append(c);

                                String hex = Integer.toHexString(0xFF & c);
                                if (hex.length() == 1) {
                                    // could use a for loop, but we're only dealing with a single byte
                                    hexString.append('0');
                                }
                                hexString.append(hex);
                            }
                            Log.d("ReadThread", hexString.toString());

                            String str_in = mBuffer + sb.toString().trim();
                            int len = str_in.length();
                            if (len > 0) {

                                String lastchar = hexString.substring(hexString.length() - 2, hexString.length());
                                if (lastchar.equals("0d") || lastchar.equals("0a")) {
                                    // Clear the buffer
                                    mBuffer = "";

                                    Log.d("ReadThread", "TAKING: " + str_in);

                                    // Get code (first char)
                                    String code = str_in.substring(0, 1);

                                    // We have data/command from the timer, pass this on to the server
                                    if (code.equals(FT_START_BUTTON)) {
                                        mDriver.startPressed();
                                    } else if (code.equals(FT_WIND_LEGAL)) {
                                        mDriver.windLegal();
                                    } else if (code.equals(FT_WIND_ILLEGAL)) {
                                        mDriver.windIllegal();
                                    } else if (code.equals(FT_READY)) {
                                        mTimerStatus = 0;
                                        mDriver.ready();
                                    } else if (code.equals(FT_LEG_COMPLETE)) {
                                        switch (mTimerStatus) {
                                            case 0:
                                                mDriver.offCourse();
                                                break;
                                            case 1:
                                                mDriver.onCourse();
                                                break;
                                            default:
                                                mDriver.legComplete();
                                                break;

                                        }
                                        mTimerStatus++;
                                    } else if (code.equals(FT_RACE_COMPLETE)) {
                                        // Make sure we get 8 bytes before proceeding
                                        Log.d("ReadThread", "LENGTH: " + str_in.length());
                                        if (str_in.length() < 8) {
                                            mBuffer = str_in;
                                        } else {
                                            // Any more than 8 chars should be passed on to the next loop
                                            mBuffer = str_in.substring(8);
                                            // Don't take more than 8 or parseFloat will cause an exception + reflight!
                                            str_in = str_in.substring(0, 8);
                                            mDriver.mPilot_Time = Float.parseFloat(str_in.substring(2).trim());
                                            mDriver.runComplete();
                                            // Reset these here, as sometimes READY is not received!?
                                            mTimerStatus = 0;
                                            mDriver.ready();
                                            mBuffer = "";
                                        }
                                    }

                                } else {
                                    // Save the characters to the buffer for the next cycle
                                    mBuffer = str_in;
                                }
                            }
                        }
                    }
                }
                catch (IOException e) {
                    Log.d("ReadThread", "IO Exception");
                    e.printStackTrace();
                }
            }
            Log.d("ReadThread", "No Longer Running");
        }
    }

    // Output - Send commands to hardware
    private void sendCmd(String cmd) {
        byte[] bytes = cmd.getBytes(Charset.forName(ENCODING));
        try {
            if(outputstream != null){
                outputstream.write(bytes, 0, bytes.length);
            } else {
                Intent i = new Intent(IComm.RCV_UPDATE);
                i.putExtra(IComm.MSG_SERVICE_CALLBACK, "no_out_stream");
                sendBroadcast(i);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Intent i = new Intent(IComm.RCV_UPDATE);
            i.putExtra(IComm.MSG_SERVICE_CALLBACK, "no_out_stream");
            sendBroadcast(i);
        }
    }

    // Driver Interface implementations
    public void driverConnected() {
        mDriver.driverConnected(ICN_CONN);
    }

    public void driverDisconnected() {
        mDriver.driverDisconnected(ICN_DISCONN);
    }

    public void sendLaunch() {
        this.sendCmd(TT_LAUNCH);
        mTimerStatus = 0;
    }

    public void sendAbort() {
        this.sendCmd(TT_ABORT);
    }

    public void sendAdditionalBuzzer() {
        this.sendCmd(TT_ADDITIONAL_BUZZER);
    }

    public void sendResendTime() {
        this.sendCmd(TT_RESEND_TIME);
    }

    public void baseA() {
    }

    public void baseB() {
    }

    public void finished(String time) {
    }

}
