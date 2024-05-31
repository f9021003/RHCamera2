package com.renhui.rhcamera2

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView.SurfaceTextureListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Collections

class MainActivity : AppCompatActivity() {





    private var imageReaderList =  mutableListOf<ImageReader>()
    private val cameraDeviceList =  mutableListOf<CameraDevice>()
    private val captureSessionList = mutableListOf<CameraCaptureSession>()
//    private var mPreviewRequest: CaptureRequest? = null
    //private var mPreviewRequestBuilder: CaptureRequest.Builder? = null
    private var mTextureView: AutoFitTextureView? = null
    private var mTextureView2: AutoFitTextureView? = null


    inner class MySurfaceTextureListener(private val textureView: AutoFitTextureView, private val isFrontCamera: Boolean): SurfaceTextureListener {
        private lateinit var previewSize: Size
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            val textureView = textureView
            val (cameraId, retPreviewSize) = setupCamera(width, height, isFrontCamera, textureView)
            previewSize = retPreviewSize
            extracted(width, height, textureView, previewSize, cameraId)
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height, textureView, previewSize)
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }
//
//    // Surface状态回调
//    private var textureListener: SurfaceTextureListener = object : SurfaceTextureListener {
//        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
//            val textureView = mTextureView!!
//            val (cameraId, previewSize) = setupCamera(width, height, true, textureView)
//            mPreviewSize = previewSize
//            extracted(width, height, textureView, previewSize, cameraId)
//        }
//
//        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
//            configureTransform(width, height, mTextureView!!, mPreviewSize)
//        }
//
//        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
//            return false
//        }
//
//        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
//    }
//    private var textureListener2: SurfaceTextureListener = object : SurfaceTextureListener {
//        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
//            val textureView = mTextureView2!!
//            val (cameraId, previewSize) = setupCamera(width, height, false, textureView)
//            mPreviewSize = previewSize
//            extracted(width, height, textureView, previewSize, cameraId)
//        }
//
//        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
//            configureTransform(width, height, mTextureView2!!, mPreviewSize)
//        }
//
//        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
//            return false
//        }
//
//        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
//    }
    private fun extracted(
        width: Int,
        height: Int,
        textureView: AutoFitTextureView,
        previewSize: Size,
        cameraId: String
    ) {
        configureTransform(width, height, textureView, previewSize)
        openCamera(cameraId, object : CameraDevice.StateCallback() {

            private fun startPreview(camera: CameraDevice) {
                //etupImageReader()
                //前三个参数分别是需要的尺寸和格式，最后一个参数代表每次最多获取几帧数据
                val imageReader = ImageReader.newInstance(
                    previewSize.width,
                    previewSize.height,
                    ImageFormat.YUV_420_888,
                    1
                ).also {
                    //监听ImageReader的事件，当有图像流数据可用时会回调onImageAvailable方法，它的参数就是预览帧数据，可以对这帧数据进行处理
                    it.setOnImageAvailableListener(OnImageAvailableListener { reader ->
                        Log.i(TAG, "Image Available! cameraId:$cameraId")
                        val image = reader.acquireLatestImage()
                        // 开启线程异步保存图片
                        //Thread(ImageSaver(image)).start()
                        image.close()

                    }, null)
                }

                imageReaderList.add(imageReader)
                val surfaceTexture = textureView.surfaceTexture
                //设置TextureView的缓冲区大小
                surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
                //获取Surface显示预览数据
                val previewSurface = Surface(surfaceTexture)
                try {

                    val previewRequestBuilder =
                        camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

                    //设置预览的显示界面
                    previewRequestBuilder.addTarget(previewSurface)
                    previewRequestBuilder.addTarget(imageReader.surface)
    //            mPreviewRequestBuilder!!.addTarget(mImageReader!!.surface)
    //            mPreviewRequestBuilder!!.addTarget(mImageReader2!!.surface)
    //                        val surfaceListTemp = mImageReaderList.map { it.surface }.toMutableList()
    //                        surfaceListTemp.add(previewSurface)
    //                        surfaceListTemp.forEach {
    //                            previewRequestBuilder.addTarget(it)
    //                        }

    //                        val meteringRectangles = previewRequestBuilder.get(CaptureRequest.CONTROL_AF_REGIONS)
    //                        if (meteringRectangles.isNotEmpty()) {
    //                            Log.d(
    //                                TAG,
    //                                "PreviewRequestBuilder: AF_REGIONS=" + meteringRectangles[0].rect.toString()
    //                            )
    //                        }
                    previewRequestBuilder.set(
                        CaptureRequest.CONTROL_MODE,
                        CaptureRequest.CONTROL_MODE_AUTO
                    )
                    previewRequestBuilder.set(
                        CaptureRequest.CONTROL_AF_TRIGGER,
                        CaptureRequest.CONTROL_AF_TRIGGER_IDLE
                    )
                    //创建相机捕获会话，第一个参数是捕获数据的输出Surface列表，第二个参数是CameraCaptureSession的状态回调接口，当它创建好后会回调onConfigured方法，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行

                    //val surfaceList = mImageReaderList.map { it.surface }.toMutableList()
                    //surfaceList.add(previewSurface)
                    camera.createCaptureSession(
                        listOf(previewSurface, imageReader.surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                captureSessionList.add(session)
                                //repeatPreview()
                                previewRequestBuilder.setTag(TAG_PREVIEW)
                                val previewRequest = previewRequestBuilder.build()
                                //设置反复捕获数据的请求，这样预览界面就会一直有数据显示
                                try {
                                    session.setRepeatingRequest(
                                        previewRequest,
                                        mPreviewCaptureCallback,
                                        null
                                    )
                                } catch (e: CameraAccessException) {
                                    e.printStackTrace()
                                }
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                Log.d(TAG, "onConfigureFailed")

                            }
                        },
                        null
                    )
                } catch (e: CameraAccessException) {
                    e.printStackTrace()
                }
            }


            override fun onOpened(camera: CameraDevice) {
                cameraDeviceList.add(camera)
                //开启预览
                startPreview(camera)
            }

            override fun onDisconnected(camera: CameraDevice) {
                Log.i(TAG, "CameraDevice Disconnected")
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Log.e(TAG, "CameraDevice Error")
            }
        })
    }


    // 摄像头状态回调
