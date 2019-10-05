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
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileExport {

    final private static String TAG = "FileExport";

    void writeExportFile(Context context, String output, String filename) {
        writeExportFile(context, output, filename, "");
    }

    public void writeExportFile(Context context, String output, String filename, String path) {
        File file;
        if (path.equals("")) {
            file = this.getDataStorageDir(filename);
        } else {
            file = new File(path + String.format("/%s", filename));
        }
        File dir = new File(file.getAbsolutePath()).getParentFile();

        if (!dir.exists() || !dir.isDirectory()) {
            if (!dir.mkdirs()) {
                Log.d(TAG, "DIR CREATE FAILED: " + dir.getPath());
                return;
            }
        }
        Log.d(TAG, "WRITING FILE TO: " + file.getPath());
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (file.exists()) {

            FileOutputStream stream;
            try {
                stream = new FileOutputStream(file);
                try {
                    stream.write(output.getBytes());

                    try {
                        stream.flush();
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        stream.flush();
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state));
    }

    public File getDataStorageDir(String name) {
        // Get the directory for the user's public pictures directory.
        File base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        base = new File(base, "F3F");
        if (!base.mkdirs()) {
            Log.d(TAG, "DIR CREATED");
        }
        return new File(base.getAbsolutePath() + String.format("/%s", this.sanitise(name)));
    }

    private String sanitise(String name) {
        return name.replaceAll("[^a-zA-Z0-9.]", "-");
    }

}
