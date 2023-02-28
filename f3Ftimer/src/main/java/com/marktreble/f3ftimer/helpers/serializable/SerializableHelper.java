package com.marktreble.f3ftimer.helpers.serializable;

import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;

import java.io.Serializable;

public class SerializableHelper {
    /**
     * Compatibility wrapper for retrieving parcelable from Bundle
     *
     * This provides a normalised function across ALL API levels.
     *
     * @param bundle any bundle
     * @param key key to retrieve value
     * @return ResultReceiver
     */
    @SuppressWarnings("deprecation")
    public static Serializable getSerializable(Bundle bundle, String key) {
        Serializable value;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            value = bundle.getSerializable(key, Serializable.class);
        } else {
            value = bundle.getSerializable(key);
        }
        return value;
    }
}
