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

package com.marktreble.f3ftimer.racemanager;

import android.app.ActionBar;
import android.app.ActivityManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.os.ResultReceiver;
import androidx.preference.PreferenceManager;
import android.print.PrintAttributes;
import android.print.PrintManager;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.marktreble.f3ftimer.BaseActivity;
import com.marktreble.f3ftimer.F3FtimerApplication;
import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.constants.IComm;
import com.marktreble.f3ftimer.constants.Pref;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.pilot.PilotData;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;
import com.marktreble.f3ftimer.dialog.AboutActivity;
import com.marktreble.f3ftimer.dialog.FlyingOrderEditActivity;
import com.marktreble.f3ftimer.dialog.GenericAlert;
import com.marktreble.f3ftimer.dialog.GenericCheckboxPicker;
import com.marktreble.f3ftimer.dialog.GroupScoreEditActivity;
import com.marktreble.f3ftimer.dialog.HelpActivity;
import com.marktreble.f3ftimer.dialog.NextRoundActivity;
import com.marktreble.f3ftimer.dialog.PilotsEditActivity;
import com.marktreble.f3ftimer.dialog.RaceRoundTimeoutActivity;
import com.marktreble.f3ftimer.dialog.RaceTimerActivity;
import com.marktreble.f3ftimer.dialog.SettingsActivity;
import com.marktreble.f3ftimer.dialog.StartNumberEditActivity;
import com.marktreble.f3ftimer.dialog.TimeEntryActivity;
import com.marktreble.f3ftimer.driver.BluetoothHC05Service;
import com.marktreble.f3ftimer.driver.SoftBuzzerService;
import com.marktreble.f3ftimer.driver.TcpIoService;
import com.marktreble.f3ftimer.driver.UDPService;
import com.marktreble.f3ftimer.driver.USBIOIOService;
import com.marktreble.f3ftimer.driver.USBOpenAccessoryService;
import com.marktreble.f3ftimer.driver.USBOtherService;
import com.marktreble.f3ftimer.filesystem.F3FGearExport;
import com.marktreble.f3ftimer.filesystem.SpreadsheetExport;
import com.marktreble.f3ftimer.pilotmanager.PilotsActivity;
import com.marktreble.f3ftimer.printing.PilotListDocumentAdapter;
import com.marktreble.f3ftimer.resultsmanager.ResultsActivity;
import com.marktreble.f3ftimer.usb.USB;
import com.marktreble.f3ftimer.wifi.Wifi;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class RaceActivity extends BaseActivity
        implements ListView.OnClickListener {

    public static int RESULT_ABORTED = 4; // Changed from 2 to 4 because of conflict with dialog dismissal
    public static int ROUND_SCRUBBED = 3;
    public static int ENABLE_BLUETOOTH = 5;
    public static int RACE_FINISHED = 6;

    static final String DIALOG = "dialog";

    // Dialogs
    static int DLG_SETTINGS = 9;
    static int DLG_TIMER = 1;
    static int DLG_NEXT_ROUND = 2;
    static int DLG_TIMEOUT = 3;
    static int DLG_TIME_SET = 4;
    static int DLG_PILOT_EDIT = 5;
    static int DLG_FLYING_ORDER_EDIT = 6;
    static int DLG_GROUP_SCORE_EDIT = 7;
    static int DLG_START_NUMBER_EDIT = 8;

    private ArrayAdapter<String> mArrAdapter;
    private ArrayList<String> mArrNames;
    private ArrayList<String> mArrNumbers;
    private ArrayList<Pilot> mArrPilots;
    private ArrayList<Integer> mArrRounds;
    private ArrayList<Integer> mArrGroups;
    private ArrayList<Boolean> mFirstInGroup;

    private ArrayList<String> mArrRemainingNames;
    private ArrayList<Pilot> mArrRemainingPilots;

    private Integer mRid;
    private Integer mRnd;
    private Race mRace;
    private String mInputSource = "";
    private String mInputSourceDevice = "";
    private boolean mPrefResults;
    private boolean mPrefWifiHotspot;
    private boolean mPrefResultsDisplay;
    private String mPrefExternalDisplay;
    private boolean mPrefWindMeasurement;
    private boolean mPrefResultsF3Fgear;

    private int mRx;
    private int mTx;
    private int mStart;

    private boolean mRoundComplete;
    private boolean mRoundNotStarted;
    private boolean mGroupNotStarted;
    private Pilot mNextPilot;

    private boolean mPilotDialogShown = false; // Used to determine the action when the start button is pressed
    private boolean mTimeoutDialogShown = false;
    private boolean mTimeoutCompleteDialogShown = false;
    private boolean mMenuShown = false;

    GenericAlert mDLG;
    GenericCheckboxPicker mDLG4;

    String[] _options;    // String array of all pilots in database
    boolean[] _selections;    // bool array of which has been selected

    boolean mWifiSavedState = false;

    private TextView mPower;
    private ImageView mStatus;
    private String mStatusIcon;
    private boolean mConnectionStatus;
    private TextView mWindReadings;

    private String mExternalDisplayStatusIcon;
    private ImageView mExternalDisplayStatus;
    private boolean mDisplayStatus;

    private String mBatteryLevel;

    private ListView mListView;
    private static Parcelable mListViewScrollPos = null;

    private RaceData.Group mGroupScoring;

    private String mCachedIP = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mPageTitle = getString(R.string.app_race);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.race);

        Intent intent = getIntent();
        if (intent.hasExtra("race_id")) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                mRid = extras.getInt("race_id");
            }
        }

        RaceData datasource = new RaceData(this);
        datasource.open();
        Race race = datasource.getRace(mRid);
        mGroupScoring = datasource.getGroup(race.id, race.round);
        datasource.close();
        mRace = race;

        mListView = findViewById(android.R.id.list);
        registerForContextMenu(mListView);

        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

        mExternalDisplayStatus = findViewById(R.id.external_display_connection_status);
        mExternalDisplayStatusIcon = "off_display";

        mPower = findViewById(R.id.battery_level);

        mStatus = findViewById(R.id.connection_status);
        mStatusIcon = "off";

        mWindReadings = findViewById(R.id.wind_readings);


        // Register for notifications
        registerReceiver(onBroadcast, new IntentFilter(IComm.RCV_UPDATE));
        registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        getPreferences();
        setRound();

        if (savedInstanceState == null) {
            // Start Results server
            startServers();

            final Handler handler = new Handler();
            final int delay = 5000; //milliseconds

            handler.postDelayed(new Runnable() {
                public void run() {
                    sendCommand("get_connection_status");
                    handler.postDelayed(this, delay);
                }
            }, delay);

        } else {
            mArrNames = savedInstanceState.getStringArrayList("mArrNames");
            mArrNumbers = savedInstanceState.getStringArrayList("mArrNumbers");
            mArrPilots = new ArrayList<>();
            mArrGroups = savedInstanceState.getIntegerArrayList("mArrGroups");
            mArrRounds = savedInstanceState.getIntegerArrayList("mArrRounds");
            mFirstInGroup = new ArrayList<>();
        }

        // Render the list
        getNamesArray();
        setList();

        mListView.setAdapter(mArrAdapter);

        if (getActionBar() == null) return;
        getActionBar().addOnMenuVisibilityListener(new ActionBar.OnMenuVisibilityListener() {
            @Override
            public void onMenuVisibilityChanged(boolean isVisible) {
                mMenuShown = isVisible;
            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(onBroadcast);
        unregisterReceiver(mBatInfoReceiver);

        if (isFinishing()) {
            stopServers();
        }
    }


    public void onBackPressed() {
        // destroy the servers when back is pressed
        stopServers();
        super.onBackPressed();
    }

    public void startServers() {
        // Start Results server
        if (mPrefResults) {
            if (mPrefWifiHotspot) {
                if (Wifi.canEnableWifiHotspot(this)) {
                    mWifiSavedState = Wifi.enableWifiHotspot(this);
                    Log.i("WIFI", "Enabled - saved state " + ((mWifiSavedState) ? "On" : "Off"));
                }
            }
            RaceResultsService.stop(this);

            Intent serviceIntent = new Intent(this, RaceResultsService.class);
            serviceIntent.putExtra("com.marktreble.f3ftimer.race_id", mRid);
            startService(serviceIntent);
        }

        // Start Results Display Server
        if (mPrefResultsDisplay) {
            if (mPrefExternalDisplay == null || mPrefExternalDisplay.equals("")) {
                String[] buttons_array = new String[1];
                buttons_array[0] = getString(android.R.string.cancel);

                mDLG = GenericAlert.newInstance(
                        getString(R.string.err_external_display),
                        getString(R.string.err_external_display_no_device),
                        buttons_array,
                        null
                );

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.addToBackStack(null);
                ft.add(mDLG, DIALOG);
                ft.commit();
            } else {
                RaceResultsDisplayService.stop(this);

                RaceResultsDisplayService.startRDService(this, mPrefExternalDisplay);
            }
        }

        // Stop Any Timer Drivers
        USBIOIOService.stop(this);
        USBOpenAccessoryService.stop(this);
        USBOtherService.stop(this);
        SoftBuzzerService.stop(this);
        BluetoothHC05Service.stop(this);
        TcpIoService.stop(this);

        // Start Timer Driver
        Bundle extras = getIntent().getExtras();
        if (extras == null)
            extras = new Bundle();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Map<String, ?> keys = sharedPref.getAll();

        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            if (entry.getValue() instanceof String)
                extras.putString(entry.getKey(), (String) entry.getValue());

            if (entry.getValue() instanceof Boolean)
                extras.putBoolean(entry.getKey(), (Boolean) entry.getValue());

        }

        boolean pref_usb_tethering = sharedPref.getBoolean(Pref.USB_TETHER, Pref.USB_TETHER_DEFAULT);
        if (pref_usb_tethering) {
            if (!USB.setupUsbTethering(getApplicationContext())) {
                // Enable tethering
                Intent tetherSettings = new Intent();
                tetherSettings.setClassName("com.android.settings", "com.android.settings.TetherSettings");
                tetherSettings.setFlags(FLAG_ACTIVITY_NEW_TASK);
                startActivity(tetherSettings);
            }
        }

        startServers(extras);

    }

    public void startServers(Bundle e) {
        USBIOIOService.startDriver(this, mInputSource, mRid, e);
        USBOpenAccessoryService.startDriver(this, mInputSource, mRid, e);
        USBOtherService.startDriver(this, mInputSource, mRid, e);
        SoftBuzzerService.startDriver(this, mInputSource, mRid, e);
        BluetoothHC05Service.startDriver(this, mInputSource, mRid, e);
        TcpIoService.startDriver(this, mInputSource, mRid, e);
        UDPService.startDriver(this, mInputSource, mRid, e);
    }

    public void stopServers() {
        // Stop all Servers

        if (RaceResultsService.stop(this)) {
            if (Wifi.canEnableWifiHotspot(this)) {
                Wifi.disableWifiHotspot(this, mWifiSavedState);
            }
        }

        RaceResultsDisplayService.stop(this);

        USBIOIOService.stop(this);
        USBOpenAccessoryService.stop(this);
        USBOtherService.stop(this);
        SoftBuzzerService.stop(this);
        BluetoothHC05Service.stop(this);
        TcpIoService.stop(this);
        UDPService.stop(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("round", mRnd);
        outState.putInt("raceid", mRid);
        outState.putSerializable("mArrRounds", mArrRounds);
        outState.putBoolean("connection_status", mConnectionStatus);
        outState.putString("connection_status_icon", mStatusIcon);
        outState.putBoolean("display_status", mDisplayStatus);
        outState.putString("display_status_icon", mExternalDisplayStatusIcon);
        outState.putString("battery_level", mBatteryLevel);

        outState.putString(Pref.INPUT_SRC, mInputSource);
        outState.putString(Pref.INPUT_SRC_DEVICE, mInputSourceDevice);
        outState.putBoolean("pref_results", mPrefResults);
        outState.putBoolean("pref_wifi_hotspot", mPrefWifiHotspot);
        outState.putBoolean("pref_results_display", mPrefResultsDisplay);
        outState.putString("pref_external_display", mPrefExternalDisplay);
        TextView results = findViewById(R.id.results_ip);
        outState.putString("cache_ip_addr", results.getText().toString());


        mListViewScrollPos = mListView.onSaveInstanceState();
        outState.putParcelable("listviewscrollpos", mListViewScrollPos);

        outState.putStringArrayList("mArrNames", mArrNames);
        outState.putStringArrayList("mArrNumbers", mArrNumbers);
        outState.putStringArrayList("mArrRemainingNames", mArrRemainingNames);
        outState.putIntegerArrayList("mArrGroups", mArrGroups);
        outState.putIntegerArrayList("mArrRounds", mArrRounds);

    }

    @SuppressWarnings("unchecked")
    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        mRnd = savedInstanceState.getInt("round");
        mRid = savedInstanceState.getInt("raceid");
        mArrRounds = (ArrayList<Integer>) savedInstanceState.getSerializable("mArrRounds");
        mConnectionStatus = savedInstanceState.getBoolean("connection_status");
        mStatusIcon = savedInstanceState.getString("connection_status_icon");
        mDisplayStatus = savedInstanceState.getBoolean("display_status");
        mExternalDisplayStatusIcon = savedInstanceState.getString("display_status_icon");
        mBatteryLevel = savedInstanceState.getString("battery_level");

        mInputSource = savedInstanceState.getString(Pref.INPUT_SRC);
        mInputSourceDevice = savedInstanceState.getString(Pref.INPUT_SRC_DEVICE);
        mPrefResults = savedInstanceState.getBoolean("pref_results");
        mPrefWifiHotspot = savedInstanceState.getBoolean("pref_wifi_hotspot");
        mPrefResultsDisplay = savedInstanceState.getBoolean("pref_results_display");
        mPrefExternalDisplay = savedInstanceState.getString("pref_external_display");

        mListViewScrollPos = savedInstanceState.getParcelable("listviewscrollpos");
        if (mListView != null)
            mListView.onRestoreInstanceState(mListViewScrollPos);

        mCachedIP = savedInstanceState.getString("cache_ip_addr", "");


    }

    public void onResume() {

        super.onResume();

        String sInputSource = mInputSource;
        String sInputSourceDevice = mInputSourceDevice;
        boolean sPrefResults = mPrefResults;
        boolean sPrefWifiHotspot = mPrefWifiHotspot;
        boolean sPrefResultsDisplay = mPrefResultsDisplay;
        String sPrefExternalDisplay = mPrefExternalDisplay;
        Integer sIOIORx = mRx;
        Integer sIOIOTx = mTx;
        Integer sIOIOStart = mStart;

        getPreferences();

        if (!sInputSource.equals(mInputSource)                          // Input src changed
                || sPrefResults != mPrefResults                         // Results server toggled
                || sPrefWifiHotspot != mPrefWifiHotspot                 // wifi hotspot toggled
                || sPrefResultsDisplay != mPrefResultsDisplay           // External Display server toggled
                || !sInputSourceDevice.equals(mInputSourceDevice)       // Input Source device changed
                || !sPrefExternalDisplay.equals(mPrefExternalDisplay)   // External Display device changed
                || !sIOIORx.equals(mRx)
                || !sIOIOTx.equals(mTx)
                || !sIOIOStart.equals(mStart)
        ) {
            stopServers();
            startServers();
        }

        int id = getResources().getIdentifier(mExternalDisplayStatusIcon, "mipmap", getPackageName());
        if (id == 0) {
            id = getResources().getIdentifier(mExternalDisplayStatusIcon, "drawable", getPackageName());
        }

        if (id > 0) {
            mExternalDisplayStatus.setImageDrawable(ContextCompat.getDrawable(mContext, id));
        }

        if (mPrefResultsDisplay) {
            mExternalDisplayStatus.setVisibility(View.VISIBLE);
        } else {
            mExternalDisplayStatus.setVisibility(View.GONE);
        }

        if (sInputSource.equals(mInputSource)) {
            id = getResources().getIdentifier(mStatusIcon, "mipmap", getPackageName());
            if (id == 0) {
                id = getResources().getIdentifier(mStatusIcon, "drawable", getPackageName());
            }

            if (id > 0) {
                mStatus.setImageDrawable(ContextCompat.getDrawable(mContext, id));
            }
        }

        mPower.setText(mBatteryLevel);

        if (mPrefWindMeasurement) {
            mWindReadings.setVisibility(View.VISIBLE);
        } else {
            mWindReadings.setVisibility(View.GONE);
        }

        setResultsIP();

    }

    public void onPause() {
        super.onPause();
    }

    private void getPreferences() {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mInputSource = sharedPref.getString(Pref.INPUT_SRC, getString(R.string.Demo));
        mInputSourceDevice = sharedPref.getString(Pref.INPUT_SRC_DEVICE, "");
        mPrefResults = sharedPref.getBoolean("pref_results_server", false);
        mPrefWifiHotspot = sharedPref.getBoolean("pref_wifi_hotspot", false);
        mPrefResultsDisplay = sharedPref.getBoolean("pref_results_display", false);
        mPrefExternalDisplay = sharedPref.getString("pref_external_display", "");
        mPrefWindMeasurement = sharedPref.getBoolean("pref_wind_measurement", false);
        mPrefResultsF3Fgear = sharedPref.getBoolean("pref_results_F3Fgear", false);

        mRx = Integer.parseInt(sharedPref.getString(Pref.IOIO_RX_PIN, Pref.IOIO_RX_PIN_DEFAULT));
        mTx = Integer.parseInt(sharedPref.getString(Pref.IOIO_TX_PIN, Pref.IOIO_TX_PIN_DEFAULT));
        mStart = Integer.parseInt(sharedPref.getString(Pref.IOIO_START_PIN, Pref.IOIO_START_PIN_DEFAULT));

    }

    private void setRound() {
        mRnd = mRace.round;

        RaceData datasource2 = new RaceData(RaceActivity.this);
        datasource2.open();
        mGroupScoring = datasource2.getGroup(mRid, mRnd);
        datasource2.close();

        TextView tt = findViewById(R.id.race_title);

        String title = String.format(getString(R.string.ttl_round_number_name), mRnd, mRace.name);
        tt.setText(title);

    }

    private void setResultsIP() {
        TextView results = findViewById(R.id.results_ip);
        if (mPrefResults) {
            results.setVisibility(View.VISIBLE);

            if (mCachedIP.equals("")) {
                new fetchIPAsyncTask(results).execute();
            } else {
                results.setText(mCachedIP);
            }
        } else {
            results.setVisibility(View.GONE);
        }

    }

    private static class fetchIPAsyncTask extends AsyncTask<Void, Void, String> {

        WeakReference<TextView> mViewWR;

        fetchIPAsyncTask(TextView view) {
            mViewWR = new WeakReference<>(view);
        }

        @Override
        protected String doInBackground(Void... params) {
            String ip = "";
            while (ip.equals("")) {
                ip = Wifi.getIPAddress(true);
                if (ip.equals("")) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
            return ip;
        }

        @Override
        protected void onPostExecute(String ip) {
            // update the UI (this is executed on UI thread)
            super.onPostExecute(ip);

            TextView view = mViewWR.get();
            view.setText(String.format("%s:8080", ip));
        }
    }

    /*
     * Start a pilot on his run
     */
    @Override
    public void onClick(View v) {
        // Pilot has been clicked, so start the Race Timer Activity
        int position = (int)v.getTag();

        Pilot p = mArrPilots.get(position);
        int round = mArrRounds.get(position);
        if (p.time == 0 && !p.flown && p.status != Pilot.STATUS_RETIRED) {
            String bib_no = mArrNumbers.get(position);
            mPilotDialogShown = showPilotDialog(round, p.id, bib_no);
        }
    }

    /*
     * Return from dialogs
     */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mPilotDialogShown = false;

        getNamesArray();
        mArrAdapter.notifyDataSetChanged();

        // Clear the shown flag regardless of success
        if (requestCode == RaceActivity.DLG_TIMER)
            mPilotDialogShown = false;

        if (requestCode == RaceActivity.DLG_TIMEOUT)
            mTimeoutDialogShown = false;

        if (requestCode == RaceActivity.DLG_TIMEOUT)
            mTimeoutCompleteDialogShown = false;


        if (resultCode == RaceActivity.RESULT_OK) {
            if (requestCode == RaceActivity.ENABLE_BLUETOOTH) {
                // Post back to service that BT has been enabled
                sendCommand("bluetooth_enabled");
            }

            if (requestCode == RaceActivity.DLG_NEXT_ROUND) {
                // Positive response from next round dialog
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        nextRound();
                    }
                }, 600);
            }
            if (requestCode == RaceActivity.DLG_TIMER) {
                // Response from completed run

                if (mNextPilot != null) {
                    // Bring up next pilot's dialog
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mListView.smoothScrollToPositionFromTop(mNextPilot.position - 1, 0, 500);
                        }
                    }, 100);

                    if (!mFirstInGroup.get(mNextPilot.position)) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                showNextPilot();
                            }
                        }, 600);
                    }

                } else {
                    // Bring up the next round dialog
                    showNextRound();
                }
            }
            if (requestCode == RaceActivity.DLG_TIME_SET) {
                // Response from setting the time manually
                String time = data.getStringExtra("time");
                int pilot_id = data.getIntExtra("pilot", 0);
                int round = data.getIntExtra("round", 0);

                if (!time.equals("")) {
                    float new_time = Float.parseFloat(time);
                    RacePilotData datasource = new RacePilotData(RaceActivity.this);
                    datasource.open();
                    datasource.setPilotTimeInRound(mRid, pilot_id, round, new_time);
                    datasource.close();

                }

            }
            if (requestCode == RaceActivity.DLG_TIMEOUT) {
                // Send command to Service to say that timeout has been resumed
                sendCommand("timeout_resumed");

                // Resume timeout - start next pilot's dialog
                if (mNextPilot != null) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mListView.smoothScrollToPositionFromTop(mNextPilot.position - 1, 0, 500);
                        }
                    }, 100);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showNextPilot();
                        }
                    }, 600);
                }
            }

            if (requestCode == RaceActivity.DLG_GROUP_SCORE_EDIT) {
                String num_groups = data.getStringExtra("num_groups");

                int num;
                try {
                    num = Integer.parseInt(num_groups);
                } catch (NumberFormatException e) {
                    num = 0;
                }

                RacePilotData datasource = new RacePilotData(RaceActivity.this);
                datasource.open();
                ArrayList<Pilot> allPilots = datasource.getAllPilotsForRace(mRid, mRnd, mRace.offset, mRace.start_number);
                datasource.close();

                RaceData datasource2 = new RaceData(RaceActivity.this);
                datasource2.open();
                mGroupScoring = datasource2.setGroups(mRid, mRnd, num, getStartPilot(allPilots, mRace));
                datasource2.close();
                setRound();

            }

            if (requestCode == RaceActivity.DLG_START_NUMBER_EDIT) {
                String start_number = data.getStringExtra("start_number");

                int num;
                try {
                    num = Math.max(1, Integer.parseInt(start_number));
                } catch (NumberFormatException e) {
                    num = 1;
                }

                RaceData datasource = new RaceData(RaceActivity.this);
                datasource.open();
                datasource.setStartNumber(mRid, num);
                mRace = datasource.getRace(mRid); // Update the ram model
                mGroupScoring = datasource.setGroups(mRid, mRnd, mGroupScoring.num_groups, num);
                datasource.close();
                setRound();
            }

            getNamesArray();
            mArrAdapter.notifyDataSetChanged();
        }

        if (resultCode == RaceActivity.RACE_FINISHED) {
            if (requestCode == RaceActivity.DLG_NEXT_ROUND) {
                finishRace();
            }
        }

        if (resultCode == RaceActivity.ROUND_SCRUBBED) {
            if (requestCode == RaceActivity.DLG_TIMEOUT) {
                scrubRound();
            }
        }

        if (requestCode == RaceListActivity.DLG_SETTINGS) {
            ((F3FtimerApplication) getApplication()).restartApp();
        }

        invalidateOptionsMenu(); // Refresh menu so that any changes in state are shown
    }

    /*
     * Create the context menu, and handle the result
     */

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (v.getId() == android.R.id.list) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle(mArrNames.get(info.position));

            Pilot p = mArrPilots.get(info.position);

            if ((p.status & Pilot.STATUS_NORMAL) == Pilot.STATUS_NORMAL && p.flown)
                menu.add(Menu.NONE, 0, 0, getString(R.string.cmenu_reflight));

            if ((p.status & Pilot.STATUS_NORMAL) == Pilot.STATUS_NORMAL && p.flown) {
                menu.add(Menu.NONE, 1, 1, getString(R.string.cmenu_add_penalty));
                menu.add(Menu.NONE, 2, 2, getString(R.string.cmenu_remove_penalty));
            }
            if ((p.status & Pilot.STATUS_RETIRED) == 0 && !p.flown)
                menu.add(Menu.NONE, 3, 3, getString(R.string.cmenu_skip_round));

            if ((p.status & Pilot.STATUS_RETIRED) == 0)
                menu.add(Menu.NONE, 4, 4, getString(R.string.cmenu_retire));

            if ((p.status & Pilot.STATUS_RETIRED) > 0)
                menu.add(Menu.NONE, 5, 5, getString(R.string.cmenu_reinstate));

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            boolean allowManualEntry = sharedPref.getBoolean("pref_manual_entry", true);
            if (allowManualEntry)
                menu.add(Menu.NONE, 6, 6, getString(R.string.cmenu_manual_entry));

            menu.add(Menu.NONE, 7, 7, getString(R.string.cmenu_edit_pilot));

        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int menuItemIndex = item.getItemId();

        Pilot p = mArrPilots.get(info.position);
        Integer round = mArrRounds.get(info.position);

        RacePilotData datasource = new RacePilotData(RaceActivity.this);
        datasource.open();

        if (menuItemIndex == 0) {
            // Award Reflight
            datasource.setReflightInRound(mRid, p.id, round);
        }

        if (menuItemIndex == 1) {
            // Penalty
            datasource.incPenalty(mRid, p.id, round);
        }

        if (menuItemIndex == 2) {
            // Penalty (remove)
            datasource.decPenalty(mRid, p.id, round);
        }

        if (menuItemIndex == 3) {
            // Zero
            scorePilotZero(p);
        }

        if (menuItemIndex == 4) {
            // Retired
            datasource.setRetired(true, mRid, p.id);
        }

        if (menuItemIndex == 5) {
            // Retired
            datasource.setRetired(false, mRid, p.id);
        }

        if (menuItemIndex == 6) {
            // Manual time entry dialog
            enterManualTimeForPilot(p, round);
        }

        if (menuItemIndex == 7) {
            // Pilot edit dialog
            editPilot(p);
        }

        datasource.close();

        getNamesArray();
        mArrAdapter.notifyDataSetChanged();
        return true;
    }

    public void scorePilotZero(Pilot p) {
        RacePilotData datasource = new RacePilotData(RaceActivity.this);
        datasource.open();
        datasource.setPilotTimeInRound(mRid, p.id, mRnd, 0);
        datasource.close();

        getNamesArray();
        mArrAdapter.notifyDataSetChanged();
        invalidateOptionsMenu();
    }

    private int getStartPilot(ArrayList<Pilot> allPilots, Race race) {

        int start = 1;
        int numPilots = allPilots.size();
        if (race.start_number > 0) {
            start = race.start_number;
        } else {
            if (numPilots > 0)
                start = ((race.round - 1) * race.offset) % numPilots;
        }
        return start;
    }
    /*
     * Get Pilots from database to populate the listview
     */

    private void getNamesArray() {

        RacePilotData datasource = new RacePilotData(this);
        datasource.open();
        ArrayList<ArrayList<Pilot>> allPilots = new ArrayList<>();
        for (int r = mRnd; r < mRnd + mRace.rounds_per_flight; r++)
            allPilots.add(datasource.getAllPilotsForRace(mRid, r, mRace.offset, mRace.start_number));

        // Initialise Arrays if needed
        if (mArrNames == null) {
            mArrNames = new ArrayList<>();
            mArrNumbers = new ArrayList<>();
            mArrPilots = new ArrayList<>();
            mArrRounds = new ArrayList<>();
            mArrGroups = new ArrayList<>();
            mFirstInGroup = new ArrayList<>();
        }

        // Calculate correct sz and increase if needed
        int sz = allPilots.get(0).size() * mRace.rounds_per_flight;

        while (mArrNames.size() < sz) mArrNames.add("");
        while (mArrNumbers.size() < sz) mArrNumbers.add("");
        while (mArrPilots.size() < sz) mArrPilots.add(new Pilot());
        while (mArrRounds.size() < sz) mArrRounds.add(0);
        while (mArrGroups.size() < sz) mArrGroups.add(0);
        while (mFirstInGroup.size() < sz) mFirstInGroup.add(false);

        mRoundComplete = true;
        mRoundNotStarted = true;
        mGroupNotStarted = true;
        mNextPilot = null;
        Pilot mNextReflightPilot = null;

        // No. of bib numbers skipped
        // (used to close up the gaps in the list, and pop the unused ends off the arrays at the end)
        int skipped = 0;

        // Get the start number
        int start = mGroupScoring.start_pilot;
        int numPilots = allPilots.get(0).size();

        // Find actual number of pilots
        int num_pilots = 0;
        for (int i = 0; i < allPilots.get(0).size(); i++) {
            Pilot p = allPilots.get(0).get(i);
            if (p.pilot_id > 0) {
                num_pilots++;
            }
        }
        num_pilots *= mRace.rounds_per_flight;


        // Loop through rounds per_flight
        // Calculations are done round by round when multiple rounds are flown per flight.
        for (int r = 0; r < mRace.rounds_per_flight; r++) {
            int c = 0; // Flying order number
            int apn = 0; // Actual Pilot number (Used for calculating groups);

            int g = 0; // Current group we are calculating

            float[] ftg = new float[mGroupScoring.num_groups + 1]; // Fastest time in group (used for calculating normalised scores)
            for (int i = 0; i < mGroupScoring.num_groups + 1; i++)
                ftg[i] = 9999;  // initialise ftg for all groups to a stupidly large number

            // First to fly
            // (only when r = 0!)
            boolean first = (r == 0);
            int group_size = (int) Math.floor(num_pilots / (float)mGroupScoring.num_groups);
            int remainder = num_pilots - (mGroupScoring.num_groups * group_size);

            skipped = 0; // Tally of missing bib numbers

            for (Pilot p : allPilots.get(r)) {
                // Increment group number
                if (g < remainder) {
                    // The remainder is divided amongst the first groups giving 1 extra pilot until exhausted
                    if (apn >= (group_size + 1) * (g + 1)) {
                        g++;
                        first = (r == 0);
                    }
                } else {
                    // Any remainder is now exhausted
                    if (apn >= ((group_size + 1) * remainder) + (group_size * ((g + 1) - remainder))) {
                        g++;
                        first = (r == 0);
                    }
                }

                // Calculate the position in the array
                int position = (c * mRace.rounds_per_flight) + r - skipped;

                // Set the pilot's name
                mArrNames.set(position, String.format("%s %s", p.firstname, p.lastname));

                // (c+start+1)%numPilots is the bib number
                int bib_number = ((c + (start - 1)) % numPilots) + 1;
                if (r == 0) {
                    mArrNumbers.set(position, String.format("%d", bib_number));
                } else {
                    // Only show the bib number for the first entry in multiple rounds per flight mode
                    mArrNumbers.set(position, "");
                }
                mArrPilots.set(position, p);
                mArrRounds.set(position, mRnd + r);
                mArrGroups.set(position, g);
                mFirstInGroup.set(position, first);
                c++;

                if (p.pilot_id == 0) {
                    // Skipped bib number
                    skipped += mRace.rounds_per_flight;
                } else {

                    if (p.time == 0 && (((p.status & Pilot.STATUS_RETIRED) == 0) && (!p.flown))) {
                        // Unset round complete flag
                        // Somebody who isn't retired hasn't flown
                        mRoundComplete = false;
                    } else {
                        // Unset round not started flag
                        // Somebody has flown
                        if ((p.status & Pilot.STATUS_RETIRED) != Pilot.STATUS_RETIRED) {


                            mRoundNotStarted = false;
                            mGroupNotStarted = false;
                        }
                    }

                    // Get the next pilot in the running order
                    if (p.time == 0 && !p.flown &&
                            (((p.status & Pilot.STATUS_NORMAL) == Pilot.STATUS_NORMAL) /*|| ((p.status & Pilot.STATUS_REFLIGHT) == Pilot.STATUS_REFLIGHT)*/)
                            && (mNextPilot == null || mNextPilot.position > position)) {
                        mNextPilot = p;
                        mNextPilot.position = position;
                        mNextPilot.number = mArrNumbers.get(position);
                        // Log the round number against the next pilot, so that the data is available to the external "start_pressed" message
                        mNextPilot.round = mRnd + r;
                    }

                    // Get the next reflight pilot in the running order
                    if (p.time == 0 && !p.flown &&
                            ((p.status & Pilot.STATUS_REFLIGHT) == Pilot.STATUS_REFLIGHT)
                            && (mNextReflightPilot == null || mNextReflightPilot.position > position)) {
                        mNextReflightPilot = p;
                        mNextReflightPilot.position = position;
                        mNextReflightPilot.number = mArrNumbers.get(position);
                        // Log the round number against the next pilot, so that the data is available to the external "start_pressed" message
                        mNextReflightPilot.round = mRnd + r;
                    }

                    ftg[g] = (p.time > 0) ? Math.min(ftg[g], p.time) : ftg[g];
                    apn++;
                    first = false; // Only reset the first flag when we actually have a pilot

                }

            }

            // Set points for each pilot
            for (int i = 0; i < mArrPilots.size(); i += mRace.rounds_per_flight) {
                Pilot p = mArrPilots.get(i + r);
                if (p.time > 0)
                    p.points = (int) ((ftg[mArrGroups.get(i + r)] / p.time) * 1000);

                if (p.time == 0 && p.flown) // Avoid division by 0
                    p.points = 0;

                p.points -= p.penalty * 100;

                if (p.time == 0 && p.status == Pilot.STATUS_RETIRED) // Avoid division by 0
                    p.points = 0;
            }
        }

        // Remove skipped values from the end of all the arrays
        for (int i = 0; i < skipped; i++) {
            int index = mArrNames.size() - 1;
            mArrNames.remove(index);
            mArrNumbers.remove(index);
            mArrPilots.remove(index);
            mArrRounds.remove(index);
            mArrGroups.remove(index);
            mFirstInGroup.remove(index);
        }

        if (mNextPilot == null) mNextPilot = mNextReflightPilot;

        datasource.close();

    }

    private void setList() {

        final int background = F3FtimerApplication.themeAttributeToColor(
                R.attr.bg,
                this,
                R.color.black);

        final int normal = F3FtimerApplication.themeAttributeToColor(
                R.attr.t2,
                this,
                R.color.text3);

        mArrAdapter = new ArrayAdapter<String>(this, R.layout.listrow_racepilots, R.id.text1, mArrNames) {
            @Override
            public @NonNull
            View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row;

                //if (mArrNames.get(position) == null) return null;
                if (0 > position || position >= mArrNames.size() || mArrNames.get(position) == null)
                    return convertView;

                if (null == convertView) {
                    row = getLayoutInflater().inflate(R.layout.listrow_racepilots, parent, false);
                    row.setOnClickListener(RaceActivity.this);
                    row.setOnCreateContextMenuListener(RaceActivity.this);
                } else {
                    row = convertView;
                }

                Pilot p = mArrPilots.get(position);

                TextView p_number = row.findViewById(R.id.number);
                String bib_no = mArrNumbers.get(position);
                if (!bib_no.equals("")) {
                    p_number.setVisibility(View.VISIBLE);
                    p_number.setText(bib_no);
                } else {
                    p_number.setVisibility(View.INVISIBLE);
                }

                TextView p_name = row.findViewById(R.id.text1);
                p_name.setText(mArrNames.get(position));
                p_name.setPaintFlags(p_name.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                p_name.setTextColor(normal);

                View group_header = row.findViewById(R.id.group_header);
                TextView group_header_label = row.findViewById(R.id.group_header_label);
                if (mGroupScoring.num_groups > 1 && mFirstInGroup.get(position)) {
                    group_header.setVisibility(View.VISIBLE);
                    group_header_label.setText(String.format(getString(R.string.group_heading), mArrGroups.get(position) + 1));
                } else {
                    group_header.setVisibility(View.GONE);
                }

                Drawable flag = p.getFlag(mContext);
                if (flag != null) {
                    p_name.setCompoundDrawablesWithIntrinsicBounds(flag, null, null, null);
                    int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
                    p_name.setCompoundDrawablePadding(padding);
                }

                row.setBackgroundColor(background);

                if (p.status == Pilot.STATUS_RETIRED) {
                    p_name.setPaintFlags(p_name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    p_name.setTextColor(ContextCompat.getColor(mContext, R.color.red));
                }

                if (p.status == Pilot.STATUS_REFLIGHT) {
                    p_name.setTextColor(ContextCompat.getColor(mContext, R.color.green));
                    row.setBackgroundColor(ContextCompat.getColor(mContext, R.color.med_grey));
                }

                TextView penalty = row.findViewById(R.id.penalty);
                if (p.penalty > 0) {
                    penalty.setText(String.format(getString(R.string.penalty), p.penalty));
                } else {
                    penalty.setText(getResources().getString(R.string.empty));
                }

                TextView time = row.findViewById(R.id.time);
                if (p.time == 0 && !p.flown) {
                    time.setText(getResources().getString(R.string.notime));
                    if (mNextPilot != null && mNextPilot.pilot_id == p.pilot_id && mNextPilot.position == position)
                        row.setBackgroundColor(ContextCompat.getColor(mContext, R.color.lt_grey));

                } else {
                    time.setText(String.format("%.2f", p.time));
                    row.setBackgroundColor(ContextCompat.getColor(mContext, R.color.dk_grey));
                }

                TextView points = row.findViewById(R.id.points);
                if (p.flown || p.status == Pilot.STATUS_RETIRED) {
                    points.setText(String.format("%.2f", p.points));
                } else {
                    points.setText("");
                }

                row.setTag(position);

                return row;
            }
        };
    }

    /*
     * Handle + button press
     * Get all Pilots that are not in the race
     * Display a picker
     * Add selected pilots into the database and dismiss
     */
    private void getRemainingNamesArray() {
        PilotData datasource = new PilotData(this);
        datasource.open();
        ArrayList<Pilot> allPilots = datasource.getAllPilotsExcept(mArrPilots);
        datasource.close();

        mArrRemainingNames = new ArrayList<>();
        mArrRemainingPilots = new ArrayList<>();

        for (Pilot p : allPilots) {
            mArrRemainingNames.add(String.format("%s %s", p.firstname, p.lastname));
            mArrRemainingPilots.add(p);

        }
    }

    private void showPilotsDialog() {
        getRemainingNamesArray();
        _options = new String[mArrRemainingNames.size()];
        _options = mArrRemainingNames.toArray(_options);
        _selections = new boolean[_options.length];

        String[] buttons_array = new String[2];
        buttons_array[0] = getString(android.R.string.cancel);
        buttons_array[1] = getString(android.R.string.ok);

        mDLG4 = GenericCheckboxPicker.newInstance(
                getString(R.string.ttl_select_pilots),
                mArrRemainingNames,
                buttons_array,
                new ResultReceiver(new Handler()) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        super.onReceiveResult(resultCode, resultData);
                        switch (resultCode) {
                            case 0:
                                break;
                            case 1:
                                if (resultData.containsKey("selected")) {
                                    _selections = resultData.getBooleanArray("selected");
                                    if (_selections != null) {
                                        for (int i = 0; i < _options.length; i++) {
                                            if (_selections[i]) addPilot(mArrRemainingPilots.get(i));
                                        }
                                    }
                                }
                                getNamesArray();
                                mArrAdapter.notifyDataSetChanged();
                                break;
                        }
                    }
                }
        );

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(mDLG4, DIALOG);
        ft.commit();
    }

    private boolean showPilotDialog(int round, int pilot_id, String bib_no) {
        if (mPilotDialogShown) return true;
        if (mTimeoutDialogShown) return false;
        if (mMenuShown) return false;
        if (mTimeoutCompleteDialogShown) return false;

        Intent intent = new Intent(this, RaceTimerActivity.class);
        intent.putExtra("pilot_id", pilot_id);
        intent.putExtra("race_id", mRid);
        intent.putExtra("round", round);
        intent.putExtra("bib_no", bib_no);
        startActivityForResult(intent, DLG_TIMER);

        return true;
    }

    private void showNextPilot() {
        if (mPilotDialogShown) return;
        if (mTimeoutDialogShown) return;
        if (mTimeoutCompleteDialogShown) return;
        if (mMenuShown) return;

        if (mNextPilot.position != null) {
            int round = mArrRounds.get(mNextPilot.position);
            mPilotDialogShown = showPilotDialog(round, mNextPilot.id, mNextPilot.number);
        } else {
            showNextRound();
        }
    }

    private void showNextRound() {
        if (mPilotDialogShown) return;
        if (mTimeoutDialogShown) return;
        if (mTimeoutCompleteDialogShown) return;
        if (mMenuShown) return;

        invalidateOptionsMenu(); // Refresh menu so that next round becomes active
        Intent intent = new Intent(this, NextRoundActivity.class);
        intent.putExtra("round_id", mRnd);
        startActivityForResult(intent, DLG_NEXT_ROUND);
    }

    private void enterManualTimeForPilot(Pilot p, Integer round) {
        Intent intent = new Intent(this, TimeEntryActivity.class);
        intent.putExtra("pilot_id", p.id);
        intent.putExtra("round", round);
        startActivityForResult(intent, DLG_TIME_SET);
    }

    private void showTimeout(long start) {
        if (mPilotDialogShown) return;
        if (mMenuShown) return;
        if (mTimeoutDialogShown) return;
        if (mTimeoutCompleteDialogShown) return;

        Intent intent = new Intent(mContext, RaceRoundTimeoutActivity.class);
        intent.putExtra("start", start);
        intent.putExtra("group_scored", (mGroupScoring.num_groups > 1));
        startActivityForResult(intent, DLG_TIMEOUT);
        mTimeoutDialogShown = true;
    }

    private void showTimeoutComplete() {
        if (mPilotDialogShown) return;
        if (mMenuShown) return;
        if (mTimeoutDialogShown) return;
        if (mTimeoutCompleteDialogShown) return;

        Intent intent = new Intent(mContext, RaceRoundTimeoutActivity.class);
        intent.putExtra("start", 0L);
        intent.putExtra("group_scored", (mGroupScoring.num_groups > 1));
        startActivityForResult(intent, DLG_TIMEOUT);
        mTimeoutCompleteDialogShown = true;
    }

    private void showTimeoutNotStarted() {
        mMenuShown = false;

        String[] buttons_array = new String[1];
        buttons_array[0] = getString(android.R.string.ok);

        mDLG = GenericAlert.newInstance(
                getString(R.string.err_round_timeout),
                getString(R.string.err_round_timeout_inactive),
                buttons_array,
                new ResultReceiver(new Handler()) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        super.onReceiveResult(resultCode, resultData);
                        mDLG.dismiss();
                    }
                }
        );

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(mDLG, DIALOG);
        ft.commit();
    }

    public boolean isServiceRunning(String serviceClassName) {
        final ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            final List<android.app.ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

            for (android.app.ActivityManager.RunningServiceInfo runningServiceInfo : services) {
                if (runningServiceInfo.service.getClassName().equals(serviceClassName)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Binding for UI->Service Communication
    public void sendCommand(String cmd) {
        Intent i = new Intent(IComm.RCV_UPDATE_FROM_UI);
        i.putExtra(IComm.MSG_UI_CALLBACK, cmd);
        sendBroadcast(i);
    }

    // Binding for Service->UI Communication
    private BroadcastReceiver onBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(IComm.MSG_SERVICE_CALLBACK)) {
                Bundle extras = intent.getExtras();
                String data = null;
                if (extras != null) {
                    data = extras.getString(IComm.MSG_SERVICE_CALLBACK);
                }
                if (data == null) {
                    return;
                }

                if (data.equals("start_pressed")) {
                    if (mPilotDialogShown) return;
                    if (mTimeoutCompleteDialogShown) return;

                    if (mNextPilot != null) {
                        Intent intent2 = new Intent(mContext, RaceTimerActivity.class);
                        intent2.putExtra("pilot_id", mNextPilot.id);
                        intent2.putExtra("race_id", mRid);
                        intent2.putExtra("round", mNextPilot.round);
                        intent2.putExtra("bib_no", mNextPilot.number);
                        startActivityForResult(intent2, DLG_TIMER);
                        mPilotDialogShown = true;
                    }
                }

                if (data.equals("show_timeout")) {
                    long start = intent.getLongExtra("start", 0);
                    Log.d("PPP", "START: " + start);
                    if (start > 0)
                        showTimeout(start);
                }

                if (data.equals("show_timeout_complete")) {
                    showTimeoutComplete();
                }

                if (data.equals("show_timeout_not_started")) {
                    showTimeoutNotStarted();
                }

                if (data.equals("driver_started")) {
                    mStatusIcon = extras.getString("icon");

                    mConnectionStatus = true;
                    int id = getResources().getIdentifier(mStatusIcon, "mipmap", getPackageName());
                    if (id == 0) {
                        id = getResources().getIdentifier(mStatusIcon, "drawable", getPackageName());
                    }
                    mStatus.setImageDrawable(ContextCompat.getDrawable(mContext, id));
                }

                if (data.equals("driver_stopped")) {
                    mStatusIcon = extras.getString("icon");

                    mConnectionStatus = false;
                    int id = getResources().getIdentifier(mStatusIcon, "mipmap", getPackageName());
                    if (id == 0) {
                        id = getResources().getIdentifier(mStatusIcon, "drawable", getPackageName());
                    }
                    mStatus.setImageDrawable(ContextCompat.getDrawable(mContext,id));
                }

                if (data.equals("external_display_connected")) {
                    mExternalDisplayStatusIcon = extras.getString("icon");

                    mDisplayStatus = true;
                    int id = getResources().getIdentifier(mExternalDisplayStatusIcon, "mipmap", getPackageName());
                    if (id == 0) {
                        id = getResources().getIdentifier(mExternalDisplayStatusIcon, "drawable", getPackageName());
                    }
                    mExternalDisplayStatus.setImageDrawable(ContextCompat.getDrawable(mContext, id));
                }

                if (data.equals("external_display_disconnected")) {
                    mExternalDisplayStatusIcon = extras.getString("icon");

                    mDisplayStatus = false;
                    int id = getResources().getIdentifier(mExternalDisplayStatusIcon, "mipmap", getPackageName());
                    if (id == 0) {
                        id = getResources().getIdentifier(mExternalDisplayStatusIcon, "drawable", getPackageName());
                    }
                    mExternalDisplayStatus.setImageDrawable(ContextCompat.getDrawable(mContext, id));
                }

                if (data.equals("unsupported")) {
                    String vid = extras.getString("vendorId");
                    String pid = extras.getString("productId");

                    String[] buttons_array = new String[1];
                    buttons_array[0] = getString(android.R.string.ok);

                    mDLG = GenericAlert.newInstance(
                            getString(R.string.err_unsupported),
                            String.format(getString(R.string.err_unsupported_details), vid, pid),
                            buttons_array,
                            null
                    );

                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.addToBackStack(null);
                    ft.add(mDLG, DIALOG);
                    ft.commit();
                }

                if (data.equals("no_bluetooth")) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH);
                }

                if (data.equals("finishRace")) {
                    finishRace();
                }

                if (data.equals(Pref.BASEA_IP)) {
                    String baseA = extras.getString(Pref.BASEA_IP, "");
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(Pref.BASEA_IP, baseA);
                    editor.apply();
                }

                if (data.equals(Pref.BASEB_IP)) {
                    String baseB = extras.getString(Pref.BASEB_IP, "");
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(Pref.BASEB_IP, baseB);
                    editor.apply();
                }
/*
                if (data.equals("wind_legal")) {

                }

                if (data.equals("wind_illegal")) {
                }
 */
            }

            if (intent.hasExtra("com.marktreble.f3ftimer.value.wind_values")) {
                if (mPrefWindMeasurement) {
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        mWindReadings.setText(extras.getString("com.marktreble.f3ftimer.value.wind_values"));
                    }
                }
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.race, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        getNamesArray(); // just to ensure that the bools below are up to date!

        // Round Complete (Only enable when the round is actually complete)
        menu.getItem(0).setEnabled(mRoundComplete);

        // Change Flying Order (Only enable before the RACE is started)
        menu.getItem(1).setEnabled(mRoundNotStarted && mRnd == 1);

        // Change Start Number (Only enable before the round is started)
        menu.getItem(2).setEnabled(mRoundNotStarted);

        // Scrub Round
        menu.getItem(3).setEnabled(!mGroupNotStarted);

        // Group scoring
        // disable when multiple flights are being used
        // disable when <10 pilots
        menu.getItem(5).setEnabled((mRace.rounds_per_flight < 2) && (mArrPilots.size()) >= 10);

        return super.onPrepareOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mMenuShown = false;
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.menu_next_round:
                nextRound();
                return true;
            case R.id.menu_change_flying_order:
                changeFlyingOrder();
                return true;
            case R.id.menu_change_start_number:
                changeStartNumber();
                return true;
            case R.id.menu_scrub_round:

                String[] buttons_array = new String[2];
                buttons_array[0] = getString(android.R.string.cancel);
                buttons_array[1] = getString(android.R.string.ok);

                mDLG = GenericAlert.newInstance(
                        getString(R.string.menu_scrub_round),
                        getString(R.string.menu_scrub_round_confirm),
                        buttons_array,
                        new ResultReceiver(new Handler()) {
                            @Override
                            protected void onReceiveResult(int resultCode, Bundle resultData) {
                                super.onReceiveResult(resultCode, resultData);

                                if (resultCode == 1) {
                                    scrubRound();
                                }
                            }
                        }
                );

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.addToBackStack(null);
                ft.add(mDLG, DIALOG);
                ft.commit();

                return true;
            case R.id.menu_group_score:
                groupScore();
                return true;
            case R.id.menu_show_time_remaining:
                sendCommand("show_round_timeout");
                return true;
            case R.id.menu_add_pilots:
                showPilotsDialog();
                return true;
            case R.id.menu_settings:
                settings();
                return true;
            case R.id.menu_pilot_manager:
                pilotManager();
                return true;
            case R.id.menu_results_manager:
                resultsManager();
                return true;
            case R.id.menu_print_pilots_list:
                print_pilot_list();
                return true;
            case R.id.menu_help:
                help();
                return true;
            case R.id.menu_about:
                about();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        mMenuShown = true;
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        mMenuShown = false;
        super.onOptionsMenuClosed(menu);
    }

    private void finishRace() {
        RaceData datasource = new RaceData(this);
        datasource.open();
        datasource.nextRound(mRid);
        datasource.setStatus(mRid, Race.STATUS_COMPLETE);
        datasource.close();

        if (mPrefResultsF3Fgear) {
            if (!new F3FGearExport().writeResultsFile(mContext, mRace)) {
                // Failed to write
                // Need some UI to indicate the problem
                Log.d("PPP", "Failed to write F3FGear");
            }
        }

        finish();
    }

    private void nextRound() {
        RacePilotData datasource = new RacePilotData(RaceActivity.this);
        datasource.open();
        ArrayList<Pilot> allPilots = datasource.getAllPilotsForRace(mRid, mRnd, mRace.offset, mRace.start_number);
        datasource.close();

        RaceData datasource2 = new RaceData(this);
        datasource2.open();
        mRace = datasource2.nextRound(mRid);
        mRnd = mRace.round;
        mGroupScoring = datasource2.setGroups(mRid, mRnd, 1, getStartPilot(allPilots, mRace));
        datasource.close();

        // Update the spreadsheet file
        if (!new SpreadsheetExport().writeResultsFile(mContext, mRace)) {
            // Failed to write
            // Need some UI to indicate the problem
        }

        if (mPrefResultsF3Fgear) {
            if (!new F3FGearExport().writeResultsFile(mContext, mRace)) {
                // Failed to write
                // Need some UI to indicate the problem
                Log.d("PPP", "Failed to write F3FGear");
            }
        }

        setRound();
        getNamesArray();
        mArrAdapter.notifyDataSetChanged();
        invalidateOptionsMenu(); // Refresh menu so that next round becomes active

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                mListView.smoothScrollToPositionFromTop(0, 0, 500);
            }
        }, 100);

        // Bring up next pilot's dialog
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                showNextPilot();
            }
        }, 1000);

    }

    public void scrubRound() {
        sendCommand("cancel_timeout");

        if (mGroupScoring.num_groups == 1) {
            // Not group scored
            // Delete all times from round
            RacePilotData datasource = new RacePilotData(RaceActivity.this);
            datasource.open();
            datasource.deleteRound(mRid, mRnd);
            datasource.close();

            // Delete the round
            RaceData datasource2 = new RaceData(RaceActivity.this);
            datasource2.open();
            datasource2.deleteRound(mRid, mRnd);
            datasource2.close();
        } else {
            // Just delete the times for the current group
            RacePilotData datasource = new RacePilotData(RaceActivity.this);
            datasource.open();
            Log.i("RACEACTIVITY", "DELETE GROUP: " + mRid + ":" + mRnd + ":" + mNextPilot.position);
            datasource.deleteGroup(mRid, mRnd, mNextPilot.position, mArrGroups, mArrPilots);
            datasource.close();
        }

        getNamesArray();
        mArrAdapter.notifyDataSetChanged();

    }

    public void changeFlyingOrder() {
        Intent intent = new Intent(mContext, FlyingOrderEditActivity.class);
        intent.putExtra("race_id", mRid);
        startActivityForResult(intent, DLG_FLYING_ORDER_EDIT);
    }

    public void changeStartNumber() {
        Intent intent = new Intent(mContext, StartNumberEditActivity.class);
        intent.putExtra("race_id", mRid);
        startActivityForResult(intent, DLG_START_NUMBER_EDIT);
    }

    public void groupScore() {
        Intent intent = new Intent(mContext, GroupScoreEditActivity.class);
        int max_groups = (int) Math.floor(mArrPilots.size() / 10f);
        intent.putExtra("max_groups", max_groups);
        intent.putExtra("current_groups", mGroupScoring.num_groups);
        startActivityForResult(intent, DLG_GROUP_SCORE_EDIT);
    }

    private void addPilot(Pilot p) {
        RacePilotData datasource = new RacePilotData(this);
        datasource.open();
        datasource.addPilot(p, mRid);
        datasource.close();
    }

    private void editPilot(Pilot p) {
        Intent intent = new Intent(this, PilotsEditActivity.class);
        intent.putExtra("pilot_id", p.id);
        intent.putExtra("race_id", mRid);
        intent.putExtra("caller", "racemanager");
        startActivityForResult(intent, DLG_PILOT_EDIT);
    }

    public void settings() {
        Intent intent = new Intent(mContext, SettingsActivity.class);
        startActivityForResult(intent, DLG_SETTINGS);
    }

    public void pilotManager() {
        Intent intent = new Intent(mContext, PilotsActivity.class);
        startActivity(intent);
    }

    public void resultsManager() {
        Intent intent = new Intent(mContext, ResultsActivity.class);
        startActivity(intent);
    }

    public void print_pilot_list() {
        RacePilotData datasource = new RacePilotData(this);
        datasource.open();
        ArrayList<Pilot> allPilots = datasource.getAllPilotsForRace(mRid, 0, mRace.offset, 1);
        datasource.close();

        PrintManager printManager = (PrintManager) this
                .getSystemService(Context.PRINT_SERVICE);
        if (printManager == null) return;

        String jobName = String.format("%s %s", getString(R.string.app_name), getString(R.string.ttl_pilot_list));

        PrintAttributes.Builder PAbuilder = new PrintAttributes.Builder();
        PAbuilder.setMediaSize(PrintAttributes.MediaSize.ISO_A4);
        PAbuilder.setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PAbuilder.setDuplexMode(PrintAttributes.DUPLEX_MODE_SHORT_EDGE);
        }
        printManager.print(jobName, new PilotListDocumentAdapter(this, allPilots, mRace.name),
                PAbuilder.build());
    }

    public void help() {
        Intent intent = new Intent(mContext, HelpActivity.class);
        startActivity(intent);
    }

    public void about() {
        Intent intent = new Intent(mContext, AboutActivity.class);
        startActivity(intent);
    }

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            mBatteryLevel = String.format("%d%%", level);
            mPower.setText(mBatteryLevel);
        }
    };
}