/*
 * Languages
 * Utility functions for language settings
 */

package com.marktreble.f3ftimer.languages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.StringTokenizer;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.util.Log;

import com.marktreble.f3ftimer.R;



public class Languages {
	public static String[] getAvailableLanguages(Context a){
		/*
		 *  Get all the available languages as a String array of 639-1 (2 letter language codes)
		 *  Sorted in alphabetical order
		 */
    	ArrayList<String> al_languages = new ArrayList<>();
    	DisplayMetrics metrics = new DisplayMetrics();
    	Resources r = a.getResources();
        Configuration c = r.getConfiguration();
        String[] tmp_languages = r.getAssets().getLocales();
        for (String _language : tmp_languages){
        	if (_language.split("_").length == 1){ // Language only locales (no country code)
	        	c.locale = new Locale(_language);
	            Resources res = new Resources(a.getAssets(), metrics, c);
	            try {
                    // check if the app_name key exists. If it does, the language is available
                    String name = res.getString(R.string.locale);
                    if (name!=null && !al_languages.contains(name))
                        al_languages.add(name);
	            } catch (NotFoundException e){
	            	
	            }
        	} else {

                String[] l = _language.split("_");
                c.locale = new Locale(l[0]);
                Resources res = new Resources(a.getAssets(), metrics, c);
                try {
                    // check if the app_name key exists. If it does, the language is available
                    String name = res.getString(R.string.locale);
                    if (name!=null && !al_languages.contains(name))
                            al_languages.add(name);

                } catch (NotFoundException e){

                }
            }
        }

        for (String language : al_languages){
            Log.i("LANG", language);
        }
        Collections.sort(al_languages);
        return al_languages.toArray(new String[al_languages.size()]);
	}
	
	public static Resources useLanguage(Context a, String lang){
		/*
		 * Returns a handle to the resources for a given textual language
		 */
		Resources res = a.getResources();
		Configuration config = res.getConfiguration();
        config.locale = Languages.stringToLocale(lang);
        res.updateConfiguration(config, null);
        return res;
	}
	
	public static String setSpeechLanguage(String to_lang, String fallback, TextToSpeech engine){
		/*
		 * Returns the language string that has been set:
		 * to_lang - if set to the requested
		 * fallback - if requested is not available
		 * empty string - if no change is required
		 */
		Locale lang = Languages.stringToLocale(to_lang);
		Locale currLang = engine.getLanguage();
		String ret = "";
		
		if ((currLang == null)
			|| !currLang.getLanguage().equals(lang.getISO3Language()) 
			|| !currLang.getCountry().equals(lang.getISO3Country())){
			
			int available = engine.isLanguageAvailable(lang);
			ret = to_lang;
			if (available != TextToSpeech.LANG_COUNTRY_AVAILABLE){
				if (available != TextToSpeech.LANG_AVAILABLE){
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

	public static Locale stringToLocale(String s){
		/*
		 * Convert String to Locale
		 */
	    StringTokenizer tempStringTokenizer = new StringTokenizer(s,"_");
	    String l = "";
	    String c = "";
	    if(tempStringTokenizer.hasMoreTokens())
	    	l = (String) tempStringTokenizer.nextElement();
	    if(tempStringTokenizer.hasMoreTokens())
	    	c = (String) tempStringTokenizer.nextElement();
	    return new Locale(l,c);
	}
}