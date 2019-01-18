package com.orsteg.residingtab.sample

import android.app.Activity
import android.content.Context
import android.hardware.Camera
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.IOException

/**
 * Created by goodhope on 1/18/19.
 */

/**
 * This is just a sample camera implementation. Primarily designed to just show a correct preview
 */
class CameraPreview : SurfaceView, SurfaceHolder.Callback {

    var mCamera: Camera? = null
    var camId = -1
    var camInfo: Camera.CameraInfo? = null
    var isSurfaceCreated = false

    val TAG = "CameraPreview"

    private var mHolder: SurfaceHolder = holder.apply {
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        addCallback(this@CameraPreview)
        // deprecated setting, but required on Android versions prior to 3.0
        setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    constructor(context: Context): super(context, null)

    constructor(context: Context, attrs: AttributeSet?): super(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes)

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        refreshCamera()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {}

    override fun surfaceCreated(holder: SurfaceHolder?) {
        // The Surface has been created, now tell the camera where to draw the preview.
        startPreview(holder)
        isSurfaceCreated = true
    }

    private fun startPreview(holder: SurfaceHolder?) {
        mCamera?.apply {
            setCameraDisplayOrientation()
            parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            try {
                setPreviewDisplay(holder)
                startPreview()
            } catch (e: IOException) {
                Log.d(TAG, "Error setting camera preview: ${e.message}")
            }
        }
    }
    fun initCamera() {
        if (mCamera == null) {
            mCamera = getCameraInstance()
            if (isSurfaceCreated) {
                startPreview(mHolder)
            }
        }
    }

    fun destroyCamera() {
        mCamera?.stopPreview()
        mCamera?.release() // release the camera for other applications
        mCamera = null
    }

    private fun refreshCamera() {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        if (mHolder.surface == null) {
            // preview surface does not exist
            return
        }

        // stop preview before making changes
        try {
            mCamera?.stopPreview()
        } catch (e: Exception) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        mCamera?.apply {
            try {
                setPreviewDisplay(mHolder)
                startPreview()
            } catch (e: Exception) {
                Log.d(TAG, "Error starting camera preview: ${e.message}")
            }
        }

    }

    /** A safe way to get an instance of the Camera object. */
    private fun getCameraInstance(): Camera? {
        return try {
            findCamera().let { if (it != -1) Camera.open(findCamera()) else null } // attempt to get a Camera instance
        } catch (e: Exception) {
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable
        }
    }

    private fun setCameraDisplayOrientation() {
        val activity = context as Activity
        val rotation = activity.windowManager.defaultDisplay.rotation
        val degree = when (rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }

        var result: Int
        if (camInfo?.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = ((camInfo?.orientation?:0) + degree) % 360
            result = (360 - result) % 360
        } else {
            result = ((camInfo?.orientation?:0) - degree + 360) % 360
        }
        mCamera?.apply {
            setDisplayOrientation(result)
            parameters.set("orientation", result)
        }
    }

    private fun findCamera(face: Int = Camera.CameraInfo.CAMERA_FACING_BACK) : Int {
        for (i in 0 until Camera.getNumberOfCameras()) {
            val info = Camera.CameraInfo()
            Camera.getCameraInfo(i, info)

            if (info.facing == face) {
                camId = i
                camInfo = info
                return i
            }
        }
        return -1
    }

}