package com.marktreble.f3ftimer.driver;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import static java.lang.Thread.sleep;

public class TcpIoService extends Service implements DriverInterface {

	private static final String TAG = "TcpIoService";
//	private static final String F3FTIMER_SERVER = "f3ftimer-server";
	private static final String F3FTIMER_SERVER = "192.168.1.2";
	//private static final String F3FTIMER_SERVER = "192.168.42.24";
	private static final int F3FTIMER_SERVER_PORT = 1234;

	// Commands from raspberrypi
	static final String FT_TURNA = "A";
	static final String FT_TURNB = "B";
	static final String FT_START = "S";
	static final String FT_CANCEL = "C";
	static final String FT_CANCEL_ZERO = "Z";
	static final String FT_PENALTY = "P";
	static final String FT_WIND = "W";
	static final String FT_TIME = "T";
	static final String FT_SPEECH = "X";

	static final String ICN_CONN = "on";
	static final String ICN_DISCONN = "off";

	private Intent mIntent;

	private int mTimerStatus = 0;
	private int mState = 0;
	private boolean mConnected = false;
	private boolean mConnecting = false;

	private static float mSlopeOrientation = 0.0f;

	int mWindSpeedCounterSeconds = 0;
	int mWindSpeedCounter = 0;
	long mWindTimestamp;

	private boolean mTurnA = false;
	private boolean mTurnB = false;
	private int mLeg = 0;

	private boolean timeAlreadyReceived;
	private boolean timeReceived;

	private boolean mReceivedAbort = false;

	private static boolean stopConnectThread = false;

	private static Driver mDriver;
	private static Socket mmSocket;
	private static InputStream mmInStream;
	private static OutputStream mmOutStream;
	private static Thread connectThread;
	private static Thread listenThread;
	private static SendThread sendThread;

	/*
	 * General life-cycle function overrides
	 */

	@Override
    public void onCreate() {
		super.onCreate();
		mDriver = new Driver(this);

		mSlopeOrientation = Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(this).getString("pref_wind_angle_offset", "0.0"));

