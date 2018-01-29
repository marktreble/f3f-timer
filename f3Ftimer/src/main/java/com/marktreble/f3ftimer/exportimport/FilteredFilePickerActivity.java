package com.marktreble.f3ftimer.exportimport;

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
        FilePickerFragmentJsonCsv fp = new FilePickerFragmentJsonCsv();
        fp.setArguments(getIntent().getExtras());
        fp.setExtension();
        AbstractFilePickerFragment<File> fragment = fp;
        // startPath is allowed to be null. In that case, default folder should be SD-card and not "/"
        fragment.setArgs(startPath != null ? startPath : Environment.getExternalStorageDirectory().getPath(),
                mode, allowMultiple, allowCreateDir);
        return fragment;
    }
}
