package com.marktreble.f3ftimer.driver;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

public class SoftBuzzerService extends Service implements DriverInterface, Thread.UncaughtExceptionHandler {

    private static final String TAG = "SoftBuzzerService";
    
	private Driver mDriver;

    public int mTimerStatus = 0;

    public boolean mBoardConnected = false;
	
	/*
	 * General life-cycle function overrides
	 */

    @Override
    public void uncaughtException(Thread thread, Throwable ex){
        stopSelf();
    }
    
	@Override
    public void onCreate() {
		super.onCreate();
		mDriver = new Driver(this);

        this.registerReceiver(onBroadcast, new IntentFilter("com.marktreble.f3ftimer.onUpdateFromUI"));

        Thread.setDefaultUncaughtExceptionHandler(this);
    }

	@Override
	public void onDestroy() {
        Log.i("DRIVER (SOFT BUZZER)", "Destroyed");
		super.onDestroy();
        if (mDriver != null)
		    mDriver.destroy();

        try {
            this.unregisterReceiver(onBroadcast);
        } catch (IllegalArgumentException e){
            e.printStackTrace();
        }
        mBoardConnected = false;
        callbackToUI("driver_stopped");

    }

    public static void startDriver(RaceActivity context, String inputSource, Integer race_id, Bundle params){
        if (inputSource.equals(context.getString(R.string.Demo))){
            Intent serviceIntent = new Intent(context, SoftBuzzerService.class);
            serviceIntent.putExtras(params);
            serviceIntent.putExtra("com.marktreble.f3ftimer.race_id", race_id);
            context.startService(serviceIntent);
        }
    }

    public static boolean stop(RaceActivity context){
        if (context.isServiceRunning("com.marktreble.f3ftimer.driver.SoftBuzzerService")) {
            Intent serviceIntent = new Intent(context, SoftBuzzerService.class);
            context.stopService(serviceIntent);
            return true;
        }
        return false;
    }
    
    // Binding for UI->Service Communication
    private BroadcastReceiver onBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("com.marktreble.f3ftimer.ui_callback")) {
                Bundle extras = intent.getExtras();
                String data = extras.getString("com.marktreble.f3ftimer.ui_callback");
                Log.i(TAG, data);

                if (data.equals("get_connection_status")) {
                    if (mBoardConnected){
                        callbackToUI("driver_started");

                    } else {
                        callbackToUI("driver_stopped");
                    }
                }
            }
        }
    };
    
	@Override
    public int onStartCommand(Intent intent, int flags, int startId){
    	super.onStartCommand(intent, flags, startId);
    	
    	mDriver.start(intent);
        mBoardConnected = true;
        
    	return (START_STICKY);    	
    }
       	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	// Input - Listener Loop
	private void base(String base){
        switch (mTimerStatus){
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
        
    }

    private void callbackToUI(String cmd){
        Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
        i.putExtra("com.marktreble.f3ftimer.service_callback", cmd);
        Log.d("CallBackToUI", cmd);
        this.sendBroadcast(i);
    }
	
	// Driver Interface implementations
	public void sendLaunch(){
        mTimerStatus = 0;
	}

	public void sendAbort(){
	}
	
	public void sendAdditionalBuzzer(){
	}
	
	public void sendResendTime(){
	}
    
    public void baseA(){
        Log.i(TAG + "BASEA", Integer.toString(mTimerStatus % 2));
        if ((mTimerStatus == 0) || (mTimerStatus%2 == 1))
            base("A");
    }
    
    public void baseB(){
        Log.i(TAG + "BASEB", Integer.toString(mTimerStatus % 2));
        if ((mTimerStatus>0) && (mTimerStatus%2 == 0))
            base("B");
    }
    
    public void finished(String time){
        Log.d(TAG + "TIME", time.trim());
        mDriver.mPilot_Time = Float.parseFloat(time.trim());
        Log.d(TAG + "TIME", Float.toString(mDriver.mPilot_Time) );
        mDriver.runComplete();
        mTimerStatus = 0;
        mDriver.ready();

    }
}
