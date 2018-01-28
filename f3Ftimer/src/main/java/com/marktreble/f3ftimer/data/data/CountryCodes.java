package com.marktreble.f3ftimer.data.data;

import android.content.Context;
import android.content.res.Resources;

import com.marktreble.f3ftimer.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;

/**
 * Created by marktreble on 09/01/2018.
 */

public class CountryCodes {

    private static CountryCodes sharedCountryCodes;
    private static JSONArray countryCodes;

    private CountryCodes(Context context){
        initCountryData(context);
    }  //private constructor.

    public static CountryCodes sharedCountryCodes(Context context){
        if (sharedCountryCodes == null){ //if there is no instance available... create new one
            sharedCountryCodes = new CountryCodes(context);
        }

        return sharedCountryCodes;
    }

    protected void initCountryData(Context context) {
        try {
            Resources res = context.getResources();
            InputStream in = res.openRawResource(R.raw.countrycodes);
            byte[] ba = new byte[512000];
            in.read(ba);
            in.close();
            String data = new String(ba);
            countryCodes = new JSONArray(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String findIsoCountryCode(String iocCountryCode) {
        try {
            for (int i=0; i< countryCodes.length(); i++) {
                JSONObject country = countryCodes.optJSONObject(i);
                if (country.getString("IOC").equals(iocCountryCode)) {
                    return country.getString("ISO3166-1-Alpha-2");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
