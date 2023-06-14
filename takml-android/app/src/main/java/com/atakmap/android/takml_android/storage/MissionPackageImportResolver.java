package com.atakmap.android.takml_android.storage;

import android.content.Intent;
import android.util.Log;

import com.atakmap.android.importfiles.sort.ImportResolver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.takml_android.Constants;
import com.atakmap.android.takml_android.Takml;
import com.atakmap.android.takml_android.TakmlReceiver;

import java.io.File;
import java.util.Set;

public class MissionPackageImportResolver extends ImportResolver {
    private static final String TAG = MissionPackageImportResolver.class.getName();

    private final Takml takml;


    public MissionPackageImportResolver(String ext, String folderName, boolean validateExt,
                                        boolean copyFile, Takml takml) {
        super(ext, folderName, validateExt, copyFile);
        this.takml = takml;
    }


    @Override
    public boolean beginImport(File file, Set<SortFlags> flags) {
        return beginImport(file);
    }

    @Override
    public boolean beginImport(File file) {
        Intent intent = new Intent();
        intent.setAction(TakmlReceiver.IMPORT_TAKML_MODEL);
        intent.putExtra(Constants.TAK_ML_UUID, takml.getUuid().toString());
        intent.putExtra(Constants.TAKML_MODEL_PATH, file.getPath());
        AtakBroadcast.getInstance().sendBroadcast(intent);
        return true;
    }



    @Override
    public File getDestinationPath(File file) {
        Log.d(TAG, "getDestinationPath " + file.toString());
        if (file.toString().endsWith(Constants.TAKML_CONFIG_FILE)) {
            return file;
        }
        return null;
    }

    @Override
    public String getDisplayableName() {
        return null;
    }
}
