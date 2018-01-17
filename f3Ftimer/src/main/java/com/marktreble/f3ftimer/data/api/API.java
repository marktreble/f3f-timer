package com.marktreble.f3ftimer.data.api;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;


import com.marktreble.f3ftimer.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * Created by marktreble on 04/01/2016.
 */


public class API {

    private static final String TAG = "APIDEBUG";

    public static final String STATUS_KEY = "status";
    public static final String STATUS_OK = "ok";

    public static final String ENDPOINT_KEY = "endpoint";

    public static final String API_IMPORT = "/import";
    public static final String API_IMPORT_RACE = "/import_race";
    public static final String API_UPLOAD = "/upload_race";

    public static final String F3XV_IMPORT = "searchEvents";
    public static final String F3XV_IMPORT_RACE = "getEventInfo";
    public static final String F3XV_UPLOAD = "";

    public class httpmethod {
        public static final int GET = 1;
        public static final int POST = 2;
    }

    public interface APICallbackInterface {

        void onAPISuccess(String request, JSONObject result);
        void onAPIError(String request, JSONObject result);
    }

    public APICallbackInterface mCallback;
    public String request;

    private apiCall api;
    public boolean mAppendEndpoint = true;
    public boolean mIsJSON = true;

    public void makeAPICall(Context context, String base, int method, Map<String, String> params){
        api = new apiCall(context, base, method, mAppendEndpoint, mIsJSON, mCallback, request);
        api.execute(params);
    }

    public void cancel(){
        api.cancel(true);
    }

    public static class apiCall extends AsyncTask<Map<String, String>, Void, String> {
        private WeakReference<Context> mContext;
        String mBase;
        int mMethod;
        boolean mAppendEndpoint;
        boolean mIsJSON;
        APICallbackInterface mCallback;
        String mRequest;


        apiCall(Context context, String base, int method, boolean appendEndpoint, boolean isJSON, APICallbackInterface callback, String request) {
            super();
            mContext = new WeakReference<>(context);
            mBase = base;
            mMethod = method;
            mAppendEndpoint = appendEndpoint;
            mIsJSON = isJSON;
            mCallback = callback;
            mRequest = request;
        }

        @Override
        protected String doInBackground(Map<String, String>... params) {
            Map<String, String> nvp = params[0];
            String base = mBase;
            String endpoint = nvp.get(ENDPOINT_KEY);
            nvp.remove(ENDPOINT_KEY);

            String str_response = "";

            String url = base;
            if (mAppendEndpoint) url+= endpoint;

            if (mMethod == API.httpmethod.GET) {
                str_response = get(url, nvp);
            }
            if (mMethod == API.httpmethod.POST) {
                str_response = post(url, nvp);
            }
            return str_response;
        }

        private String get(String url, Map<String, String> nvp){

            if (nvp.size()>0){
                url+= "?";
                Iterator it = nvp.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry)it.next();
                    String key = (String)pair.getKey();
                    String val = (String)pair.getValue();
                    url+= key+"="+val;

                    it.remove(); // avoids a ConcurrentModificationException
                    if (it.hasNext()) url+="&";

                }
            }

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            Log.d(TAG, url);
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            String response = null;

            try {
                Response r = client.newCall(request).execute();
                response = r.body().string();
            } catch (IOException e){
                e.printStackTrace();
            }
            return response;
        }

        private String post(String url, Map<String, String> nvp){
            RequestBody body;
            MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);

            if (nvp.size()>0){
                Iterator it = nvp.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry)it.next();
                    String key = (String)pair.getKey();
                    String val = (String)pair.getValue();
                    builder.addFormDataPart(key, val);

                    it.remove(); // avoids a ConcurrentModificationException

                }
            }
            body = builder.build();

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            Log.d(TAG, url);
            Request request = new Request.Builder()
                    .post(body)
                    .url(url)
                    .build();

            String response = null;

            try {
                Response r = client.newCall(request).execute();
                response = r.body().string();
            } catch (IOException e){
                e.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            if (this.isCancelled()) return;

            Log.i(TAG, "RESPONSE WAS: " + response);

            if (mIsJSON) {
                JSONObject o = null;

                try {
                    o = new JSONObject(response);
                } catch (JSONException | NullPointerException e) {
                    e.printStackTrace();
                }

                if (o != null && o.has(STATUS_KEY)) {
                    String status = "";
                    try {
                        status = o.getString(STATUS_KEY);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (mCallback != null) {
                        if (status.equals(STATUS_OK)) {
                            mCallback.onAPISuccess(mRequest, o);
                        } else {
                            mCallback.onAPIError(mRequest, o);
                        }
                    }
                    return;
                }
            } else {
                String success = "";
                JSONObject o = new JSONObject();

                if (response != null && response.length()>0) {
                    success = response.substring(0, 1);
                    try {
                        o.put("data", response.substring(1).trim());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                if (success.equals("1")){
                    mCallback.onAPISuccess(mRequest, o);
                } else {
                    mCallback.onAPIError(mRequest, o);
                }
                return;
            }

            if (response == null){
                ((Activity)mContext.get()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(mContext.get())
                                .setTitle("Network Unavailable")
                                .setMessage("Please check that you are connected to either WiFi or a Mobile network")
                                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        if (mCallback != null)
                                            mCallback.onAPIError(mRequest, null);

                                    }
                                })
                                .create().show();
                    }
                });

            } else {
                if (mCallback != null)
                    mCallback.onAPIError(mRequest, null);
            }
        }
    }


}