		this.registerReceiver(onBroadcast, new IntentFilter("com.marktreble.f3ftimer.onUpdateFromUI"));
    }

	@Override
	public void onDestroy() {
		Log.i(TAG, "ONDESTROY!");
		super.onDestroy();

		mConnected = false;
		mConnecting = false;
		stopConnectThread = true;

		driverDisconnected();

		try {
			if (mmInStream != null) mmInStream.close();
			if (mmOutStream != null) mmOutStream.close();
			if (mmSocket != null) mmSocket.close();

		} catch (IOException e){
			e.printStackTrace();
		}

		try {
			this.unregisterReceiver(onBroadcast);
		} catch (IllegalArgumentException e){
			e.printStackTrace();
		}

		if (connectThread != null) {
			connectThread.interrupt();
			connectThread = null;
		}
		if (sendThread != null) {
			sendThread.interrupt();
			sendThread = null;
		}
		if (listenThread != null) {
			listenThread.interrupt();
			listenThread = null;
		}

		if (mDriver != null) {
			mDriver.destroy();
			mDriver = null;
		}
    }

	public static void startDriver(RaceActivity context, String inputSource, Integer race_id, Bundle params){
		if (inputSource.equals(context.getString(R.string.TCP_IO))){
			Intent serviceIntent = new Intent(context, TcpIoService.class);
			serviceIntent.putExtras(params);
			mSlopeOrientation = Float.parseFloat(params.getString("pref_wind_angle_offset"));
			serviceIntent.putExtra("com.marktreble.f3ftimer.race_id", race_id);
			context.startService(serviceIntent);
			Log.d(TAG, "TCP DRIVER STARTED");
		}
	}

	public static boolean stop(RaceActivity context){
		if (context != null && context.isServiceRunning("com.marktreble.f3ftimer.driver.TcpIoService")) {
			Log.i(TAG, "RUNNING");
			Intent serviceIntent = new Intent(context, TcpIoService.class);
			context.stopService(serviceIntent);
			return true;
		}
		Log.i(TAG, "NOT RUNNING??");
		return false;
	}

	// Binding for UI->Service Communication
	private BroadcastReceiver onBroadcast = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.hasExtra("com.marktreble.f3ftimer.ui_callback")) {
				Bundle extras = intent.getExtras();
				String data = extras.getString("com.marktreble.f3ftimer.ui_callback");
				Log.d(TAG, data);

				if (data == null) return;

				if (data.equals("get_connection_status")) {
					if (!mConnected) {
						driverDisconnected();
						if (!mConnecting) {
							startConnectThread();
						}
					} else {
						driverConnected();
					}
				}

				if (data.equals("model_launched")) {
					mState = 2;
				}

				if (data.equals("working_time_started")) {
					mState = 1;
				}

				if (data.equals("pref_wind_angle_offset")) {
					mSlopeOrientation = Float.valueOf(intent.getExtras().getString("com.marktreble.f3ftimer.value"));
					Log.d("TcpIoService", "pref_wind_angle_offset=" + mSlopeOrientation);
				}
			}
		}
	};

	@Override
    public int onStartCommand(Intent intent, int flags, int startId){
    	super.onStartCommand(intent, flags, startId);

		Log.d(TAG, "onStartCommand");
		mIntent = intent;

		if (!mConnected) {
			driverDisconnected();
			Log.d(TAG, "NOT CONNECTED");
			if (!mConnecting) {
				startConnectThread();
			}
		} else {
			Log.d(TAG, "ENABLED");
			driverConnected();
		}

		return (START_STICKY);
	}


	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void callbackToUI(String cmd){
		Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
		i.putExtra("com.marktreble.f3ftimer.service_callback", cmd);
		this.sendBroadcast(i);
	}

	private class SendThread extends Thread {
		private Handler handler;
		private Looper myLooper;

		@SuppressLint("HandlerLeak")
		private SendThread() {
			handler = new Handler() {
				public void handleMessage(Message msg) {
					try {
						String cmd = ((String)msg.obj);
						if (cmd.length() > 0)
							Log.i(TAG, "send Cmd \"" + cmd + "\" (" + cmd.length() + ")");
						mmOutStream.write(cmd.getBytes(), 0, cmd.length());
						mmOutStream.flush();
					} catch (Throwable e) {
						handleSocketThrowable(e);
					}
				}
			};
		}

		public void destroy() {
			myLooper.quit();
		}

		public void interrupt() {
			myLooper.quit();
			super.interrupt();
		}

		void sendCmd(String cmd) {
			Message msg = new Message();
			msg.obj = cmd;
			handler.sendMessage(msg);
		}

		@Override
		public void run() {
			setName("SendThread" + this.getId());
			Looper.prepare();
			myLooper = Looper.myLooper();
			Looper.loop();
		}
	}

	@Override
	public void driverConnected() {
		mDriver.driverConnected(ICN_CONN);
	}

	@Override
	public void driverDisconnected() {
		mDriver.driverDisconnected(ICN_DISCONN);
	}

	// Driver Interface implementations
	public void sendLaunch(){
		timeAlreadyReceived = false;
		timeReceived = false;
		if (sendThread != null) {
			Log.i(TAG, "sendLaunch");
			sendThread.sendCmd(FT_START + " ");
		}
	}

	public void sendAbort(){
		mState = 0;
		mTimerStatus = 0;
		mLeg = 0;

		if (sendThread != null && !mReceivedAbort) {
			Log.i(TAG, "sendAbort");
			sendThread.sendCmd(FT_CANCEL + " ");
		} else {
			Log.i(TAG, "received Abort -> no sendAbort");
			mReceivedAbort = false;
		}
	}

	public void sendAdditionalBuzzer(){}

	public void sendResendTime(){}

	public void baseA(){}
	public void baseB(){}

	public void finished(String time){
		if (!timeReceived) {
			try {
				sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (!timeAlreadyReceived) {
			timeAlreadyReceived = true;
			Log.d(TAG, "TIME " + time.trim());
			mDriver.mPilot_Time = Float.parseFloat(time.trim().replace(",", "."));
			Log.d(TAG, "TIME " + Float.toString(mDriver.mPilot_Time));
			mDriver.runComplete();
			mState = 0;
			mTimerStatus = 0;
			mLeg = 0;
			mDriver.ready();
		}
	}

	public void sendSpeechText(String lang, String text){
		if (sendThread != null) {
			Log.i(TAG, "sendSpeechText \"" + lang.substring(0, 2) + "\" \"" + text + "\"");
			sendThread.sendCmd(FT_SPEECH + lang.substring(0, 2) + text + " ");
		}
	}

	// socket functions
	private void startConnectThread() {
		mConnecting = false;
		mConnected = false;
		driverDisconnected();

		stopConnectThread = true;
		if (connectThread != null) {
			connectThread.interrupt();
		}
		connectThread = new Thread(new Runnable() {
			@Override
			public void run() {
				Thread.currentThread().setName("ConnectThread" + Thread.currentThread().getId());
				stopConnectThread = false;
				while (!stopConnectThread) {
					mConnecting = true;
					Log.i(TAG, "Starting Runnable");
					android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

					try {
						// Connect the device through the socket. This will block
						// until it succeeds or throws an exception
						InetSocketAddress rpiSocketAdr = new InetSocketAddress(F3FTIMER_SERVER, F3FTIMER_SERVER_PORT);
						Log.i(TAG, "connecting to " + rpiSocketAdr.getHostName() + ":" + rpiSocketAdr.getPort());
						mmSocket = new Socket();
						mmSocket.setReuseAddress(true);
						mmSocket.setTcpNoDelay(true);
						mmSocket.setSoLinger(false, 0);
						mmSocket.setSoTimeout(1000);
						mmSocket.connect(rpiSocketAdr, 5000);
						// Do work to manage the connection (in a separate thread)
						Log.i(TAG, "GET IO STREAMS");
						mmInStream = mmSocket.getInputStream();
						mmOutStream = mmSocket.getOutputStream();

						mDriver.start(mIntent);

						if (listenThread != null) {
							listenThread.interrupt();
						}
						listenThread = new Thread(null, new Runnable() {
							@Override
							public void run() {
								Thread.currentThread().setName("ListenThread" + Thread.currentThread().getId());
								listen();
							}
						});
						listenThread.start();

						if (sendThread != null) {
							sendThread.interrupt();
						}
						sendThread = new SendThread();
						sendThread.start();

						mConnected = true;
						mConnecting = false;
						driverConnected();

						Log.i(TAG, "connected to " + rpiSocketAdr.getHostName() + ":" + rpiSocketAdr.getPort());
						callbackToUI("driver_started");
					} catch (IOException connectException) {
						closeSocketAndStop();
					}
					while (mConnected && !stopConnectThread) {
						try {
							sleep(500);
						} catch (InterruptedException e) {
							if (!stopConnectThread) e.printStackTrace();
						}
					}
				}
			}
		});
		connectThread.start();
	}

	private void listen() {
		// Listen
		byte[] buffer = new byte[1024];  // 1K buffer store for the stream
		int bufferLength; // bytes returned from read()
		String wind_angle_str;
		String wind_speed_str;
		float wind_angle_absolute;
		float wind_angle_relative;
		float wind_speed;
		mWindTimestamp = System.currentTimeMillis();

		//noinspection InfiniteLoopStatement
		while(true) {
			try {
				// Read from the InputStream
				bufferLength = mmInStream.read(buffer);
				if (bufferLength > 0) {
					byte[] data = new byte[bufferLength];
					System.arraycopy(buffer, 0, data, 0, bufferLength);
					String strbuf = new String(data, 0, data.length).replaceAll("[^\\x20-\\x7F]", "").trim();
					String strarray[] = strbuf.split(" ");
					for (String str : strarray) {
						int len = str.length();
						if (len > 0) {
							// Get code (first char)
							String code = str.substring(0, 1);
							// We have data/command from the timer, pass this on to the server
							switch (code) {
								case FT_START:
									Log.d(TAG, "received: \"" + strbuf + "\"");
									if (mState < 2) {
										mDriver.startPressed();
										mState++;
									}
									break;
								case FT_CANCEL:
									Log.d(TAG, "received: \"" + strbuf + "\"");
									mDriver.cancelDialog();
									mReceivedAbort = true;
									mDriver.abort();
									break;
								case FT_CANCEL_ZERO:
									Log.d(TAG, "received: \"" + strbuf + "\"");
									mDriver.scoreZeroAndCancelDialogAndNextPilot();
									mReceivedAbort = true;
									mDriver.abort();
									break;
								case FT_PENALTY:
									Log.d(TAG, "received: \"" + strbuf + "\"");
									if (mLeg >= 1) {
										mDriver.incPenalty();
									}
									break;
								case FT_TIME:
									Log.d(TAG, "received: \"" + strbuf + "\"");
									String flight_time = str.substring(1, str.length());
									Log.i(TAG, "Flight time: " + flight_time);
									timeReceived = true;
									finished(flight_time);
									break;
								case FT_TURNB:
									// after the model has been started only accept turn A as the first signal
									// then only accept A after B after A ...
									mTurnB = true;
								case FT_TURNA:
									Log.d(TAG, "received: \"" + strbuf + "\"");
									if (!mTurnB) mTurnA = true;
									if (mState >= 2) {
										switch (mTimerStatus) {
											case 0:
												if (mTurnA) {
													mDriver.offCourse();
												}
												break;
											case 1:
												if (mTurnA) {
													mDriver.onCourse();
													mLeg = 1;
												}
												break;
											default:
												int turn = mLeg % 2;
												if ((turn == 1 && mTurnB) || (turn == 0 && mTurnA)) {
													mDriver.legComplete();
													mLeg++;
												}
												break;
										}
										if (mTimerStatus <= 1 && mTurnA) {
											mTimerStatus++;
										}
									}
									mTurnA = false;
									mTurnB = false;
									break;
								case FT_WIND:
									if (mDriver.mWindMeasurement) {
										wind_angle_str = str.substring(str.indexOf(",") + 1, str.lastIndexOf(","));
										wind_speed_str = str.substring(str.lastIndexOf(",") + 1, str.length());
										try {
											wind_angle_absolute = (Float.parseFloat(wind_angle_str)) % 360;
											wind_angle_relative = wind_angle_absolute - mSlopeOrientation;
											if (wind_angle_absolute > 180 + mSlopeOrientation) {
												wind_angle_relative -= 360;
											}
											wind_speed = Float.parseFloat(wind_speed_str);
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
												//Log.d("TcpIoService", String.format("Wind illegal (wind angle_absolute=%f, wind angle_relative=%f wind speed=%f wind_speed_counter=%d)", wind_angle_absolute, wind_angle_relative, wind_speed, mWindSpeedCounter));
												mDriver.windIllegal();
												windLegal = false;
											} else {
												//Log.d("TcpIoService", String.format("Wind legal (wind angle_absolute=%f, wind angle_relative=%f wind speed=%f wind_speed_counter=%d)", wind_angle_absolute, wind_angle_relative, wind_speed, mWindSpeedCounter));
												mDriver.windLegal();
												windLegal = true;
											}
											Intent i2 = new Intent("com.marktreble.f3ftimer.onUpdate");
											i2.putExtra("com.marktreble.f3ftimer.value.wind_values", "");
											i2.putExtra("com.marktreble.f3ftimer.value.wind_legal", windLegal);
											i2.putExtra("com.marktreble.f3ftimer.value.wind_angle_absolute", wind_angle_absolute);
											i2.putExtra("com.marktreble.f3ftimer.value.wind_angle_relative", wind_angle_relative);
											i2.putExtra("com.marktreble.f3ftimer.value.wind_speed", wind_speed);
											i2.putExtra("com.marktreble.f3ftimer.value.wind_speed_counter", 20 - mWindSpeedCounterSeconds);
											sendBroadcast(i2);
										} catch (NumberFormatException e) {
											if (!stopConnectThread) e.printStackTrace();
										}
									}
									break;
							} // switch
						}
					} // end for loop
				} else if (bufferLength == -1) {
					closeSocketAndStop();
				}
			} catch (Throwable e) {
				if (1 == handleSocketThrowable(e)) {
					break;
				}
			}
			try {
				sendThread.sendCmd(""); // send alive
			} catch (Throwable e) {
				if (1 == handleSocketThrowable(e)) {
					break;
				}
			}
		}
	}

	private int handleSocketThrowable(Throwable e) {
		if (!(e instanceof SocketTimeoutException)) {
			closeSocketAndStop();
			if (!stopConnectThread) e.printStackTrace();
			return 1;
		} else {
			sendThread.sendCmd(""); // send alive
		}
		return 0;
	}

	private void closeSocketAndStop() {
		mConnected = false;
		mConnecting = false;
		driverDisconnected();
		try {
			mmSocket.close();
		} catch (IOException e1) {
			if (!stopConnectThread) e1.printStackTrace();
		}
		callbackToUI("driver_stopped");
		if (!stopConnectThread) Log.i(TAG, "Not Listening (EXCEPTION)");
	}
}
