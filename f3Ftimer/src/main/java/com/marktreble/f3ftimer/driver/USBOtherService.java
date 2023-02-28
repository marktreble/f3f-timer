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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.constants.IComm;
import com.marktreble.f3ftimer.constants.Pref;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class USBOtherService extends Service implements DriverInterface {

    private static final String TAG = "USBOtherService";

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

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager = null;

    private int mBaudRate;
    private int mDataBits;
    private int mStopBits;
    private int mParity;

    private boolean mUnsupportedMessageSent = false;
    private int mVendorId = 0;
    private int mProductId = 0;

    /*
     * General life-cycle function overrides
     */

    @Override
    public void onCreate() {
        super.onCreate();
        mDriver = new Driver(this);

        this.registerReceiver(onBroadcast, new IntentFilter(IComm.RCV_UPDATE_FROM_UI));
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDriver != null)
            mDriver.destroy();
        mDriver = null;

        if (mSerialIoManager != null) {
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }

        try {
            this.unregisterReceiver(onBroadcast);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public static void startDriver(RaceActivity context, String inputSource, Integer race_id, Bundle params) {
        if (inputSource.equals(context.getString(R.string.USB_OTHER))) {
            Intent i = new Intent(IComm.RCV_UPDATE);
            i.putExtra("icon", ICN_DISCONN);
            i.putExtra(IComm.MSG_SERVICE_CALLBACK, "driver_stopped");
            context.sendBroadcast(i);

            Intent serviceIntent = new Intent(context, USBOtherService.class);
            serviceIntent.putExtras(params);
            serviceIntent.putExtra("com.marktreble.f3ftimer.race_id", race_id);
            context.startService(serviceIntent);
        }
    }

    public static boolean stop(RaceActivity context) {
        if (context.isServiceRunning("com.marktreble.f3ftimer.driver.USBOtherService")) {
            Intent serviceIntent = new Intent(context, USBOtherService.class);
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
        }
    };

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras == null) {
                return START_STICKY;
            }

            mBaudRate = 2400;
            String baudrate = extras.getString(Pref.USB_BAUDRATE, Pref.USB_BAUDRATE_DEFAULT);
            try {
                mBaudRate = Integer.parseInt(baudrate, 10);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            String databits = extras.getString(Pref.USB_DATABITS, Pref.USB_DATABITS_DEFAULT);

            switch (databits) {
                case "5":
                    mDataBits = UsbSerialPort.DATABITS_5;
                    break;
                case "6":
                    mDataBits = UsbSerialPort.DATABITS_6;
                    break;
                case "7":
                    mDataBits = UsbSerialPort.DATABITS_7;
                    break;
                case "8":
                default:
                    mDataBits = UsbSerialPort.DATABITS_8;
                    break;
            }

            String stopbits = extras.getString(Pref.USB_STOPBITS, Pref.USB_STOPBITS_DEFAULT);

            switch (stopbits) {
                case "2":
                    mStopBits = UsbSerialPort.STOPBITS_2;
                    break;
                case "1":
                default:
                    mStopBits = UsbSerialPort.STOPBITS_1;
                    break;
            }

            String parity = extras.getString(Pref.USB_PARITY, Pref.USB_PARITY_DEFAULT);
            switch (parity) {
                case "Odd":
                    mParity = UsbSerialPort.PARITY_ODD;
                    break;
                case "Even":
                    mParity = UsbSerialPort.PARITY_EVEN;
                    break;
                case "Mark":
                    mParity = UsbSerialPort.PARITY_MARK;
                    break;
                case "Space":
                    mParity = UsbSerialPort.PARITY_SPACE;
                    break;
                case "None":
                default:
                    mParity = UsbSerialPort.PARITY_NONE;
                    break;
            }
        }
        final Runnable make_connection = new Runnable() {
            @Override
            public void run() {
                if (!connect(intent) && mDriver != null) {
                    new Handler(Looper.getMainLooper()).postDelayed(this, 100);
                }
            }
        };

        new Handler(Looper.getMainLooper()).postDelayed(make_connection, 100);

        return (START_STICKY);
    }

    public boolean connect(Intent intent) {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        if (usbManager == null) {
            return false;
        }

        UsbSerialProber prober = UsbSerialProber.getDefaultProber();
        final List<UsbSerialDriver> drivers = prober.findAllDrivers(usbManager);

        List<UsbSerialPort> result = new ArrayList<>();


        Log.i(TAG, "Trying connection...");
        SystemClock.sleep(1000);
        for (final UsbSerialDriver driver : drivers) {
            final List<UsbSerialPort> ports = driver.getPorts();
            result.addAll(ports);
            Log.i(TAG, "Added driver: " + driver.getClass().toString());
        }

        if (result.size() == 0) {
            if (prober.mVendorId > 0 && prober.mProductId > 0) {
                if (mVendorId != prober.mVendorId
                        && mProductId != prober.mProductId) {
                    mUnsupportedMessageSent = false;
                }
                if (!mUnsupportedMessageSent) {
                    Log.i(TAG, "Devices found");
                    // Notify user of the vendor/productId
                    Intent i = new Intent(IComm.RCV_UPDATE);
                    i.putExtra(IComm.MSG_SERVICE_CALLBACK, "unsupported");
                    i.putExtra("vendorId", String.format("0x%04X", prober.mVendorId));
                    i.putExtra("productId", String.format("0x%04X", prober.mProductId));
                    sendBroadcast(i);
                    mUnsupportedMessageSent = true;
                }
                mVendorId = prober.mVendorId;
                mProductId = prober.mProductId;
            } else {
                Log.i(TAG, "No Devices found");
            }

        } else {
            Log.i(TAG, "Getting first device");
            UsbSerialPort port = result.get(0);

            UsbDeviceConnection connection = usbManager.openDevice(port.getDriver().getDevice());
            if (connection != null) {
                Log.i(TAG, "Trying to Open Port");

                try {
                    port.open(connection);
                    port.setParameters(mBaudRate, mDataBits, mStopBits, mParity);
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        port.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                    port = null;
                }

                if (port != null) {
                    Log.i(TAG, "Port OK!");
                    if (mSerialIoManager != null) {
                        mSerialIoManager.stop();
                        mSerialIoManager = null;
                    }
                    mSerialIoManager = new SerialInputOutputManager(port, mListener);
                    mExecutor.submit(mSerialIoManager);
                }
            }
            //if (mSerialIoManager == null) return false;
            mBoardConnected = true;
            mDriver.start(intent);
            driverConnected();
            return true;
        }
        return false;
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "BINDING");

        return null;
    }

    // Input - Listener Loop
    private final SerialInputOutputManager.Listener mListener = new SerialInputOutputManager.Listener() {

        @Override
        public void onRunError(Exception e) {
            Log.d(TAG, "Runner stopped.");
            // Disconnection ??
            mBoardConnected = false;
            mDriver.destroy();
        }

        @Override
        public void onNewData(final byte[] data) {

            char[] charArray = (new String(data, 0, data.length)).toCharArray();

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

            String str_in = mBuffer + sb.toString().trim();
            int len = str_in.length();
            if (len > 0) {

                String lastchar = hexString.substring(hexString.length() - 2, hexString.length());
                if (lastchar.equals("0d") || lastchar.equals("0a")) {
                    // Clear the buffer
                    mBuffer = "";

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
                        // Make sure we get 9 bytes before proceeding
                        if (str_in.length() < 9) {
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
    };


    // Output - Send commands to hardware
    private void sendCmd(String cmd) {
        byte[] bytes = cmd.getBytes(Charset.forName(ENCODING));
        int sz = bytes.length;
        if (sz > 0) {
            if (mSerialIoManager != null) {
                Log.i(TAG, "Sending Data: " + cmd);
                try {
                    mSerialIoManager.writeAsync(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // Call alert dialog on UI Thread "No Output Stream Available"
                Intent i = new Intent(IComm.RCV_UPDATE);
                i.putExtra(IComm.MSG_SERVICE_CALLBACK, "no_out_stream");
                sendBroadcast(i);


            }
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
