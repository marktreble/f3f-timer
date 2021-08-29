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

package com.marktreble.f3ftimer.constants;

public class Pref {
    public final static String INPUT_SRC = "pref_input_src";
    public final static String INPUT_SRC_DEVICE = "pref_input_src_device";
    public final static String INPUT_TCPIO_IP = "pref_input_tcpio_ip";

    public final static String USB_BAUDRATE = "pref_usb_baudrate";
    public final static String USB_STOPBITS = "pref_usb_stopbits";
    public final static String USB_DATABITS = "pref_usb_databits";
    public final static String USB_PARITY = "pref_usb_parity";
    public final static String USB_TETHER = "pref_usb_tether";

    public final static String IOIO_RX_PIN = "pref_ioio_rx";
    public final static String IOIO_TX_PIN = "pref_ioio_tx";
    public final static String IOIO_START_PIN = "pref_ioio_start";

    public final static String RESET_BUTTON = "pref_btn_reset_to_default";

    public final static String BASEA_IP = "pref_baseAIP";
    public final static String BASEB_IP = "pref_baseBIP";

    public final static String RESULTS = "pref_results";
    public final static String RESULTS_SERVER = "pref_results_server";
    public final static String WIFI_HOTSPOT = "pref_wifi_hotspot";
    public final static String RESULTS_DISPLAY = "pref_results_display";
    public final static String EXTERNAL_DISPLAY = "pref_external_display";

    public final static String WIND_MEASUREMENT = "pref_wind_measurement";
    public final static String WIND_ANGLE_OFFSET = "pref_wind_angle_offset";
    public final static String AUDIBLE_WIND_WARNING = "pref_audible_wind_warning";


    public final static String FULL_VOLUME = "pref_full_volume";
    public final static String BUZZER = "pref_buzzer";
    public final static String BUZZER_OFF_COURSE = "pref_buzzer_off_course";
    public final static String BUZZER_ON_COURSE = "pref_buzzer_on_course";
    public final static String BUZZER_TURN = "pref_buzzer_turn";
    public final static String BUZZER_TURN9 = "pref_buzzer_turn9";
    public final static String BUZZER_PENALTY = "pref_buzzer_penalty";
    public final static String VOICE = "pref_voice";
    public final static String VOICE_LANG = "pref_voice_lang";

    public final static String MANUAL_ENTRY = "pref_manual_entry";

    public final static String APP_THEME = "pref_app_theme";

    public final static String ENABLE_ACRA = "pref_enable_acra";


    // Defaults
    public final static String USB_BAUDRATE_DEFAULT = "9600";
    public final static String USB_STOPBITS_DEFAULT = "1";
    public final static String USB_DATABITS_DEFAULT = "8";
    public final static String USB_PARITY_DEFAULT = "None";
    public final static Boolean USB_TETHER_DEFAULT = false;

    public final static String IOIO_RX_PIN_DEFAULT = "3";
    public final static String IOIO_TX_PIN_DEFAULT = "4";
    public final static String IOIO_START_PIN_DEFAULT = "46";

}