//    private val stateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
//        override fun onOpened(camera: CameraDevice) {
//            mCameraDevice = camera
//            //开启预览
//            startPreview()
//        }
//
//        override fun onDisconnected(camera: CameraDevice) {
//            Log.i(TAG, "CameraDevice Disconnected")
//        }
//
//        override fun onError(camera: CameraDevice, error: Int) {
//            Log.e(TAG, "CameraDevice Error")
//        }
//    }
    private val mPreviewCaptureCallback: CaptureCallback = object : CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //front_camera
        mTextureView = findViewById<AutoFitTextureView?>(R.id.textureView).apply {
            surfaceTextureListener = MySurfaceTextureListener(this, true)
        }
        //back_camera
        mTextureView2 = findViewById<AutoFitTextureView?>(R.id.textureView2).apply {
            surfaceTextureListener = MySurfaceTextureListener(this, false)
        }
        //findViewById<View>(R.id.takePicture).setOnClickListener(View.OnClickListener { capture() })
    }

    override fun onPause() {
        closeCamera()
        super.onPause()
    }

    private fun setupCamera(width: Int, height: Int, isFront: Boolean, textureView: AutoFitTextureView ): Pair<String, Size> {

        // 获取摄像头的管理者CameraManager
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager

        // 遍历所有摄像头
        val returnCameraId= manager.cameraIdList.find { cameraId->
            val characteristics = manager.getCameraCharacteristics(cameraId)
            if (isFront) {
                characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            } else {
                characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
        }

        val characteristics = manager.getCameraCharacteristics(returnCameraId!!)
        // 获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
        val returnPreviewSize =
            getOptimalSize(map.getOutputSizes(SurfaceTexture::class.java), width, height)
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            textureView.setAspectRatio(returnPreviewSize.width, returnPreviewSize.height)
        } else {
            textureView.setAspectRatio(returnPreviewSize.height, returnPreviewSize.width)
        }

        /*
        for (cameraId in manager.cameraIdList) {
            val characteristics = manager.getCameraCharacteristics(cameraId)
            // 默认打开后置摄像头 - 忽略前置摄像头
            if (isFront) {
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) continue
            } else {
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) continue
            }
            // 获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            returnPreviewSize =
                getOptimalSize(map.getOutputSizes(SurfaceTexture::class.java), width, height)
            val orientation = resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView!!.setAspectRatio(mPreviewSize!!.width, mPreviewSize!!.height)
            } else {
                textureView!!.setAspectRatio(mPreviewSize!!.height, mPreviewSize!!.width)
            }
            //mCameraId = cameraId
            returnCameraId = cameraId
            break
        }*/
        return Pair(returnCameraId, returnPreviewSize)
    }


    private fun openCamera(cameraId: String, stateCallback: CameraDevice.StateCallback) {
        //获取摄像头的管理者CameraManager
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        //检查权限
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            //打开相机，第一个参数指示打开哪个摄像头，第二个参数stateCallback为相机的状态回调接口，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
            manager.openCamera(cameraId, stateCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun closeCamera() {
        captureSessionList.forEach {
            it.close()
        }
        captureSessionList.clear()
        cameraDeviceList.forEach {
            it.close()
        }
        cameraDeviceList.clear()

        imageReaderList.forEach {
            it.close()
        }
        imageReaderList.clear()

    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int, textureView: AutoFitTextureView, previewSize: Size) {
        if (null == textureView || null == previewSize) {
            return
        }
        val rotation = windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect =
            RectF(0f, 0f, previewSize!!.height.toFloat(), previewSize!!.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = Math.max(
                viewHeight.toFloat() / previewSize!!.height,
                viewWidth.toFloat() / previewSize!!.width
            )
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        textureView!!.setTransform(matrix)
    }
/*

    private fun startPreview() {
        setupImageReader()
        val mSurfaceTexture = textureView!!.surfaceTexture
        //设置TextureView的缓冲区大小
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)
        //获取Surface显示预览数据
        mPreviewSurface = Surface(mSurfaceTexture)
        try {
            previewRequestBuilder
            //创建相机捕获会话，第一个参数是捕获数据的输出Surface列表，第二个参数是CameraCaptureSession的状态回调接口，当它创建好后会回调onConfigured方法，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行

            val surfaceList = mImageReaderList.map { it.surface }.toMutableList()
            surfaceList.add(mPreviewSurface)
            mCameraDevice!!.createCaptureSession(
                surfaceList, object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        mCaptureSession = session
                        repeatPreview()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.d(TAG,"onConfigureFailed")

                    }
                }, null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }
*/
/*

    private fun repeatPreview() {
        mPreviewRequestBuilder!!.setTag(TAG_PREVIEW)
        mPreviewRequest = mPreviewRequestBuilder!!.build()
        //设置反复捕获数据的请求，这样预览界面就会一直有数据显示
        try {
            mCaptureSession!!.setRepeatingRequest(mPreviewRequest, mPreviewCaptureCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }
*/

    /*private fun setupImageReader() {
        //前三个参数分别是需要的尺寸和格式，最后一个参数代表每次最多获取几帧数据
        mImageReader = ImageReader.newInstance(
            mPreviewSize!!.width,
            mPreviewSize!!.height,
            ImageFormat.YUV_420_888,
            1
        ).also {
            //监听ImageReader的事件，当有图像流数据可用时会回调onImageAvailable方法，它的参数就是预览帧数据，可以对这帧数据进行处理
            it.setOnImageAvailableListener(OnImageAvailableListener { reader ->
                Log.i(TAG, "Image Available!1")
                val image = reader.acquireLatestImage()
                // 开启线程异步保存图片
                //Thread(ImageSaver(image)).start()
                image.close()

            }, null)
        }
        mImageReader2 = ImageReader.newInstance(
            mPreviewSize!!.width,
            mPreviewSize!!.height,
            ImageFormat.YUV_420_888,
            1
        ).also {
            //监听ImageReader的事件，当有图像流数据可用时会回调onImageAvailable方法，它的参数就是预览帧数据，可以对这帧数据进行处理
            it.setOnImageAvailableListener(OnImageAvailableListener { reader ->
                Log.i(TAG, "Image Available!2")
                val image = reader.acquireLatestImage()
                // 开启线程异步保存图片
                //Thread(ImageSaver(image)).start()
                image.close()
            }, null)

        }

        for(i in 0..0) {
            val mImageReader3 = ImageReader.newInstance(
                mPreviewSize!!.width,
                mPreviewSize!!.height,
                ImageFormat.YUV_420_888,
                1
            ).also {
                //监听ImageReader的事件，当有图像流数据可用时会回调onImageAvailable方法，它的参数就是预览帧数据，可以对这帧数据进行处理
                it.setOnImageAvailableListener(OnImageAvailableListener { reader ->
                    Log.i(TAG, "Image Available! $i")
                    val image = reader.acquireLatestImage()
                    // 开启线程异步保存图片
                    //Thread(ImageSaver(image)).start()
                    image.close()
                }, null)

            }
            mImageReaderList.add(mImageReader3)

        }

    }*/

    // 选择sizeMap中大于并且最接近width和height的size
    private fun getOptimalSize(sizeMap: Array<Size>, width: Int, height: Int): Size {
        val sizeList: MutableList<Size> = ArrayList()
        for (option in sizeMap) {
            if (width > height) {
                if (option.width > width && option.height > height) {
                    sizeList.add(option)
                }
            } else {
                if (option.width > height && option.height > width) {
                    sizeList.add(option)
                }
            }
        }
        return if (sizeList.size > 0) {
            Collections.min(
                sizeList,
                java.util.Comparator { lhs, rhs -> java.lang.Long.signum((lhs.width * lhs.height - rhs.width * rhs.height).toLong()) })
        } else sizeMap[0]
    }//设置预览的显示界面

    // 创建预览请求的Builder（TEMPLATE_PREVIEW表示预览请求）
//    private val previewRequestBuilder: Unit
//        private get() {
//            try {
//                mPreviewRequestBuilder =
//                    mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
//            } catch (e: CameraAccessException) {
//                e.printStackTrace()
//            }
//            //设置预览的显示界面
////            mPreviewRequestBuilder!!.addTarget(mPreviewSurface)
////            mPreviewRequestBuilder!!.addTarget(mImageReader!!.surface)
////            mPreviewRequestBuilder!!.addTarget(mImageReader2!!.surface)
//            val surfaceList = mImageReaderList.map { it.surface }.toMutableList()
//            surfaceList.add(mPreviewSurface)
//            surfaceList.forEach {
//                mPreviewRequestBuilder!!.addTarget(it)
//            }
//
//
//            val meteringRectangles = mPreviewRequestBuilder!!.get(CaptureRequest.CONTROL_AF_REGIONS)
//            if (meteringRectangles != null && meteringRectangles.size > 0) {
//                Log.d(
//                    TAG,
//                    "PreviewRequestBuilder: AF_REGIONS=" + meteringRectangles[0].rect.toString()
//                )
//            }
//            mPreviewRequestBuilder!!.set(
//                CaptureRequest.CONTROL_MODE,
//                CaptureRequest.CONTROL_MODE_AUTO
//            )
//            mPreviewRequestBuilder!!.set(
//                CaptureRequest.CONTROL_AF_TRIGGER,
//                CaptureRequest.CONTROL_AF_TRIGGER_IDLE
//            )
//        }

//    // 拍照
//    private fun capture() {
//        try {
//            //首先我们创建请求拍照的CaptureRequest
//            val mCaptureBuilder =
//                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
//            //获取屏幕方向
//            val rotation = windowManager.defaultDisplay.rotation
//            mCaptureBuilder.addTarget(mPreviewSurface)
//            mImageReaderList.forEach {
//                mCaptureBuilder.addTarget(it.surface)
//            }
////            mCaptureBuilder.addTarget(mImageReader!!.surface)
//
//            //设置拍照方向
//            mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION[rotation])
//
//            //停止预览
//            mCaptureSession!!.stopRepeating()
//
//            //开始拍照，然后回调上面的接口重启预览，因为mCaptureBuilder设置ImageReader作为target，所以会自动回调ImageReader的onImageAvailable()方法保存图片
//            val captureCallback: CaptureCallback = object : CaptureCallback() {
//                override fun onCaptureCompleted(
//                    session: CameraCaptureSession,
//                    request: CaptureRequest,
//                    result: TotalCaptureResult
//                ) {
//                    repeatPreview()
//                }
//            }
//            mCaptureSession!!.capture(mCaptureBuilder.build(), captureCallback, null)
//        } catch (e: CameraAccessException) {
//            e.printStackTrace()
//        }
//    }

    class ImageSaver(private val mImage: Image) : Runnable {
        override fun run() {
            val buffer = mImage.planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer[data]
            val imageFile =
                File(Environment.getExternalStorageDirectory().toString() + "/DCIM/myPicture.jpg")
            var fos: FileOutputStream? = null
            try {
                fos = FileOutputStream(imageFile)
                fos.write(data, 0, data.size)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                if (fos != null) {
                    try {
                        fos.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "RHCamera2"
        private const val TAG_PREVIEW = "预览"
        private val ORIENTATION = SparseIntArray()

        init {
            ORIENTATION.append(Surface.ROTATION_0, 90)
            ORIENTATION.append(Surface.ROTATION_90, 0)
            ORIENTATION.append(Surface.ROTATION_180, 270)
            ORIENTATION.append(Surface.ROTATION_270, 180)
        }
    }
}