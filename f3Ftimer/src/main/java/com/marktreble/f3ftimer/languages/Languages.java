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

package com.marktreble.f3ftimer.languages;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.util.Log;

import com.marktreble.f3ftimer.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.StringTokenizer;


public class Languages {
    /**
     *  Get all the available languages as a String array of ISO 639-1 (2 letter language codes)
     *  Sorted in alphabetical order
     *
     * @param context Context
     * @return String[]
     */
    public static String[] getAvailableLanguages(Context context) {
        ArrayList<String> al_languages = new ArrayList<>();
        DisplayMetrics metrics = new DisplayMetrics();
        Resources r = context.getResources();
        Configuration c = r.getConfiguration();
        AssetManager a = r.getAssets();

        String[] tmp_languages = {};
        try {
            tmp_languages = a.getLocales();
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (String _language : tmp_languages) {
            if (_language.split("_").length == 1) { // Language only locales (no country code)
                c.locale = new Locale(_language);
                Resources res = new Resources(context.getAssets(), metrics, c);
                try {
                    // check if the app_name key exists. If it does, the language is available
                    String name = res.getString(R.string.locale);
                    if (!al_languages.contains(name))
                        al_languages.add(name);
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
            } else {

                String[] l = _language.split("_");
                c.locale = new Locale(l[0]);
                Resources res = new Resources(context.getAssets(), metrics, c);
                try {
                    // check if the app_name key exists. If it does, the language is available
                    String name = res.getString(R.string.locale);
                    if (!al_languages.contains(name))
                        al_languages.add(name);

                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        for (String language : al_languages) {
            Log.i("LANG", language);
        }
        Collections.sort(al_languages);
        return al_languages.toArray(new String[0]);
    }

    /**
     * Returns a handle to the resources for a given textual language
     *
     * @param context Context
     * @param lang String
     * @return Resources
     */
    public static Resources useLanguage(Context context, String lang) {
        Resources res = context.getResources();
        Configuration config = res.getConfiguration();
        config.locale = Languages.stringToLocale(lang);
        res.updateConfiguration(config, null);
        return res;
    }

    /**
     * Returns the language string that has been set:
     * to_lang - if set to the requested
     * fallback - if requested is not available
     * empty string - if no change is required
     *
     * @param to_lang String
     * @param fallback String
     * @param engine TextToSpeech
     * @return String
     */
    public static String setSpeechLanguage(String to_lang, String fallback, TextToSpeech engine) {
        Locale lang = Languages.stringToLocale(to_lang);
        Locale currLang = engine.getLanguage();
        String ret = "";

        if ((currLang == null)
                || !currLang.getLanguage().equals(lang.getISO3Language())
                || !currLang.getCountry().equals(lang.getISO3Country())) {

            int available = engine.isLanguageAvailable(lang);
            ret = to_lang;
            if (available != TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                if (available != TextToSpeech.LANG_AVAILABLE) {
                    // Switch to default
                    ret = fallback;
                }
            }
            Log.i("Speech Lang to ", ret);
        } else {
            Log.i("Languages", "NO Language Change");
        }
        return ret;
    }

    /**
     * Convert String to Locale
     *
     * @param localeStr String
     * @return Locale
     */
    public static Locale stringToLocale(String localeStr) {
        StringTokenizer tempStringTokenizer = new StringTokenizer(localeStr, "_");
        String l = "";
        String c = "";
        if (tempStringTokenizer.hasMoreTokens())
            l = (String) tempStringTokenizer.nextElement();
        if (tempStringTokenizer.hasMoreTokens())
            c = (String) tempStringTokenizer.nextElement();
        return new Locale(l, c);
    }
}