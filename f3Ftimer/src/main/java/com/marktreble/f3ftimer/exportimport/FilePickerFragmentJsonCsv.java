package com.marktreble.f3ftimer.exportimport;

import android.support.annotation.NonNull;

import com.nononsenseapps.filepicker.FilePickerFragment;

import java.io.File;

public class FilePickerFragmentJsonCsv extends FilePickerFragment {

    public static final String EXTENSION = "extension";
    private String mExtension;
    
    public void setArguments(android.os.Bundle args) {
        super.setArguments(args);
        mExtension = getArguments().getString(EXTENSION, null);
    }
    
    /**
     *
     * @param file
     * @return The file extension. If file has no extension, it returns null.
     */
    private String getExtension(@NonNull File file) {
        String path = file.getPath();
        int i = path.lastIndexOf(".");
        if (i < 0) {
            return null;
        } else {
            return path.substring(i);
        }
    }

    @Override
    protected boolean isItemVisible(final File file) {
        boolean ret = super.isItemVisible(file);
        if (ret && !isDir(file) && (mode == MODE_FILE || mode == MODE_FILE_AND_DIR) && mExtension != null) {
            String ext = getExtension(file);
            return (ext != null && ext.matches(mExtension));
        }
        return ret;
    }

}