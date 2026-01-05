package com.atakmap.android.takml_android.util;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.core.content.FileProvider;

import java.io.*;
import java.util.UUID;

public class SecureFileTransfer {
    private static final String TAG = "SecureFileTransfer";
    private static final String SUB_DIR = "takml";
    private final Context context;

    public SecureFileTransfer(Context context) {
        this.context = context;
    }

    /**
     * Writes JSON to a temporary file and returns a ParcelFileDescriptor
     * that can be passed via AIDL for secure inter-process communication
     */
    public ParcelFileDescriptor writeJsonToTempFile(String jsonData) {
        File tempFile = createTempFile();
        if (tempFile == null) return null;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write(jsonData);
            return ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (IOException e) {
            Log.e(TAG, "Error writing to temp file", e);
            tempFile.delete(); // Clean up if failed
            return null;
        }
    }

    /**
     * Reads JSON from a ParcelFileDescriptor (received via AIDL)
     */
    public String readJsonFromPfd(ParcelFileDescriptor pfd) {
        if (pfd == null){
            Log.e(TAG, "readJsonFromPfd: was null");
            return null;
        }

        try (FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            Log.e(TAG, "Error reading from ParcelFileDescriptor", e);
            return null;
        } finally {
            try {
                pfd.close();
            } catch (IOException e) {
                Log.w(TAG, "Error closing ParcelFileDescriptor", e);
            }
        }
    }

    /**
     * Creates a secure temporary file in the app's internal storage
     */
    private File createTempFile() {
        File storageDir = new File(context.getFilesDir(), SUB_DIR);
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Log.e(TAG, "Failed to create temp directory");
            return null;
        }

        try {
            return File.createTempFile(
                    "tmp_" + UUID.randomUUID().toString(),
                    ".json",
                    storageDir
            );
        } catch (IOException e) {
            Log.e(TAG, "Failed to create temp file", e);
            return null;
        }
    }

    /**
     * Cleans up all temporary files
     */
    public void cleanupTempFiles() {
        File storageDir = new File(context.getFilesDir(), SUB_DIR);
        if (storageDir.exists() && storageDir.isDirectory()) {
            File[] files = storageDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().startsWith("tmp_") && !file.delete()) {
                        Log.w(TAG, "Failed to delete temp file: " + file.getName());
                    }
                }
            }
        }
    }
}