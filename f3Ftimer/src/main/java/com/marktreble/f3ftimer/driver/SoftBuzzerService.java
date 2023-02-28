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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.constants.IComm;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

public class SoftBuzzerService extends Service implements DriverInterface, Thread.UncaughtExceptionHandler {

    private static final String TAG = "SoftBuzzerService";

    private Driver mDriver;

    public int mTimerStatus = 0;

    public boolean mBoardConnected = false;

    static final String ICN_CONN = "on";
    static final String ICN_DISCONN = "off";

    static ServiceConnection softBuzzerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "SoftBuzzerService BOUND = true");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "SoftBuzzerService BOUND = false");
        }
    };

    /*
     * General life-cycle function overrides
     */

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
        stopSelf();
    }

    public static void startDriver(Context context, String inputSource, Integer race_id, Bundle params) {
        if (inputSource.equals(context.getString(R.string.Demo))) {
            Log.i(TAG, "Starting");
            Intent i = new Intent(IComm.RCV_UPDATE);
            i.putExtra("icon", ICN_DISCONN);
            i.putExtra(IComm.MSG_SERVICE_CALLBACK, "driver_stopped");
            context.sendBroadcast(i);

            Intent serviceIntent = new Intent(context, SoftBuzzerService.class);
            serviceIntent.putExtras(params);
            serviceIntent.putExtra("com.marktreble.f3ftimer.race_id", race_id);
            Log.i(TAG, "Calling StartService");
            Log.i(TAG, context.toString());
            context.startService(serviceIntent);
            context.bindService(serviceIntent, softBuzzerServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDriver = new Driver(this);

        this.registerReceiver(onBroadcast, new IntentFilter(IComm.RCV_UPDATE_FROM_UI));

        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "ONBIND");
        return mBinder;
    }

    private class LocalBinder extends Binder {
        SoftBuzzerService getService() {
            return SoftBuzzerService.this;
        }
    }


    private final IBinder mBinder = new LocalBinder();

    public static boolean stop(RaceActivity context) {
        if (context.isServiceRunning("com.marktreble.f3ftimer.driver.SoftBuzzerService")) {
            //Log.d("SERVER STOPPED", Log.getStackTraceString(new Exception()));
            Intent serviceIntent = new Intent(context, SoftBuzzerService.class);
            context.stopService(serviceIntent);
            return true;
        }
        return false;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.i(TAG, "onStartCommand");
        driverDisconnected();

        mBoardConnected = true;
        mDriver.start(intent);
        driverConnected();

        Log.i(TAG, "Started");
        return (START_STICKY);
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
        mBoardConnected = false;
        driverDisconnected();

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
        mDriver.driverConnected(ICN_CONN);
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

    public void finished(String time) {
        mDriver.mPilot_Time = Float.parseFloat(time.trim().replace(",", "."));
        mDriver.runComplete();
        mTimerStatus = 0;
        mDriver.ready();

    }
}
