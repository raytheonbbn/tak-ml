package com.atakmap.android.takml_android.executorch_mx_plugin;

import static org.apache.commons.io.FileUtils.copyFile;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import org.pytorch.executorch.Module;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;

public final class ExecuTorchUtils {

    private static final String TAG = ExecuTorchUtils.class.getName();

    public static Module loadModelFromUriWithMmap(Context context, Uri uri) throws IOException {
        File file = File.createTempFile("model", ".pte", null);

        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(file)) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }

        } catch (IOException e) {
            Log.e("ModelLoader", "Failed to copy model from URI", e);
            throw e;
        }
        return Module.load(file.getPath(), Module.LOAD_MODE_MMAP);
    }
}
