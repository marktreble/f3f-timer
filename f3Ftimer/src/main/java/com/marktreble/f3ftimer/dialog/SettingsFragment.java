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

package com.marktreble.f3ftimer.dialog;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceFragmentCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.constants.IComm;
import com.marktreble.f3ftimer.constants.Pref;
import com.marktreble.f3ftimer.languages.Languages;
import com.marktreble.f3ftimer.media.TTS;
import com.marktreble.f3ftimer.wifi.Wifi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static android.app.Activity.RESULT_OK;

public class SettingsFragment extends PreferenceFragmentCompat
        implements OnSharedPreferenceChangeListener, TTS.onInitListenerProxy {

    private TTS mTts;

    final private int REQUEST_PICK_FOLDER = 9999;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTts = TTS.sharedTTS(getActivity(), this);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null)
            view.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.background_dialog));

        // Set values
        setInputSourceActiveFields();
        setBTDeviceSummary(Pref.INPUT_SRC_DEVICE);
        setLangSummary("pref_voice_lang");
        setLangSummary(Pref.INPUT_SRC);
        setStringSummary(Pref.USB_BAUDRATE  );
        setStringSummary("pref_input_tcpio_ip");
        setListSummary(Pref.USB_STOPBITS);
        setListSummary(Pref.USB_DATABITS);
        setListSummary(Pref.USB_PARITY);
        setListSummary(Pref.IOIO_RX_PIN);
        setListSummary(Pref.IOIO_TX_PIN);

        setBTDeviceSummary("pref_external_display");

        setStringSummary("pref_wind_angle_offset");
        setStringSummary("pref_wind_measurement");

        setListSummary("pref_buzz_off_course");
        setListSummary("pref_buzz_on_course");
        setListSummary("pref_buzz_turn");
        setListSummary("pref_buzz_turn9");
        setListSummary("pref_buzz_penalty");

        setListSummary("pref_app_theme");


        Preference pref_results_server = findPreference("pref_results_server");

        Preference pref_wifi_hotspot = findPreference("pref_wifi_hotspot");
        String ip = Wifi.getIPAddress(true);
        if (pref_wifi_hotspot.isEnabled()) {
            pref_results_server.setSummary("Broadcasting results over wifi (http://" + ip + ":8080)\nYour device may not support this.\nYou will need to enable 'portable wifi hotspot' manually in your settings app.");
        } else {
            pref_results_server.setSummary("Serve results over HTTP");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Preference filePicker = findPreference("pref_results_F3Fgear_path");
            SharedPreferences path = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String strPath = path.getString("pref_results_F3Fgear_path", "");

            Uri uri = Uri.parse(strPath);
            String dcdPath = uri.getLastPathSegment();
            filePicker.setSummary(dcdPath);

            filePicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    i.addCategory(Intent.CATEGORY_DEFAULT);
                    startActivityForResult(Intent.createChooser(i, "Choose directory"), REQUEST_PICK_FOLDER);
                    return true;
                }
            });
        }

        Preference myPref = findPreference(Pref.RESET_BUTTON);
        myPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
                resetToDefault(prefs, Pref.RESET_BUTTON);
                return true;
            }
        });
        return view;

    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PICK_FOLDER:
                    Uri uri = data.getData();

                    final int takeFlags = getActivity().getIntent().getFlags()
                            & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    // Check for the freshest data.
                    getActivity().getContentResolver().takePersistableUriPermission(uri, takeFlags);

                    Preference filePicker = findPreference("pref_results_F3Fgear_path");
                    filePicker.setSummary(uri.getLastPathSegment());
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    pref.edit().putString("pref_results_F3Fgear_path", uri.toString()).apply();

                    break;
                default:
                    // Nothing
                    break;
            }
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        setInputSourceActiveFields();

        // Update value of any list preference
        Preference pref = findPreference(key);
        if (pref instanceof ListPreference) {
            setLangSummary(key);
        }

        // Callbacks to input driver

        if (key.equals("pref_buzzer")
                || key.equals("pref_voice")
                || key.equals("pref_results_server")
                || key.equals("pref_wind_measurement")
                || key.equals(Pref.USB_TETHER)
                || key.equals("pref_audible_wind_warning")
                || key.equals("pref_full_volume")) {
            // Send to Service
            sendBooleanValueToService(key, sharedPreferences.getBoolean(key, false));
        }

        if (key.equals(Pref.USB_BAUDRATE  )
                || key.equals("pref_input_tcpio_ip")) {
            setStringSummary(key);
            sendStringValueToService(key, sharedPreferences.getString(key, ""));
        }

        if (key.equals("pref_buzz_off_course")
                || key.equals("pref_buzz_on_course")
                || key.equals("pref_buzz_turn")
                || key.equals("pref_buzz_turn9")
                || key.equals("pref_buzz_penalty")) {
            setStringSummary(key);
            sendStringValueToService(key, sharedPreferences.getString(key, ""));
        }


        if (key.equals("pref_voice_lang")) {
            // Send to Service
            setLangSummary(key);
            String lang = sharedPreferences.getString(key, "");
            String[] l = lang.split("_");
            if (l.length == 2) {
                Locale lo = new Locale(l[0], l[1]);
                // set default text language
                getResources().getConfiguration().locale = lo;
                sendStringValueToService(key, sharedPreferences.getString(key, ""));
                Log.i("SETTINGS", "Changed speech language to " + lo.getDisplayName());
            }
        }

        if (key.equals("pref_wind_angle_offset")) {
            float angle = Float.parseFloat(sharedPreferences.getString(key, "0.0"));
            if (angle < 0) {
                angle = -angle;
            }
            if (angle > 360) {
                angle = angle % 360;
            }
            String anglestr = String.format("%.1f", angle).replace(",", ".");
            if (pref instanceof EditTextPreference) {
                EditTextPreference textPref = (EditTextPreference) pref;
                textPref.setText(anglestr);
            }
            setStringSummary("pref_wind_angle_offset");
            sendStringValueToService(key, anglestr);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    private void resetToDefault(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Pref.RESET_BUTTON)) {
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.putString(Pref.USB_BAUDRATE, Pref.USB_BAUDRATE_DEFAULT);
            edit.putString(Pref.USB_STOPBITS, Pref.USB_STOPBITS_DEFAULT);
            edit.putString(Pref.USB_DATABITS, Pref.USB_DATABITS_DEFAULT);
            edit.putString(Pref.USB_PARITY, Pref.USB_PARITY_DEFAULT);
            edit.putString(Pref.IOIO_RX_PIN, Pref.IOIO_RX_PIN_DEFAULT);
            edit.putString(Pref.IOIO_TX_PIN, Pref.IOIO_TX_PIN_DEFAULT);
            edit.commit();


            setStringSummary(Pref.USB_BAUDRATE, Pref.USB_BAUDRATE_DEFAULT);
            setListSummary(Pref.USB_STOPBITS, Pref.USB_STOPBITS_DEFAULT);
            setListSummary(Pref.USB_DATABITS, Pref.USB_DATABITS_DEFAULT);
            setListSummary(Pref.USB_PARITY, Pref.USB_PARITY_DEFAULT);
            setListSummary(Pref.IOIO_RX_PIN, Pref.IOIO_RX_PIN_DEFAULT);
            setListSummary(Pref.IOIO_TX_PIN, Pref.IOIO_TX_PIN_DEFAULT);

        }
    }

    private void sendStringValueToService(String key, String value) {
        Intent i = new Intent(IComm.RCV_UPDATE_FROM_UI);
        i.putExtra(IComm.MSG_UI_CALLBACK, key);
        i.putExtra(IComm.MSG_VALUE, value);
        getActivity().sendBroadcast(i);
    }

    private void sendBooleanValueToService(String key, boolean value) {
        Intent i = new Intent(IComm.RCV_UPDATE_FROM_UI);
        i.putExtra(IComm.MSG_UI_CALLBACK, key);
        i.putExtra(IComm.MSG_VALUE, value);
        getActivity().sendBroadcast(i);
    }

    private void setInputSourceActiveFields() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String inputSource = sharedPref.getString(Pref.INPUT_SRC, "");
        if (inputSource.equals(getString(R.string.BLUETOOTH_HC_05))) {
            // BT - Hide baud rate etc.., and show device picker
            findPreference(Pref.INPUT_TCPIO_IP).setEnabled(false);
            findPreference(Pref.USB_BAUDRATE).setEnabled(false);
            findPreference(Pref.USB_STOPBITS).setEnabled(false);
            findPreference(Pref.USB_DATABITS).setEnabled(false);
            findPreference(Pref.USB_PARITY).setEnabled(false);
            findPreference(Pref.RESET_BUTTON).setEnabled(false);


            findPreference(Pref.INPUT_SRC_DEVICE).setEnabled(true);

        } else if (inputSource.equals(getString(R.string.Demo))) {
            // Demo mode - hide all options
            findPreference(Pref.INPUT_TCPIO_IP).setEnabled(false);
            findPreference(Pref.USB_BAUDRATE).setEnabled(false);
            findPreference(Pref.USB_STOPBITS).setEnabled(false);
            findPreference(Pref.USB_DATABITS).setEnabled(false);
            findPreference(Pref.USB_PARITY).setEnabled(false);
            findPreference(Pref.RESET_BUTTON).setEnabled(false);


            findPreference(Pref.INPUT_SRC_DEVICE).setEnabled(false);
        } else {
            // USB - Hide device picker, show baud rate etc..
            if (inputSource.equals(getString(R.string.TCP_IO))) {
                findPreference(Pref.INPUT_TCPIO_IP).setEnabled(true);
            } else {
                findPreference(Pref.INPUT_TCPIO_IP).setEnabled(false);
            }

            findPreference(Pref.USB_BAUDRATE  ).setEnabled(true);
            findPreference(Pref.USB_STOPBITS).setEnabled(true);
            findPreference(Pref.USB_DATABITS).setEnabled(true);
            findPreference(Pref.USB_PARITY).setEnabled(true);
            findPreference(Pref.RESET_BUTTON).setEnabled(true);

            findPreference(Pref.INPUT_SRC_DEVICE).setEnabled(false);

        }

        if (inputSource.equals(getString(R.string.USB_IOIO))) {
            findPreference(Pref.IOIO_RX_PIN).setEnabled(true);
            findPreference(Pref.IOIO_TX_PIN).setEnabled(true);
        } else {
            findPreference(Pref.IOIO_RX_PIN).setEnabled(false);
            findPreference(Pref.IOIO_TX_PIN).setEnabled(false);
        }
    }

    private void setLangSummary(String key) {
        Preference pref = findPreference(key);
        if (pref instanceof ListPreference) {
            ListPreference listPref = (ListPreference) pref;
            String value = (String) listPref.getEntry();
            if (value == null) {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                Locale current = getResources().getConfiguration().locale;
                String p = sharedPref.getString("pref_voice_lang", "");
                String lang = (p.equals("")) ? current.getLanguage() : p.substring(0, 2);
                String country = (p.equals("")) ? current.getCountry() : p.substring(3, 5);
                Locale l = new Locale(lang, country);
                value = l.getDisplayName();
            }
            pref.setSummary(value);
        }
    }

    private void setBTDeviceSummary(String key) {
        Preference pref = findPreference(key);
        if (pref instanceof ListPreference) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String value = sharedPref.getString(key, "No Devices Paired");

            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null) {
                if (bluetoothAdapter.isEnabled()) {
                    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                    if (pairedDevices.size() > 0) {
                        for (BluetoothDevice device : pairedDevices) {
                            if (device.getAddress().equals(value)) {
                                value = device.getName();
                            }
                        }
                    }
                }
            }
            pref.setSummary(value);

        }
    }

    private void setStringSummary(String key) {
        Preference pref = findPreference(key);
        if (pref instanceof EditTextPreference) {
            EditTextPreference textPref = (EditTextPreference) pref;
            String value = textPref.getText();
            pref.setSummary(value);
        }
    }

    private void setStringSummary(String key, String value) {
        Preference pref = findPreference(key);
        if (pref instanceof EditTextPreference) {
            pref.setSummary(value);
        }
    }

    private void setListSummary(String key) {
        Preference pref = findPreference(key);
        if (pref instanceof ListPreference) {
            ListPreference listPref = (ListPreference) pref;
            String value = (String) listPref.getEntry();
            pref.setSummary(value);
        }
    }

    private void setListSummary(String key, String value) {
        Preference pref = findPreference(key);
        if (pref instanceof ListPreference) {
            ListPreference listPref = (ListPreference) pref;
            pref.setSummary(value);
        }
    }

    @Override
    public void onInit(int status) {
        // Populate pref_input_src_device with paired devices
        populateInputSourceDevices();

        // Populate pref_external_display with paired devices
        populateExternalDisplayDevices();

        // Populate pref_voice_lang with installed voices
        populateVoices();

    }

    public void onStart(String utteranceId) {
    }

    public void onDone(String utteranceId) {
    }

    public void onError(String utteranceId) {
    }

    private void populateVoices() {
        String[] languages = Languages.getAvailableLanguages(getActivity());

        // Now check the available languages against the installed TTS Voices
        Locale[] locales = Locale.getAvailableLocales();
        List<String> list_values = new ArrayList<>();
        List<String> list_labels = new ArrayList<>();
        for (Locale locale : locales) {

            int ttsres = TextToSpeech.LANG_NOT_SUPPORTED;
            try {
                ttsres = mTts.ttsengine().isLanguageAvailable(locale);
            } catch (IllegalArgumentException e) {
                //e.printStackTrace();
            }

            boolean hasLang = false;
            for (String lang : languages) {
                if (lang.equals(locale.getLanguage())) hasLang = true;
            }
            if ((ttsres == TextToSpeech.LANG_COUNTRY_AVAILABLE) && hasLang) {
                list_labels.add(locale.getDisplayName());
                list_values.add(locale.getLanguage() + "_" + locale.getCountry());
                Log.i("AVAILABLE LANGUAGES", locale.getDisplayName() + ":" + locale.getLanguage() + "_" + locale.getCountry());
            }
        }

        ListPreference pref = (ListPreference) findPreference("pref_voice_lang");
        CharSequence[] labels = list_labels.toArray(new CharSequence[0]);
        CharSequence[] values = list_values.toArray(new CharSequence[0]);
        pref.setEntries(labels);
        pref.setEntryValues(values);

    }

    private void populateInputSourceDevices() {
        ListPreference pref = (ListPreference) findPreference(Pref.INPUT_SRC_DEVICE);
        populatePairedDevices(pref);
    }

    private void populateExternalDisplayDevices() {
        ListPreference pref = (ListPreference) findPreference("pref_external_display");
        populatePairedDevices(pref);
    }

    private void populatePairedDevices(ListPreference pref) {
        CharSequence[] labels = {"No Devices Paired"};
        CharSequence[] values = {""};

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    List<String> list_values = new ArrayList<>();
                    List<String> list_labels = new ArrayList<>();
                    for (BluetoothDevice device : pairedDevices) {
                        list_labels.add(device.getName());
                        list_values.add(device.getAddress());
                    }

                    labels = list_labels.toArray(new CharSequence[0]);
                    values = list_values.toArray(new CharSequence[0]);
                }
            }
        }


        pref.setEntries(labels);
        pref.setEntryValues(values);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTts != null) {
            mTts.release();
            mTts = null;
        }
    }
}