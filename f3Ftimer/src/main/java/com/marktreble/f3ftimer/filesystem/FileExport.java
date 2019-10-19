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

package com.marktreble.f3ftimer.filesystem;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FileExport {

    final private static String TAG = "FileExport";

    /**
     * Writes the text in `output` to the file `filename`
     *
     * @param context Context
     * @param output String
     * @param filename String
     */
    void writeExportFile(Context context, String output, String filename) {

        File file = this.getDataStorageDir(context, filename);

        OutputStream stream = null;
        try {
            stream = new FileOutputStream(file);
            stream.write(output.getBytes());
            stream.flush();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.flush();
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Writes the text in `output` to the file `filename`
     *
     * @param context Context
     * @param output String
     * @param uri Uri
     */
    void writeExportFile(Context context, String output, Uri uri) {

        FileOutputStream stream = null;
        try {
            ParcelFileDescriptor pfd = context.getContentResolver().
                    openFileDescriptor(uri, "w");
            stream = new FileOutputStream(pfd.getFileDescriptor());
            stream.write(output.getBytes());
            stream.flush();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.flush();
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Return true/false if external storage is writable
     *
     * @return boolean
     */
    boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state));
    }

    /**
     * Get storage directory. By default this creates and returns /F3F directory within Documents
     *
     * @param context Context
     * @param name String
     * @return File
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected File getDataStorageDir(Context context, String name) {
        // Get the directory for the user's public pictures directory.
        File base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        base = new File(base, "F3F");
        base.mkdirs();
        return new File(base.getAbsolutePath() + String.format("/%s", sanitise(name)));
    }

    /**
     * Replace all ranges of non alphanumeric characters with a single hyphen
     *
     * @param name String
     * @return String
     */
    String sanitise(String name) {
        return name.replaceAll("[^a-zA-Z0-9.]", "-");
    }

}
