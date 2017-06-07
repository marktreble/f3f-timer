package com.marktreble.f3ftimer.driver;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothHC05Service extends Service implements DriverInterface {

	private static final String TAG = "BluetoothHC05Service";

	private Intent mIntent;

	private Handler mHandler;

	private Driver mDriver;
	private String mBuffer = "";
	public int mTimerStatus = 0;
	public boolean mBoardConnected = false;

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


	static final String ENCODING = "US_ASCII";

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

	/*
	 * General life-cycle function overrides
	 */

	@Override
    public void onCreate() {
		super.onCreate();
		mDriver = new Driver(this);

		this.registerReceiver(onBroadcast, new IntentFilter("com.marktreble.f3ftimer.onUpdateFromUI"));
		registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

		mHandler = new Handler();
    }

	@Override
	public void onDestroy() {
		Log.i(TAG, "ONDESTROY!");
		super.onDestroy();        
		mDriver.destroy();

		mIsListening = false;
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
    }

	public static void startDriver(RaceActivity context, String inputSource, Integer race_id, Bundle params){
		if (inputSource.equals(context.getString(R.string.BLUETOOTH_HC_05))){
			Intent serviceIntent = new Intent(context, BluetoothHC05Service.class);
			serviceIntent.putExtras(params);
			serviceIntent.putExtra("com.marktreble.f3ftimer.race_id", race_id);
			context.startService(serviceIntent);
			Log.d(TAG, "BT DRIVER STARTED");
		}
	}

	public static boolean stop(RaceActivity context){
		Log.i(TAG, "STOP");
		if (context.isServiceRunning("com.marktreble.f3ftimer.driver.BluetoothHC05Service")) {
			Log.i(TAG, "RUNNING");
			Intent serviceIntent = new Intent(context, BluetoothHC05Service.class);
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
				Log.i(TAG, data);

				if (data == null) return;

				if (data.equals("get_connection_status")) {
					if (mBoardConnected){
						callbackToUI("driver_started");

					} else {
						callbackToUI("driver_stopped");
					}
				}

				if (data.equals("bluetooth_connected")) {
					getBluetoothDevices();
				}
			}
		}
	};
	@Override
    public int onStartCommand(Intent intent, int flags, int startId){
    	super.onStartCommand(intent, flags, startId);

		Log.d(TAG, "onSTartCommand");
		mIntent = intent;

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			// Device does not support bluetooth
			Log.d(TAG, "NOT SUPPORTED");
			return (START_STICKY);
		}

		if (!mBluetoothAdapter.isEnabled()) {

			// Need to post Message to UI to show enable BT dialog
			Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
			i.putExtra("com.marktreble.f3ftimer.service_callback", "no_bluetooth");
			sendBroadcast(i);

			Log.d(TAG, "NOT ENABLED");
		} else {
			Log.d(TAG, "ENABLED");
			getBluetoothDevices();
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

	// Output - Send commands
	private void sendCmd(String cmd){
		Log.i(TAG, "SENDING: "+cmd);
		byte[] msg;
		boolean sent = false;
		if (mmOutStream != null) {
			try {
				msg = cmd.getBytes(Charset.forName(ENCODING));
				mmOutStream.write(msg);
				sent = true;
			} catch (IOException e) {
				Log.i(TAG, "EX: "+e.getMessage());
				e.printStackTrace();
			}

		}

		if (!sent){
			// Call alert dialog on UI Thread "No Output Stream Available"
			Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
			i.putExtra("com.marktreble.f3ftimer.service_callback", "no_out_stream");
			sendBroadcast(i);
		}

	}

	// Driver Interface implementations
	public void sendLaunch(){
		this.sendCmd(TT_LAUNCH);
		mTimerStatus = 0;
	}
	public void sendAbort(){
		this.sendCmd(TT_ABORT);
	}

	public void sendAdditionalBuzzer(){
		this.sendCmd(TT_ADDITIONAL_BUZZER);
	}

	public void sendResendTime(){
		this.sendCmd(TT_RESEND_TIME);
	}

	public void baseA(){}
	public void baseB(){}
	public void finished(String time){}

	public void getBluetoothDevices(){
		mDiscoveredDeviceNames = new ArrayList<>();
		mDiscoveredDevices = new ArrayList<>();

		mPairedDeviceNames = new ArrayList<>();
		mPairedDevices = new ArrayList<>();

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

		if (!attemptDeviceConnection()){
			mBluetoothAdapter.startDiscovery();
		}

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

				// See if any paired or discovered devices accept the UUID
				if (!attemptDeviceConnection()){
					mBluetoothAdapter.startDiscovery();
				}
			}
		}
	};

	private boolean attemptDeviceConnection() {

		for (int i = 0; i < mPairedAndDiscoveredDevices.size(); i++) {
			mmDevice = mPairedAndDiscoveredDevices.get(i);
			Log.i(TAG, "Attempting connection to device " + mmDevice.getName());

			BluetoothSocket tmp = null;
			// uuid is standard for the HC-05
			UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
			try {
				ParcelUuid uuids[] = mmDevice.getUuids();

				for (ParcelUuid test : uuids){
					Log.i(TAG, "Supported " +test.toString() + " ? " + uuid.toString());
					if (test.equals(new ParcelUuid(uuid))){
						tmp = mmDevice.createRfcommSocketToServiceRecord(uuid);
					}
				}
			} catch (IOException e) {
				Log.i(TAG, "Failed to connect to device " + mmDevice.getName());
			}

			if (tmp != null){
				mmSocket = tmp;
				Log.i(TAG, "connected to " + mmDevice.getName());
				startConnectThread();
				return true;
			}
		}

		return false;
	}

	private void startConnectThread(){

		Thread connectThread = new Thread(new Runnable()
		{
			@Override
			public void run() {
				Log.i(TAG, "Starting Runnable");
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
						closeException.printStackTrace();
					}
					return;
				}

				// Do work to manage the connection (in a separate thread)
				manageConnectedSocket();
			}
		});

		new Handler().post(connectThread);
	}

	private void manageConnectedSocket() {
		Log.i(TAG, "GET IO STREAMS");
		try {
			mmInStream = mmSocket.getInputStream();
			mmOutStream = mmSocket.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}

		mBoardConnected = true;
		mDriver.start(mIntent);

		mHandler.postDelayed(listener, 100);
	}

	final Runnable listener = new Runnable() {
		@Override
		public void run() {
			listen();
		}
	};

	private void listen() {
		// Listen
		byte[] buffer = new byte[1024];  // 1K buffer store for the stream
		int bufferLength; // bytes returned from read()
		mIsListening = true;

		try {
			if (mmInStream.available()>0) {
				// Read from the InputStream
				bufferLength = mmInStream.read(buffer);
				if (bufferLength > 0) {
					byte[] data = new byte[bufferLength];

					StringBuilder builder = new StringBuilder();
					System.arraycopy(buffer, 0, data, 0, bufferLength);
					builder.append(new String(data, "UTF-8"));

					char[] charArray = (new String(data, 0, data.length)).toCharArray();

					StringBuilder sb = new StringBuilder(charArray.length);
					StringBuilder hexString = new StringBuilder();
					for (char c : charArray) {
						if (c < 0) throw new IllegalArgumentException();
						sb.append(Character.toString(c));

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
						Log.i("NEWDATA", str_in);
						String lastchar = hexString.substring(hexString.length() - 2, hexString.length());
						if (lastchar.equals("0d") || lastchar.equals("0a")) {
							// Clear the buffer
							mBuffer = "";

							// Get code (first char)
							String code = "";
							code = str_in.substring(0, 1);

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
								Log.d("BYTES RECEIVED", str_in.length() + "::" + str_in);
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
			}
		} catch (IOException e) {
			Log.i(TAG, "Not Listening (EXCEPTION)");

			e.printStackTrace();
			mIsListening = false;
		}

		if (mIsListening) mHandler.postDelayed(listener, 100);
	}
}
