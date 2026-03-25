package com.example.cashcredit.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cashcredit.databinding.ActivitySilentLivingBinding
import com.example.cashcredit.util.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 活体检测Activity
 * 用于人脸识别验证
 */
class SilentLivingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySilentLivingBinding
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var capturedImageFile: File? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    companion object {
        private const val REQUEST_CODE_CAMERA = 102
        private const val TAG = "SilentLivingActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySilentLivingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        checkCameraPermission()
    }

    /**
     * 设置点击监听器
     */
    private fun setupListeners() {
        binding.btnClose.setOnClickListener {
            finish()
        }

        binding.viewFinder.setOnClickListener {
            // 对焦
            try {
                binding.viewFinder.requestFocus()
            } catch (e: Exception) {
                Log.e(TAG, "Focus failed", e)
            }
        }

        binding.btnTakePhoto.setOnClickListener {
            takePhoto()
        }

        binding.btnRetake.setOnClickListener {
            showCameraPreview(true)
        }

        binding.btnConfirm.setOnClickListener {
            capturedImageFile?.let { file ->
                uploadImage(file.absolutePath)
            }
        }
    }

    /**
     * 检查相机权限
     */
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            // 权限已授予，启动相机
            startCamera()
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            // 需要显示权限说明
            showPermissionRationaleDialog()
        } else {
            // 直接请求权限
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CODE_CAMERA
            )
        }
    }

    /**
     * 显示权限说明对话框
     */
    private fun showPermissionRationaleDialog() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CODE_CAMERA
        )
    }

    /**
     * 启动相机预览
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                // 配置预览
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                    }

                // 配置图像捕获
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // 选择前置摄像头
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                // 解绑所有用例
                cameraProvider?.unbindAll()

                // 绑定用例
                cameraProvider?.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )

            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * 拍照
     */
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // 创建输出文件
        val photoFile = FileUtils.createImageFile(this)
        capturedImageFile = photoFile

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    runOnUiThread {
                        showCameraPreview(false)
                        displayCapturedImage(photoFile)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed", exception)
                    runOnUiThread {
                        showCameraPreview(true)
                    }
                }
            }
        )
    }

    /**
     * 显示/隐藏相机预览
     */
    private fun showCameraPreview(showPreview: Boolean) {
        runOnUiThread {
            binding.llTakeCamera.visibility = if (showPreview) View.VISIBLE else View.GONE
            binding.cameraOption.visibility = if (showPreview) View.GONE else View.VISIBLE
            binding.ivTakeResult.visibility = if (showPreview) View.GONE else View.VISIBLE
            binding.viewFinder.visibility = if (showPreview) View.VISIBLE else View.GONE
        }
    }

    /**
     * 显示捕获的图片
     */
    private fun displayCapturedImage(file: File) {
        binding.ivTakeResult.visibility = View.VISIBLE
        binding.viewFinder.visibility = View.GONE

        // 加载并显示图片
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.path, options)

        options.inSampleSize = calculateInSampleSize(options, 800, 800)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565

        val bitmap = BitmapFactory.decodeFile(file.path, options)
        binding.ivTakeResult.setImageBitmap(bitmap)
    }

    /**
     * 计算采样大小
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * 上传图片
     */
    private fun uploadImage(imagePath: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 压缩图片
                val compressedFile = compressImage(File(imagePath))

                // 使用 ApiManager 上传
//                ApiManager.uploadLivingBodyImage(compressedFile, object : ApiManager.ApiCallback<ImageUploadResponse> {
//                    override fun onSuccess(response: ImageUploadResponse) {
//                        response.uploadDetails?.let { details ->
//                            if (details.isUploadSuccess) {
//                                val intent = Intent().apply {
//                                    putExtra("url", details.redirectUrl)
//                                }
//                                setResult(RESULT_OK, intent)
//                            } else {
//                                val intent = Intent().apply {
//                                    putExtra("url", details.redirectUrl)
//                                }
//                                setResult(RESULT_CANCELED, intent)
//                            }
//                            finish()
//                        }
//                    }
//
//                    override fun onError(error: String) {
//                        Log.e(TAG, "Upload failed: $error")
//                        TwairtOrthoApp.getInstance().trackEvent("14010155")
//                        ApiManager.reportImageUploadFailure(error)
//                        finish()
//                    }
//                })
            } catch (e: Exception) {
                Log.e(TAG, "Image processing failed", e)
                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }
    }

    /**
     * 压缩图片
     */
    private fun compressImage(file: File, maxSizeKB: Int = 2048): File {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.path, options)

        options.inSampleSize = calculateInSampleSize(options, 1200, 1200)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565

        val bitmap = BitmapFactory.decodeFile(file.path, options)
        val compressedFile = File.createTempFile("compressed_", ".jpg", file.parentFile)

        var quality = 90
        do {
            FileOutputStream(compressedFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            }
            quality -= 10
        } while (compressedFile.length() / 1024 > maxSizeKB && quality >= 10)

        bitmap.recycle()
        return compressedFile
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                // 权限被拒绝
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    // 用户选择了"不再询问"，引导到设置页面
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            if (cameraProvider == null) {
                startCamera()
            }
        } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            showPermissionRationaleDialog()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.e(TAG, "Camera release failed", e)
        }
        cameraExecutor.shutdown()
    }
}