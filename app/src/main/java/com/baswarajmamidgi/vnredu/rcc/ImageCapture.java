/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baswarajmamidgi.vnredu.rcc;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.baswarajmamidgi.vnredu.rcc.camera.CameraSource;
import com.baswarajmamidgi.vnredu.rcc.camera.CameraSourcePreview;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

public final class ImageCapture extends AppCompatActivity {
    private static final String TAG = "Imagecapture";

    // Intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    // Permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    // Constants used to pass extra data in the intent
    public static final String AutoFocus = "AutoFocus";
    public static final String UseFlash = "UseFlash";
    public static final String TextBlockObject = "String";
    private static final int gallery=12;


    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;

    private static final int[] FLASH_OPTIONS = {3,0,1};

    private static final String[] FLASHTYPE={Camera.Parameters.FLASH_MODE_AUTO,Camera.Parameters.FLASH_MODE_OFF,Camera.Parameters.FLASH_MODE_ON};

    private static final int[] FLASH_ICONS = {
            R.drawable.ic_flash_auto,
            R.drawable.ic_flash_off,
            R.drawable.ic_flash_on,
    };
    private static final int[] FLASH_TITLES = {
            R.string.flash_auto,
            R.string.flash_off,
            R.string.flash_on,
    };
    private int mCurrentFlash;
    private ProgressDialog progressDialog;
    private byte[] inputData;





    // Helper objects for detecting taps and pinches.
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    /**
     * Initializes the UI and creates the detector pipeline.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.ocr_capture);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);

        // read parameters from the intent used to launch the activity.
        boolean autoFocus = true;
        boolean useFlash = false;

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(autoFocus, useFlash);
        } else {
            requestCameraPermission();
        }

        gestureDetector = new GestureDetector(this, new CaptureGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());



        FloatingActionButton capture= (FloatingActionButton) findViewById(R.id.capture);
        FloatingActionButton barcode= (FloatingActionButton) findViewById(R.id.uploadfile);
        barcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String type="*/*";

             Intent i=new Intent(Intent.ACTION_GET_CONTENT);
              i.setType(type);
             startActivityForResult(Intent.createChooser(i,"select file") ,gallery);
            }
        });
        final CameraSource.ShutterCallback shutterCallback=new CameraSource.ShutterCallback() {
            @Override
            public void onShutter() {
            Log.i("log","image captured");
            }
        };

        final CameraSource.PictureCallback picturecallback=new CameraSource.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data) {
                Log.d(TAG, "onPictureTaken " + data.length);

                File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                if (pictureFile == null){
                    Log.d(TAG, "Error creating media file, check storage permissions: ");
                    return;
                }

                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    Log.i("log","file saved");
                    fos.close();
                } catch (FileNotFoundException e) {
                    Log.d(TAG, "File not found: " + e.getMessage());
                } catch (IOException e) {
                    Log.d(TAG, "Error accessing file: " + e.getMessage());
                }

                if (data != null) {
                    Log.i("log","data send called");
                    Log.i("log",pictureFile.toString());

                    Intent intent = new Intent(ImageCapture.this,OcrActivity.class);
                    intent.putExtra("data", pictureFile.toString());
                    startActivity(intent);
                    Log.i("log","finish called");
                }
            }
        };
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraSource.takePicture(shutterCallback,picturecallback);
                progressDialog=new ProgressDialog(ImageCapture.this);
                progressDialog.setMessage("Processing...");
                //progressDialog.show();
            }
        });


    }





    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == gallery && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            InputStream iStream = null;
            try {
                iStream = getContentResolver().openInputStream(uri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                 inputData = getBytes(iStream);
            } catch (IOException e) {
                e.printStackTrace();
            }


            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(inputData);
                Log.i("log", "file saved");
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }

            if (data != null) {
                Log.i("log", "data send called");
                Log.i("log", pictureFile.toString());

                Intent intent = new Intent(ImageCapture.this, OcrActivity.class);
                intent.putExtra("data", pictureFile.toString());
                startActivity(intent);
               


            }
        }
    }
    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.switch_flash:
                if (mCameraSource != null) {
                    mCurrentFlash = (mCurrentFlash + 1) % FLASH_OPTIONS.length;
                    item.setTitle(FLASH_TITLES[mCurrentFlash]);
                    item.setIcon(FLASH_ICONS[mCurrentFlash]);
                    mCameraSource.setFlashMode(FLASHTYPE[mCurrentFlash]);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }




    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean b = scaleGestureDetector.onTouchEvent(e);

        boolean c = gestureDetector.onTouchEvent(e);

        return b || c || super.onTouchEvent(e);
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the ocr detector to detect small text samples
     * at long distances.
     *
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash) {
        Context context = getApplicationContext();

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the text recognizer to detect small pieces of text.
        mCameraSource =
                new CameraSource.Builder(getApplicationContext())
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1280, 1024)
                .setRequestedFps(2.0f)
                .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                .setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null)
                .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if(progressDialog!=null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();

        if (mPreview != null) {
            mPreview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mPreview != null) {
            mPreview.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // We have permission, so create the camerasource
            boolean autoFocus = getIntent().getBooleanExtra(AutoFocus,false);
            boolean useFlash = getIntent().getBooleanExtra(UseFlash, false);
            createCameraSource(autoFocus, useFlash);
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("RCC")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        // Check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }



    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "rcc");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("rcc", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMAGE" + ".jpg");
        }  else {
            return null;
        }

        return mediaFile;
    }






    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return super.onSingleTapConfirmed(e);
        }
    }

    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

        /**
         * Responds to scaling events for a gesture in progress.
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should consider this event
         * as handled. If an event was not handled, the detector
         * will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example,
         * only wants to update scaling factors if the change is
         * greater than 0.01.
         */
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        /**
         * Responds to the beginning of a scaling gesture. Reported by
         * new pointers going down.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should continue recognizing
         * this gesture. For example, if a gesture is beginning
         * with a focal point outside of a region where it makes
         * sense, onScaleBegin() may return false to ignore the
         * rest of the gesture.
         */
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        /**
         * Responds to the end of a scale gesture. Reported by existing
         * pointers going up.
         * <p/>
         * Once a scale has ended, {@link ScaleGestureDetector#getFocusX()}
         * and {@link ScaleGestureDetector#getFocusY()} will return focal point
         * of the pointers remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         */
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mCameraSource.doZoom(detector.getScaleFactor());
        }
    }
}
