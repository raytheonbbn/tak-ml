
package com.atakmap.android.takml.plugin;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.atakmap.android.takml.receivers.PluginTemplateDropDownReceiver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraActivity extends Activity {
    private static final int CAMERA_REQUEST = 8888;
    private static final String CAMERA_INFO = "com.atakmap.android.helloworld.PHOTO";
    private static final String TAG = CameraActivity.class.getSimpleName();
    private byte[] byteArray;
    public static CallBack callBack;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                startCameraIntent();
            }
            else{
                ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
            }
        }else{
            startCameraIntent();
        }
    }

    private void startCameraIntent(){
        Intent cameraIntent = new Intent(
                android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        Log.v(TAG, "onActivityResult");

        try {
            if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
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

            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to write image", e);
        }

        finish();

        //AtakBroadcast.getInstance().sendBroadcast(intent);
        //sendBroadcast(intent);
    }

    public static void setCallBack(CallBack callBack1){
        callBack = callBack1;
    }

    public interface CallBack{
        void callback(Bitmap bitmap);
    }

}
