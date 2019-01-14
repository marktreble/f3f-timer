package com.marktreble.f3ftimer.filesystem;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by marktreble on 09/01/2018.
 */

public class FileExport {

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
        Log.d("EXPORT", "WRITING FILE TO: " + file.getPath());

        if (file != null) {
            FileOutputStream stream = null;
            try {
                stream = new FileOutputStream(file);
                try {
                    stream.write(output.getBytes());
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

    boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public File getDataStorageDir(String name) {
        // Get the directory for the user's public pictures directory.
        File base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        base = new File(base, "F3F");
        base.mkdirs();
        File file = new File(base.getAbsolutePath() + String.format("/%s", this.sanitise(name)));

        return file;
    }

    private String sanitise(String name) {
        return name.replaceAll("[^a-zA-Z0-9\\.]", "-");
    }

}
