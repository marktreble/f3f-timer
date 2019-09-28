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
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.constants.IComm;
import com.marktreble.f3ftimer.languages.Languages;
import com.marktreble.f3ftimer.media.TTS;
import com.marktreble.f3ftimer.wifi.Wifi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SettingsFragment extends PreferenceFragment
        implements OnSharedPreferenceChangeListener, TTS.onInitListenerProxy {

    private TTS mTts;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTts = TTS.sharedTTS(getActivity(), this);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null)
            view.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.background_dialog));

        // Set values
        setInputSourceActiveFields();
        setBTDeviceSummary("pref_input_src_device");
        setLangSummary("pref_voice_lang");
        setLangSummary("pref_input_src");
        setStringSummary("pref_usb_baudrate");
        setStringSummary("pref_input_tcpio_ip");
        setListSummary("pref_usb_stopbits");
        setListSummary("pref_usb_databits");
        setListSummary("pref_usb_parity");
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

        return view;

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
                || key.equals("pref_usb_tether")
                || key.equals("pref_audible_wind_warning")
                || key.equals("pref_full_volume")) {
            // Send to Service
            sendBooleanValueToService(key, sharedPreferences.getBoolean(key, false));
        }

        if (key.equals("pref_usb_baudrate")
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

    private void sendStringValueToService(String key, String value) {
        Intent i = new Intent(IComm.RCV_UPDATE_FROM_UI);
        i.putExtra("com.marktreble.f3ftimer.ui_callback", key);
        i.putExtra("com.marktreble.f3ftimer.value", value);
        getActivity().sendBroadcast(i);
    }

    private void sendBooleanValueToService(String key, boolean value) {
        Intent i = new Intent(IComm.RCV_UPDATE_FROM_UI);
        i.putExtra("com.marktreble.f3ftimer.ui_callback", key);
        i.putExtra("com.marktreble.f3ftimer.value", value);
        getActivity().sendBroadcast(i);
    }

    private void setInputSourceActiveFields() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String inputSource = sharedPref.getString("pref_input_src", "");
        if (inputSource.equals(getString(R.string.BLUETOOTH_HC_05))) {
            // BT - Hide baud rate etc.., and show device picker
            findPreference("pref_input_tcpio_ip").setEnabled(false);
            findPreference("pref_usb_baudrate").setEnabled(false);
            findPreference("pref_usb_stopbits").setEnabled(false);
            findPreference("pref_usb_databits").setEnabled(false);
            findPreference("pref_usb_parity").setEnabled(false);

            findPreference("pref_input_src_device").setEnabled(true);

        } else if (inputSource.equals(getString(R.string.Demo))) {
            // Demo mode - hide all options
            findPreference("pref_input_tcpio_ip").setEnabled(false);
            findPreference("pref_usb_baudrate").setEnabled(false);
            findPreference("pref_usb_stopbits").setEnabled(false);
            findPreference("pref_usb_databits").setEnabled(false);
            findPreference("pref_usb_parity").setEnabled(false);

            findPreference("pref_input_src_device").setEnabled(false);
        } else {
            // USB - Hide device picker, show baud rate etc..
            if (inputSource.equals(getString(R.string.TCP_IO))) {
                findPreference("pref_input_tcpio_ip").setEnabled(true);
            } else {
                findPreference("pref_input_tcpio_ip").setEnabled(false);
            }

            findPreference("pref_usb_baudrate").setEnabled(true);
            findPreference("pref_usb_stopbits").setEnabled(true);
            findPreference("pref_usb_databits").setEnabled(true);
            findPreference("pref_usb_parity").setEnabled(true);

            findPreference("pref_input_src_device").setEnabled(false);

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

    private void setListSummary(String key) {
        Preference pref = findPreference(key);
        if (pref instanceof ListPreference) {
            ListPreference listPref = (ListPreference) pref;
            String value = (String) listPref.getEntry();
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
        ListPreference pref = (ListPreference) findPreference("pref_input_src_device");
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