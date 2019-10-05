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

package com.marktreble.f3ftimer.data.api;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

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

public class API {

    private static final String TAG = "APIDEBUG";

    private static final String STATUS_KEY = "status";
    private static final String STATUS_OK = "ok";

    public static final String ENDPOINT_KEY = "endpoint";

    public static final String API_IMPORT = "/import";
    public static final String API_IMPORT_RACE = "/import_race";
    // public static final String API_UPLOAD = "/upload_race";

    public static final String F3XV_IMPORT = "searchEvents";
    public static final String F3XV_IMPORT_RACE = "getEventInfo";
    // public static final String F3XV_UPLOAD = "";

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

    public void makeAPICall(Context context, String base, int method, Map<String, String> params) {
        api = new apiCall(context, base, method, mAppendEndpoint, mIsJSON, mCallback, request);
        api.execute(new JSONObject(params));
    }

    public void cancel() {
        api.cancel(true);
    }


    public static class apiCall extends AsyncTask<JSONObject, Void, String> {
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
        protected String doInBackground(JSONObject... params) {
            JSONObject nvp = params[0];
            String base = mBase;
            try {
                String endpoint = nvp.getString(ENDPOINT_KEY);

                nvp.remove(ENDPOINT_KEY);

                String str_response = "";

                String url = base;
                if (mAppendEndpoint) url += endpoint;

                if (mMethod == API.httpmethod.GET) {
                    str_response = get(url, nvp);
                }
                if (mMethod == API.httpmethod.POST) {
                    str_response = post(url, nvp);
                }
                return str_response;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return "";
        }

        private String get(String url, JSONObject nvp) {
            try {
                if (nvp.length() > 0) {
                    StringBuilder urlbuilder = new StringBuilder(url);
                    urlbuilder.append("?");
                    Iterator<String> keys = nvp.keys();

                    while (keys.hasNext()) {
                        String key = keys.next();
                        String val = nvp.getString(key);
                        urlbuilder.append(key);
                        urlbuilder.append("=");
                        urlbuilder.append(val);
                        urlbuilder.append("&");
                    }
                    url = urlbuilder.toString();
                    url = url.substring(0, url.length() - 1);
                }

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .cache(null)
                        .build();

                Log.d(TAG, url);
                Request request = new Request.Builder()
                        .url(url)
                        .build();

                String response = null;

                try {
                    Response r = client.newCall(request).execute();
                    if (r.body() != null) {
                        response = r.body().string();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return "";
        }

        private String post(String url, JSONObject nvp) {
            RequestBody body;
            MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);

            try {
                if (nvp.length() > 0) {
                    url += "?";
                    Iterator<String> keys = nvp.keys();

                    while (keys.hasNext()) {
                        String key = keys.next();
                        String val = nvp.getString(key);
                        builder.addFormDataPart(key, val);
                    }
                }

                body = builder.build();

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .cache(null)
                        .build();

                Log.d(TAG, url);
                Request request = new Request.Builder()
                        .post(body)
                        .url(url)
                        .build();

                String response = null;

                try {
                    Response r = client.newCall(request).execute();
                    if (r.body() != null) {
                        response = r.body().string();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return "";
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

                if (response != null && response.length() > 0) {
                    success = response.substring(0, 1);
                    try {
                        o.put("data", response.substring(1).trim());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                if (success.equals("1")) {
                    mCallback.onAPISuccess(mRequest, o);
                } else {
                    mCallback.onAPIError(mRequest, null);
                }
                return;
            }

            if (response == null) {
                ((Activity) mContext.get()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null)
                            mCallback.onAPIError(mRequest, null);
                    }
                });

            } else {
                if (mCallback != null)
                    mCallback.onAPIError(mRequest, null);
            }
        }
    }


}
