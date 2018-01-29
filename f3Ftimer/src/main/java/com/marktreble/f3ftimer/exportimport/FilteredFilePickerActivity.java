package com.marktreble.f3ftimer.exportimport;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;

import com.nononsenseapps.filepicker.AbstractFilePickerActivity;
import com.nononsenseapps.filepicker.AbstractFilePickerFragment;

import java.io.File;

/**
 * Created by hes on 10.09.2017.
 */

public class FilteredFilePickerActivity extends AbstractFilePickerActivity<File> {

    public FilteredFilePickerActivity() {
        super();
    }

    @Override
    protected AbstractFilePickerFragment<File> getFragment(@Nullable final String startPath, final int mode, final boolean allowMultiple,
                                                           final boolean allowCreateDir) {
        AbstractFilePickerFragment<File> fragment = new FilePickerFragmentJsonCsv();
        // startPath is allowed to be null. In that case, default folder should be SD-card and not "/"
        fragment.setArgs(startPath != null ? startPath : Environment.getExternalStorageDirectory().getPath(),
                mode, allowMultiple, allowCreateDir);
        Bundle extras = getIntent().getExtras();
        Bundle args = fragment.getArguments();
        if (null != extras) {
            String extarg = extras.getString(FilePickerFragmentJsonCsv.EXTENSION);
            if (null != extarg) {
                args.putString(FilePickerFragmentJsonCsv.EXTENSION, extarg);
                fragment.setArguments(args);
            }
        }
        return fragment;
    }
}
