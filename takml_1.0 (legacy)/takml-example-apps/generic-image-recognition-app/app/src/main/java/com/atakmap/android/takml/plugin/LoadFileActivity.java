
package com.atakmap.android.takml.plugin;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class LoadFileActivity extends Activity {
    private static final int LOAD_FILE_REQUEST = 8888;
    private static final String TAG = LoadFileActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 0);

        } else {
            startLoadFileIntent();
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int i = 0 ; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, 0);
                return;
            }
        }
    }

    private void startLoadFileIntent() {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");

        // Do this if you need to be able to open the returned URI as a stream
        // (for example here to read the image data).
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        startActivityForResult(intent, LOAD_FILE_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        Log.d(TAG, "onActivityResult");

        try {
            if (requestCode == LOAD_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "Got result of file request.");

                Bitmap photo = BitmapFactory.decodeStream(getContentResolver().openInputStream(data.getData()));

                Log.d(TAG, "onActivityResult: !!! " + photo.toString());

                File sdCardDirectory = Environment.getExternalStorageDirectory();
                File image = new File(sdCardDirectory, "test.png");

                boolean success = false;

                // Encode the file as a PNG image.
                FileOutputStream outStream;
                try {

                    outStream = new FileOutputStream(image);
                    photo.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                    /* 100 to keep full quality of the image */

                    outStream.flush();
                    outStream.close();
                    success = true;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                Log.d(TAG, "Failed to get image: " + requestCode + ", " + resultCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to write image", e);
        }

        finish();
    }

}
