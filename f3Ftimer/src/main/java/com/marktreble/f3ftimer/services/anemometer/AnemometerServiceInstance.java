package com.marktreble.f3ftimer.services.anemometer;

import android.util.Log;

public final class AnemometerServiceInstance {
    public static AnemometerServiceInstance anemometerServiceInstance = null;
    public AnemometerService anemometerService = null;
    public Boolean isBound = false;

    public static AnemometerServiceInstance getInstance()
    {
        if (anemometerServiceInstance  == null) {
            Log.d("BOUND", "NEW INSTANCE!");
            anemometerServiceInstance = new AnemometerServiceInstance();
        }

        Log.d("BOUND", "GET INSTANCE: " + anemometerServiceInstance);
        return anemometerServiceInstance;
    }

    public static void setInstance(AnemometerService instance)
    {
        Log.d("BOUND", "SET INSTANCE");
        anemometerServiceInstance = getInstance();
        anemometerServiceInstance.anemometerService = instance;
        anemometerServiceInstance.isBound = true;
    }
}
