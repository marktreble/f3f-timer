package com.marktreble.f3ftimer.services.softbuzzer;

import android.util.Log;

import com.marktreble.f3ftimer.driver.SoftBuzzerService;

public class SoftBuzzerServiceInstance {
   public static SoftBuzzerServiceInstance softBuzzerServiceInstance = null;
   public SoftBuzzerService softBuzzerService = null;
   public Boolean isBound = false;

   public static SoftBuzzerServiceInstance getInstance()
   {
      if (softBuzzerServiceInstance  == null) {
         Log.d("BOUND", "NEW INSTANCE!");
         softBuzzerServiceInstance = new SoftBuzzerServiceInstance();
      }

      Log.d("BOUND", "GET INSTANCE: " + softBuzzerServiceInstance);
      return softBuzzerServiceInstance;
   }

   public static void setInstance(SoftBuzzerService instance)
   {
      Log.d("BOUND", "SET INSTANCE");
      softBuzzerServiceInstance = getInstance();
      softBuzzerServiceInstance.softBuzzerService = instance;
      softBuzzerServiceInstance.isBound = true;
   }
}
