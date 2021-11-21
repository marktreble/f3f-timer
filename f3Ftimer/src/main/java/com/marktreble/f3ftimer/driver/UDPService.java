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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.constants.IComm;
import com.marktreble.f3ftimer.constants.Pref;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPService extends Service implements DriverInterface, Thread.UncaughtExceptionHandler {

    private static final String TAG = "UDPService";

    private Driver mDriver;

    public int mTimerStatus = 0;

    public boolean mBaseAConnected = false;
    public boolean mBaseBConnected = false;

    private String mBaseAIP = "";
    private String mBaseBIP = "";

    private DatagramReceiver datagramReceiver = null;

    static final String ICN_CONN_A = "amber";
    static final String ICN_CONN_A_B = "on";
    static final String ICN_DISCONN = "off";

    private Handler mWindEmulator;
    private static float mSlopeOrientation = 0.0f;

    int mWindSpeedCounterSeconds = 0;
    int mWindSpeedCounter = 0;
    long mWindTimestamp;

    private static final int MAX_UDP_DATAGRAM_LEN = 64;
    private static final int UDP_SERVER_PORT = 4445;


    /*
     * General life-cycle function overrides
     */

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        stopSelf();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDriver = new Driver(this);

        this.registerReceiver(onBroadcast, new IntentFilter(IComm.RCV_UPDATE_FROM_UI));

        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Destroyed");
        super.onDestroy();
        if (mDriver != null)
            mDriver.destroy();

        try {
            this.unregisterReceiver(onBroadcast);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        if (datagramReceiver != null) {
            datagramReceiver.kill();
        }

        mBaseAConnected = false;
        mBaseBConnected = false;
        driverDisconnected();

        mWindEmulator.removeCallbacksAndMessages(null);

    }

    public static void startDriver(Context context, String inputSource, Integer race_id, Bundle params) {
        if (inputSource.equals(context.getString(R.string.UDP))) {
            Intent i = new Intent(IComm.RCV_UPDATE);
            i.putExtra("icon", ICN_DISCONN);
            i.putExtra(IComm.MSG_SERVICE_CALLBACK, "driver_stopped");
            context.sendBroadcast(i);

            Intent serviceIntent = new Intent(context, UDPService.class);
            serviceIntent.putExtras(params);
            serviceIntent.putExtra("com.marktreble.f3ftimer.race_id", race_id);
            context.startService(serviceIntent);
        }
    }

    public static boolean stop(RaceActivity context) {
        if (context.isServiceRunning("com.marktreble.f3ftimer.driver.UDPService")) {
            Log.d("SERVER STOPPED", Log.getStackTraceString(new Exception()));
            Intent serviceIntent = new Intent(context, UDPService.class);
            context.stopService(serviceIntent);
            return true;
        }
        return false;
    }

    private class DatagramReceiver extends Thread {
        private boolean bKeepRunning = true;

        public void run() {
            String message;
            String ipAddress;
            byte[] lmessage = new byte[MAX_UDP_DATAGRAM_LEN];
            DatagramPacket packet = new DatagramPacket(lmessage, lmessage.length);

            try {
                DatagramSocket socket = new DatagramSocket(UDP_SERVER_PORT);
                socket.setReuseAddress(true);

                while(bKeepRunning) {
                    socket.receive(packet);
                    message = new String(lmessage, 0, packet.getLength());
                    ipAddress = packet.getAddress().toString();

                    Log.i(TAG, String.format("%s %s", ipAddress, message));

                    if (message.equals("Event")) {
                        // Handle buzz case ASAP
                        if (mBaseAConnected
                            && mBaseBConnected
                            && mDriver.mModelLaunched) {
                            buzz(ipAddress);
                        }

                        // Reconnect if the IP is known
                        if (mBaseAIP.equals(ipAddress)) {
                            mBaseAConnected = true;
                        }
                        if (mBaseBIP.equals(ipAddress)) {
                            mBaseBConnected = true;
                        }
                    }

                    if (message.equals("UDPBeep Thread Start")) {
                        // First try a reconnect if we already have the IP
                        if (mBaseAIP.equals(ipAddress)) {
                            mBaseAConnected = true;
                        }
                        if (mBaseBIP.equals(ipAddress)) {
                            mBaseBConnected = true;
                        }

                        // Connection from an unknown IP
                        // Take the first unknown connection as base A
                        // and the second unknown connection as base B
                        if (!mBaseAConnected) {
                            connectBaseA(ipAddress);
                        } else if (!mBaseBConnected) {
                            connectBaseB(ipAddress);
                        }
                    }

                }

                socket.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        public void kill() {
            bKeepRunning = false;
        }
    }

    // Binding for UI->Service Communication
    private final BroadcastReceiver onBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(IComm.MSG_UI_CALLBACK)) {
                Bundle extras = intent.getExtras();
                if (extras == null)
                    extras = new Bundle();

                String data = extras.getString(IComm.MSG_UI_CALLBACK, "");
                Log.i(TAG, data);

                if (data.equals("get_connection_status")) {
                    if (mBaseAConnected || mBaseBConnected) {
                        driverConnected();
                    } else {
                        driverDisconnected();
                    }
                }

                if (data.equals("pref_wind_angle_offset")) {
                    mSlopeOrientation = Float.valueOf(intent.getExtras().getString(IComm.MSG_VALUE));
                }
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                mBaseAIP = extras.getString(Pref.BASEA_IP);
                mBaseBIP = extras.getString(Pref.BASEB_IP);
            }
        }

        driverDisconnected();

        mDriver.start(intent);

        datagramReceiver = new DatagramReceiver();
        datagramReceiver.start();

        // Output dummy wind readings
        if (intent != null) {
            Bundle extras = intent.getExtras();
            mWindTimestamp = System.currentTimeMillis();
            mSlopeOrientation = 0.f;
            if (extras != null) {
                mSlopeOrientation = Float.parseFloat(extras.getString("pref_wind_angle_offset", "0.0"));
            }
        }

        mWindEmulator = new Handler();
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                Intent i = new Intent(IComm.RCV_UPDATE);
                float wind_angle_absolute = mSlopeOrientation + (float) (Math.random() * 2) - 1.f;
                float wind_angle_relative = wind_angle_absolute - mSlopeOrientation;
                if (wind_angle_absolute > 180 + mSlopeOrientation) {
                    wind_angle_relative -= 360;
                }
                float wind_speed = 6f + (float) (Math.random() * 3) - 1.5f;
                if (wind_speed < 3 || wind_speed > 25) {
                    mWindSpeedCounter++;
                } else {
                    mWindSpeedCounter = 0;
                    mWindSpeedCounterSeconds = 0;
                    mWindTimestamp = System.currentTimeMillis();
                }
                if (mWindSpeedCounter == 2) {
                    mWindSpeedCounterSeconds++;
                    mWindSpeedCounter = 0;
                }

                boolean windLegal;
                if ((wind_angle_relative > 45 || wind_angle_relative < -45) || mWindSpeedCounterSeconds >= 20
                        || (System.currentTimeMillis() - mWindTimestamp >= 20000)) {
                    mWindSpeedCounterSeconds = 20;
                    mDriver.windIllegal();
                    windLegal = false;
                } else {
                    mDriver.windLegal();
                    windLegal = true;
                }

                String wind_data = formatWindValues(windLegal, wind_angle_absolute, wind_angle_relative, wind_speed, 20 - mWindSpeedCounterSeconds);
                i.putExtra("com.marktreble.f3ftimer.value.wind_values", wind_data);
                sendBroadcast(i);

                mWindEmulator.postDelayed(this, 1000);
            }
        };
        mWindEmulator.post(runnable);

        return (START_STICKY);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Input - Listener Loop
    private void base(String base) {
        switch (mTimerStatus) {
            case 0:
                if (base.equals("A")) {
                    mDriver.offCourse();
                }
                break;
            case 1:
                if (base.equals("A")) {
                    mDriver.onCourse();
                }
                break;
            default:
                mDriver.legComplete();
                break;

        }
        mTimerStatus++;

    }

    // Driver Interface implementations
    public void driverConnected() {
        String icon = "";
        if (mBaseAConnected) {
            icon = ICN_CONN_A;
        }
        if (mBaseBConnected) {
            icon = ICN_CONN_A_B;
        }
        mDriver.driverConnected(icon);
    }

    public void driverDisconnected() {
        mDriver.driverDisconnected(ICN_DISCONN);
    }

    public void sendLaunch() {
        mTimerStatus = 0;
    }

    public void sendAbort() {
    }

    public void sendAdditionalBuzzer() {
    }

    public void sendResendTime() {
    }

    public void baseA() {
        if ((mTimerStatus == 0) || (mTimerStatus % 2 == 1))
            base("A");
    }

    public void baseB() {
        if ((mTimerStatus > 0) && (mTimerStatus % 2 == 0))
            base("B");
    }

    private void connectBaseA(String message) {
        Intent i = new Intent(IComm.RCV_UPDATE);
        i.putExtra(Pref.BASEA_IP, message);
        i.putExtra(IComm.MSG_SERVICE_CALLBACK, Pref.BASEA_IP);
        sendBroadcast(i);

        mBaseAIP = message;
        mBaseAConnected = true;
        driverConnected();
    }

    private void connectBaseB(String message) {
        Intent i = new Intent(IComm.RCV_UPDATE);
        i.putExtra(Pref.BASEB_IP, message);
        i.putExtra(IComm.MSG_SERVICE_CALLBACK, Pref.BASEB_IP);
        sendBroadcast(i);

        mBaseBIP = message;
        mBaseBConnected = true;
        driverConnected();
    }

    private void buzz(String message) {
        if (message.equals(mBaseAIP)) {
            Log.i(TAG, String.format("%s: BASE A", message));
            baseA();
        }

        if (message.equals(mBaseBIP)) {
            Log.i(TAG, String.format("%s: BASE B", message));
            baseB();
        }
    }


    public void finished(String time) {
        mDriver.mPilot_Time = Float.parseFloat(time.trim().replace(",", "."));
        mDriver.runComplete();
        mTimerStatus = 0;
        mDriver.ready();

    }

    public String formatWindValues(boolean windLegal, float windAngleAbsolute, float windAngleRelative, float windSpeed, int windSpeedCounter) {
        String str;
        if (windLegal && windSpeedCounter == 20) {
            str = String.format(" a: %.2f°", windAngleAbsolute)
                    + String.format(" r: %.2f°", windAngleRelative)
                    + String.format(" %.2fm/s", windSpeed)
                    + "   legal";
        } else if (windLegal) {
            str = String.format(" a: %.2f°", windAngleAbsolute)
                    + String.format(" r: %.2f°", windAngleRelative)
                    + String.format(" %.2fm/s", windSpeed)
                    + String.format(" legal (%d s)", windSpeedCounter);
        } else {
            str = String.format(" a: %.2f°", windAngleAbsolute)
                    + String.format(" r: %.2f°", windAngleRelative)
                    + String.format(" %.2fm/s", windSpeed)
                    + " illegal";
        }

        return str;
    }
}
