package com.example.matteo.smiley;

/**
 * Created by Matteo on 17/08/2015.
 */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.View;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FloatingWindow implements View.OnTouchListener {
    private static final String TAG = "FloatingWindow";
    private static final boolean DEBUG = true;

    // TODO: this size works well on a Nexus 7... make this more generic later.
    private static final int  X_RED_FACTOR= 3;
    private static final int Y_RED_FACTOR = 4;
    public int default_x;
    public int default_y;


    private static final float MIN_SCALE_FACTOR = 0.75f;
    private static final float MAX_SCALE_FACTOR = 3.0f;

    private static final long SLEEP_DELAY_MILLIS = 5000;

    private static final int MSG_SNAP = 0;

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SNAP:
                    int fromX = msg.arg1, toX = msg.arg2;
                    snap(fromX, toX);
                    break;
            }
        }
    };

    private final Context mContext;
    private final WindowManager mWindowManager;
    private final WindowManager.LayoutParams mWindowParams;
    private final Point mDisplaySize = new Point();

    // True if the window is clinging to the left side of the screen; false
    // if the window is clinging to the right side of the screen.
    private boolean mIsOnLeft;

    private final View mRootView;
    private final CameraPreview mPreview;
    private final Camera mCamera;

    private final PointF mInitialDown = new PointF();
    private final Point mInitialPosition = new Point();

    private final GestureDetector mGestureDetector;
    private final ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.f;

    private ValueAnimator mSnapAnimator;
    private boolean mAnimating;

    public FloatingWindow(Context context) {
        mContext = context;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.getDefaultDisplay().getSize(mDisplaySize);
        default_x = mDisplaySize.x/X_RED_FACTOR;
        default_y = mDisplaySize.y/Y_RED_FACTOR;
        mWindowParams = createWindowParams(default_x, default_y);
        mIsOnLeft = true;

        mGestureDetector = new GestureDetector(context, new GestureListener());
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());

        // TODO: don't hardcode cameraId '0' here... figure this out later.
        mCamera = openFrontFacingCamera();
        CameraPreview.setCameraDisplayOrientation(context, 0, mCamera);

        mPreview = new CameraPreview(context);
        mPreview.setCamera(mCamera);
        mPreview.setOnTouchListener(this);

        mRootView = mPreview;
    }

    public void show() {
        mWindowManager.addView(mRootView, mWindowParams);
    }

    public void hide() {
        mCamera.release();
        mWindowManager.removeView(mRootView);
    }

    private static WindowManager.LayoutParams createWindowParams(int width, int height) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.width = width;
        params.height = height;
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        params.format = PixelFormat.TRANSLUCENT;
        params.gravity = Gravity.RIGHT | Gravity.CENTER;
        return params;
    }

    private void updateWindowPosition(int x, int y) {
        if (DEBUG) Log.i(TAG, "updateWindowPosition(" + x + ", " + y + ")");
        mWindowParams.x = x;
        mWindowParams.y = y;
        mWindowManager.updateViewLayout(mRootView, mWindowParams);
    }

    private void updateWindowSize(int width, int height) {
        if (DEBUG) Log.i(TAG, "updateWindowSize(" + width + ", " + height + ")");
        mWindowParams.width = width;
        mWindowParams.height = height;
        mWindowManager.updateViewLayout(mRootView, mWindowParams);
    }

    /** Called by the FloatingWindowService when a configuration change occurs. */
    public void onConfigurationChanged(Configuration newConfiguration) {
        // TODO: properly handle configuration changes (we probably will only have
        // to care about orientation changes).
        mWindowManager.getDefaultDisplay().getSize(mDisplaySize);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (mAnimating) {
            return true;
        }

        // Unschedule any pending animations.
        mHandler.removeMessages(MSG_SNAP);

        mScaleDetector.onTouchEvent(event);
        mGestureDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                mInitialPosition.set(mWindowParams.x, mWindowParams.y);
                mInitialDown.set(event.getRawX(), event.getRawY());
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                int screenWidth = mDisplaySize.x;
                int windowWidth = mRootView.getWidth();
                int oldX = mWindowParams.x;

                if (oldX + windowWidth / 2 < screenWidth / 2) {
                    snap(oldX, - 2 * windowWidth / 3);
                    mIsOnLeft = true;
                } else {
                    snap(oldX, screenWidth - windowWidth / 3);
                    mIsOnLeft = false;
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                int newX = mInitialPosition.x + (int) (event.getRawX() - mInitialDown.x);
                int newY = mInitialPosition.y + (int) (event.getRawY() - mInitialDown.y);
                updateWindowPosition(newX, newY);
                break;
            }
        }
        return true;
    }

    private void snap(final int fromX, final int toX) {
        if (DEBUG) Log.i(TAG, "snap(" + fromX + ", " + toX + ")");

        mSnapAnimator = ValueAnimator.ofFloat(0, 1);
        mSnapAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mRootView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                mAnimating = true;
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                mRootView.setLayerType(View.LAYER_TYPE_NONE, null);
                mAnimating = false;
            }
        });
        mSnapAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int currX = fromX + (int) (animation.getAnimatedFraction() * (toX - fromX));
                updateWindowPosition(currX, mWindowParams.y);
            }
        });
        mSnapAnimator.start();
    }

    private class GestureListener extends SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {

            mCamera.takePicture(shutterCallback, rawCallback,jpegCallback);
            //mCamera.startPreview();

            return true;

        }

        /*@Override
        public boolean onDoubleTap(MotionEvent event) {
            int fromX = mWindowParams.x;
            int toX = mIsOnLeft ? 0 : mDisplaySize.x - mRootView.getWidth();
            snap(fromX, toX);
            Message snapMsg = mHandler.obtainMessage(MSG_SNAP, toX, fromX);
            mHandler.sendMessageDelayed(snapMsg, SLEEP_DELAY_MILLIS);
            return true;
        }
        */

        @Override
        public void onLongPress(MotionEvent event){
            close();
        }

        // Handles when shutter open
        Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback()
        {
            public void onShutter()
            {
                //Just a call to get sound
            }
        };

        /** Handles data for raw picture */
        Camera.PictureCallback rawCallback = new Camera.PictureCallback()
        {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                if (data != null) {

                    File pictureFileDir = mContext.getCacheDir();
                    if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
                        return;
                    }
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
                    String date = dateFormat.format(new Date());
                    String photoFile = "PictureFront_" + "_" + date + ".jpg";
                    String filename = pictureFileDir.getPath() + File.separator + photoFile;
                    File mainPicture = new File(filename);

                    try {
                        FileOutputStream fos = new FileOutputStream(mainPicture);
                        fos.write(data);
                        fos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    camera.startPreview();
                }
            }
        };

        /** Handles data for jpeg picture */
        Camera.PictureCallback jpegCallback = new Camera.PictureCallback()
        {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                if (data != null) {

                    File sdCard = Environment.getExternalStorageDirectory();
                    File dir = new File (sdCard.getAbsolutePath() + "/com.Matteo.SmileY");


                    if (!dir.exists() && !dir.mkdirs()) {
                        return;
                    }
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
                    String date = dateFormat.format(new Date());
                    String photoFile = "Smiley_" + "_" + date + ".jpg";
                    String filename = dir.getPath() + File.separator + photoFile;
                    File mainPicture = new File(filename);

                    try {
                        FileOutputStream fos = new FileOutputStream(mainPicture);
                        fos.write(data);
                        fos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //galleryAddPic(filename);



                    SmileyCreator sc = new SmileyCreator(mContext.getApplicationContext(),mainPicture);
                    sc.elaborate();

                    //File smileY = sc.getSmileY();


                    camera.startPreview();
                }

            }

        };


    }
    private Camera openFrontFacingCamera() {
        int cameraCount = 0;
        int camIdx;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = Camera.open(camIdx);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        return cam;
    }


    private void galleryAddPic(String mCurrentPhotoPath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        mContext.sendBroadcast(mediaScanIntent);
    }



    private class ScaleListener extends SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            mScaleFactor = Math.max(MIN_SCALE_FACTOR, Math.min(mScaleFactor, MAX_SCALE_FACTOR));
            int newWidth = (int) (default_x * mScaleFactor);
            int newHeight = (int) (default_y * mScaleFactor);
            updateWindowSize(newWidth, newHeight);
            return true;
        }
    }

    public void close (){


        mCamera.release();
        mWindowManager.removeView(mRootView);


    }

}
