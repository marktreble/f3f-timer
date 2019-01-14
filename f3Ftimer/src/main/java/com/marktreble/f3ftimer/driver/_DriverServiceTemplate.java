package com.marktreble.f3ftimer.driver;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class _DriverServiceTemplate extends Service implements DriverInterface {

    private Driver mDriver;

    /*
     * General life-cycle function overrides
     */

    @Override
    public void onCreate() {
        super.onCreate();
        mDriver = new Driver(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDriver.destroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        mDriver.start(intent);

        return (START_STICKY);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Input - Listener Loop
    // TODO

    // Output - Send commands
    private void sendCmd(String cmd) {
        // TODO
    }

    // Driver Interface implementations
    public void driverConnected() {
    }

    public void driverDisconnected() {
    }

    public void sendLaunch() {
    }

    public void sendAbort() {
    }

    public void sendAdditionalBuzzer() {
    }

    public void sendResendTime() {
    }

    public void baseA() {
    }

    public void baseB() {
    }

    public void finished(String time) {
    }
}
