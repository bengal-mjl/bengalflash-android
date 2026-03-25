package com.example.cashcredit.ui

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.media.ExifInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cashcredit.databinding.ActivityTakePhotoBinding
import com.example.cashcredit.repository.ImageRepository
import com.example.cashcredit.util.CameraCallbackManager
import com.example.cashcredit.util.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 证件拍照Activity
 * 用于拍摄身份证、驾驶证等证件照片
 */
class TakePhotoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTakePhotoBinding
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var capturedImageFile: File? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var imageType: String = ""
    private var resultLatch: CountDownLatch? = null
    private var capturedImageBase64: String? = null  // 缓存拍照后的Base64数据

    companion object {
        private const val REQUEST_CODE_CAMERA = 102
        private const val TAG = "TakePhotoActivity"

        // 证件类型
        const val ID_FRONT = "IdFront"
        const val ID_BACK = "IdBack"
        const val DRIVER_LICENSE = "driver"
        const val VOTER_CARD = "voterCard"
        const val FACE = "FACE"

        // 兼容H5传递的参数值
        const val ID_CARD_FRONT = "ID_CARD_FRONT"
        const val ID_CARD_BACK = "ID_CARD_BACK"

        // Intent Extra Keys
        const val EXTRA_IMAGE_PATH = "extra_image_path"
        const val EXTRA_SUCCESS = "extra_success"
        const val EXTRA_ERROR = "extra_error"

        // 事件Token
        private val EVENT_TOKENS = mapOf(
            ID_FRONT to mapOf(
                "open" to "14010138",
                "camera" to "14010139",
                "take" to "14010140",
                "confirm" to "14010144"
            ),
            ID_BACK to mapOf(
                "open" to "14010143",
                "camera" to "14010144",
                "take" to "14010145",
                "confirm" to "14010144"
            ),
            DRIVER_LICENSE to mapOf(
                "open" to "14010300",
                "camera" to "14010306",
                "take" to "14010302"
            ),
            VOTER_CARD to mapOf(
                "open" to "14010305",
                "camera" to "14010307"
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取证件类型
        imageType = intent.getStringExtra(ImageRepository.ImageType.imageType) ?: ""

        // 根据拍照类型设置屏幕方向
        requestedOrientation = when (imageType) {
            ID_FRONT, ID_BACK, ID_CARD_FRONT, ID_CARD_BACK -> {
                // 身份证拍照使用横屏
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            else -> {
                // 其他拍照使用竖屏
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }

        binding = ActivityTakePhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置遮罩框类型
        binding.overlayView.setFrameType(imageType)

        // 调整横屏时的按钮位置
        adjustButtonPositionForOrientation()

        setupListeners()
        checkCameraPermission()
    }

    /**
     * 根据屏幕方向调整按钮位置
     */
    private fun adjustButtonPositionForOrientation() {
        binding.overlayView.post {
            val isLandscape = binding.root.width > binding.root.height

            if (isLandscape) {
                // 横屏模式：拍照按钮放在边框右边，上下居中
                val frameRect = binding.overlayView.getFrameRect()
                val buttonWidth = 80 // dp，按钮宽度
                val buttonMargin = 40 // dp，边框到按钮的间距

                // 转换dp为px
                val density = resources.displayMetrics.density
                val buttonWidthPx = (buttonWidth * density).toInt()
                val buttonMarginPx = (buttonMargin * density).toInt()

                // 计算按钮位置
                val buttonLeft = frameRect.right.toInt() + buttonMarginPx
                val buttonHeightPx = buttonWidthPx
                val screenCenterY = binding.root.height / 2

                // 设置拍照按钮位置
                val takePhotoParams = binding.llTakeCamera.layoutParams as RelativeLayout.LayoutParams
                takePhotoParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                takePhotoParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
                takePhotoParams.width = buttonWidthPx
                takePhotoParams.height = RelativeLayout.LayoutParams.WRAP_CONTENT
                takePhotoParams.leftMargin = buttonLeft
                takePhotoParams.topMargin = screenCenterY - buttonHeightPx / 2
                binding.llTakeCamera.layoutParams = takePhotoParams
                binding.llTakeCamera.gravity = android.view.Gravity.CENTER
            } else {
                // 竖屏模式：按钮放在底部
                val takePhotoParams = binding.llTakeCamera.layoutParams as RelativeLayout.LayoutParams
                takePhotoParams.removeRule(RelativeLayout.ALIGN_PARENT_TOP)
                takePhotoParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                takePhotoParams.width = RelativeLayout.LayoutParams.MATCH_PARENT
                takePhotoParams.height = RelativeLayout.LayoutParams.WRAP_CONTENT
                takePhotoParams.leftMargin = 0
                takePhotoParams.topMargin = 0
                binding.llTakeCamera.layoutParams = takePhotoParams
            }
        }
    }

    /**
     * 设置点击监听器
     */
    private fun setupListeners() {
        binding.btnClose.setOnClickListener {
            finishAndReturnResult("")
        }

        binding.viewFinder.setOnClickListener {
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
            showCameraPreview(false)  // 显示相机预览，重新拍照
        }

        binding.btnConfirm.setOnClickListener {
            capturedImageFile?.let { file ->
                confirmImage(file.absolutePath)
            }
        }
    }

    /**
     * 检查相机权限
     */
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            showPermissionRationaleDialog()
        } else {
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
        showCameraPreview(false)

        // 设置PreviewView缩放模式为FILL_CENTER，确保预览填满整个View
        binding.viewFinder.scaleType = PreviewView.ScaleType.FILL_CENTER

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                // 配置预览 - 使用4:3比例以匹配拍照分辨率
                val preview = Preview.Builder()
                    .setTargetAspectRatio(androidx.camera.core.AspectRatio.RATIO_4_3)
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                    }

                // 配置图像捕获 - 使用4:3比例
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetAspectRatio(androidx.camera.core.AspectRatio.RATIO_4_3)
                    .build()

                // 选择摄像头：人脸拍照使用前置摄像头，证件拍照使用后置摄像头
                val cameraSelector = when {
                    imageType == FACE && hasFrontCamera() -> CameraSelector.DEFAULT_FRONT_CAMERA
                    hasBackCamera() -> CameraSelector.DEFAULT_BACK_CAMERA
                    else -> CameraSelector.DEFAULT_FRONT_CAMERA
                }

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
     * 检查是否有后置摄像头
     */
    private fun hasBackCamera(): Boolean {
        return try {
            val provider = ProcessCameraProvider.getInstance(this).get()
            provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查是否有前置摄像头
     */
    private fun hasFrontCamera(): Boolean {
        return try {
            val provider = ProcessCameraProvider.getInstance(this).get()
            provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 拍照
     */
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = FileUtils.createImageFile(this)
        capturedImageFile = photoFile

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).
        build()

        // 获取边框比例，用于后续裁剪
        val frameRectRatio = binding.overlayView.getFrameRectRatio()
        val previewWidth = binding.viewFinder.width
        val previewHeight = binding.viewFinder.height
        val frameType = binding.overlayView.getFrameTypeEnum()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // 人脸拍照不裁剪，身份证需要裁剪
                    val croppedFile = if (frameType == IdCardOverlayView.FrameType.FACE) {
                        // 人脸拍照不裁剪
                        null
                    } else {
                        // 裁剪图片，只保留边框内的区域
                        cropImageToFrame(
                            photoFile,
                            frameRectRatio,
                            previewWidth,
                            previewHeight,
                            frameType
                        )
                    }

                    // 如果裁剪成功，使用裁剪后的文件
                    val finalFile = croppedFile ?: photoFile

                    // 立即生成Base64，避免后续文件被删除或移动
                    capturedImageBase64 = imageToBase64(finalFile.absolutePath)

                    runOnUiThread {
                        capturedImageFile = finalFile
                        showCameraPreview(true)
                        displayCapturedImage(finalFile)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed", exception)
                    runOnUiThread {
                        showCameraPreview(false)
                    }
                }
            }
        )
    }

    /**
     * 显示/隐藏相机预览
     * @param showResult true=显示拍照结果, false=显示相机预览
     */
    private fun showCameraPreview(showResult: Boolean) {
        binding.llTakeCamera.visibility = if (showResult) View.GONE else View.VISIBLE
        binding.cameraOption.visibility = if (showResult) View.VISIBLE else View.GONE
        binding.ivTakeResult.visibility = if (showResult) View.VISIBLE else View.GONE
        binding.viewFinder.visibility = if (showResult) View.GONE else View.VISIBLE
        binding.overlayView.visibility = if (showResult) View.GONE else View.VISIBLE
    }

    /**
     * 显示捕获的图片
     */
    private fun displayCapturedImage(file: File) {
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
     * 裁剪图片到边框区域
     * @param sourceFile 原始图片文件
     * @param frameRectRatio 边框相对于预览View的比例位置
     * @param previewWidth 预览View宽度
     * @param previewHeight 预览View高度
     * @param frameType 边框类型
     * @return 裁剪后的图片文件，失败返回null
     */
    private fun cropImageToFrame(
        sourceFile: File,
        frameRectRatio: RectF,
        previewWidth: Int,
        previewHeight: Int,
        frameType: IdCardOverlayView.FrameType
    ): File? {
        try {
            // 读取图片并获取旋转角度
            val exif = ExifInterface(sourceFile.absolutePath)
            val rotation = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            // 读取原始图片
            val options = BitmapFactory.Options()
            var sourceBitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, options)
                ?: return null

            // 先旋转图片到正确方向
            sourceBitmap = rotateBitmapIfNeeded(sourceBitmap, rotation)

            val imageWidth = sourceBitmap.width
            val imageHeight = sourceBitmap.height

            Log.d(TAG, "Image size: ${imageWidth}x${imageHeight}, Preview size: ${previewWidth}x${previewHeight}")
            Log.d(TAG, "Frame ratio: $frameRectRatio")

            // PreviewView使用FILL_CENTER模式，预览内容会缩放以填充View
            // 预览内容是4:3比例，需要根据屏幕方向确定具体比例
            // 横屏时预览内容比例是4:3=1.33，竖屏时是3:4=0.75
            val previewAspect = previewWidth.toFloat() / previewHeight  // View的宽高比
            val previewContentAspect = if (previewWidth > previewHeight) {
                // 横屏模式，预览内容比例是4:3
                4f / 3f
            } else {
                // 竖屏模式，预览内容比例是3:4
                3f / 4f
            }

            // 计算预览内容在View中的显示区域（FILL_CENTER模式下）
            // 如果View比内容更"长"，内容上下会被裁剪；如果View比内容更"胖"，内容左右会被裁剪
            val (contentLeft, contentTop, contentRight, contentBottom) = if (previewContentAspect > previewAspect) {
                // 预览内容更"胖"，缩放以填满宽度，上下被裁剪
                val scaledHeight = previewWidth / previewContentAspect
                val offsetY = (previewHeight - scaledHeight) / 2
                listOf(0f, offsetY, previewWidth.toFloat(), offsetY + scaledHeight)
            } else {
                // 预览内容更"长"，缩放以填满高度，左右被裁剪
                val scaledWidth = previewHeight * previewContentAspect
                val offsetX = (previewWidth - scaledWidth) / 2
                listOf(offsetX, 0f, offsetX + scaledWidth, previewHeight.toFloat())
            }

            // 计算边框在预览内容中的实际位置
            val frameInContentLeft = (frameRectRatio.left * previewWidth - contentLeft).coerceAtLeast(0f)
            val frameInContentTop = (frameRectRatio.top * previewHeight - contentTop).coerceAtLeast(0f)
            val frameInContentRight = (frameRectRatio.right * previewWidth - contentLeft).coerceAtMost(contentRight - contentLeft)
            val frameInContentBottom = (frameRectRatio.bottom * previewHeight - contentTop).coerceAtMost(contentBottom - contentTop)

            // 预览内容的实际尺寸
            val contentWidth = contentRight - contentLeft
            val contentHeight = contentBottom - contentTop

            // 计算边框在预览内容中的比例
            val frameRatioInContent = RectF(
                frameInContentLeft / contentWidth,
                frameInContentTop / contentHeight,
                frameInContentRight / contentWidth,
                frameInContentBottom / contentHeight
            )

            Log.d(TAG, "Content rect: ($contentLeft, $contentTop, $contentRight, $contentBottom)")
            Log.d(TAG, "Frame ratio in content: $frameRatioInContent")

            // 映射到图片坐标
            val finalLeft = imageWidth * frameRatioInContent.left
            val finalTop = imageHeight * frameRatioInContent.top
            val finalRight = imageWidth * frameRatioInContent.right
            val finalBottom = imageHeight * frameRatioInContent.bottom

            val cropWidth = (finalRight - finalLeft).toInt()
            val cropHeight = (finalBottom - finalTop).toInt()

            Log.d(TAG, "Crop rect: ($finalLeft, $finalTop, $finalRight, $finalBottom), size: ${cropWidth}x${cropHeight}")

            if (cropWidth <= 0 || cropHeight <= 0) {
                Log.w(TAG, "Invalid crop region")
                return null
            }

            // 矩形裁剪
            val croppedBitmap = Bitmap.createBitmap(sourceBitmap, finalLeft.toInt(), finalTop.toInt(), cropWidth, cropHeight)

            // 保存裁剪后的图片
            val croppedFile = File.createTempFile("cropped_", ".jpg", cacheDir)
            FileOutputStream(croppedFile).use { output ->
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }

            // 回收Bitmap
            if (croppedBitmap != sourceBitmap) {
                croppedBitmap.recycle()
            }
            sourceBitmap.recycle()

            Log.d(TAG, "Image cropped successfully: ${cropWidth}x${cropHeight}")
            return croppedFile
        } catch (e: Exception) {
            Log.e(TAG, "Crop image failed", e)
            return null
        }
    }

    /**
     * 旋转图片
     */
    private fun rotateBitmapIfNeeded(source: Bitmap, rotation: Int): Bitmap {
        if (rotation == 0) return source

        val matrix = Matrix()
        matrix.postRotate(rotation.toFloat())
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        source.recycle()
        return rotated
    }

    /**
     * 确认图片 - 上传到服务器
     */
    private fun confirmImage(imagePath: String) {
        uploadImage(imagePath)
    }

    /**
     * 上传图片到服务器
     */
    private fun uploadImage(imagePath: String) {
        binding.loading.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 获取图片类型
                val imageType = imageType

                // 调用上传接口
                val result = ImageRepository.uploadImage(
                    imagePath = imagePath,
                    imageType = imageType,
                    maxSizeKB = 2048,
                    quality = 80
                )

                withContext(Dispatchers.Main) {
                    binding.loading.visibility = View.GONE

                    if (result.success) {
                        // 上传成功，返回服务器URL
                        finishAndReturnResultWithUrl(
                            localPath = imagePath,
                            imageUrl = result.imageUrl,
                            httpImageUrl = result.httpImageUrl
                        )
                    } else {
                        // 上传失败
                        Log.e(TAG, "Upload failed: ${result.error}")
                        CameraCallbackManager.notifyFailure(result.error ?: "Upload failed")
                        finish()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Image upload failed", e)
                withContext(Dispatchers.Main) {
                    binding.loading.visibility = View.GONE
                    CameraCallbackManager.notifyFailure(e.message ?: "Unknown error")
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

    /**
     * 图片转Base64
     */
    private fun imageToBase64(imagePath: String): String? {
        return try {
            val file = File(imagePath)
            if (!file.exists()) {
                Log.e(TAG, "File not exists: $imagePath")
                return null
            }

            // 先压缩图片
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(imagePath, options)

            // 计算采样率
            var inSampleSize = 1
            if (options.outHeight > 1024 || options.outWidth > 1024) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= 1024 && halfWidth / inSampleSize >= 1024) {
                    inSampleSize *= 2
                }
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            options.inPreferredConfig = Bitmap.Config.RGB_565

            val bitmap = BitmapFactory.decodeFile(imagePath, options)
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap: $imagePath")
                return null
            }

            // 转换为Base64
            val outputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()
            bitmap.recycle()

            android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "imageToBase64 failed", e)
            null
        }
    }
    /**
     * 完成并返回结果
     * @param imagePath 图片路径，如果为空则表示失败或取消
     */
    private fun finishAndReturnResult(imagePath: String) {
        if (imagePath.isNotEmpty()) {
            // 拍照成功，返回图片路径
            val resultIntent = Intent().apply {
                putExtra(EXTRA_SUCCESS, true)
                putExtra(EXTRA_IMAGE_PATH, imagePath)
            }
            setResult(RESULT_OK, resultIntent)

            // 通知回调（使用缓存的Base64）
            CameraCallbackManager.notifySuccessWithBase64(imagePath, capturedImageBase64)
        } else {
            // 用户取消或失败
            setResult(RESULT_CANCELED)
            CameraCallbackManager.notifyCancelled()
        }

        resultLatch?.countDown()
        finish()
    }

    /**
     * 完成并返回结果（包含服务器URL）
     * @param localPath 本地图片路径
     * @param imageUrl 服务器返回的图片URL
     * @param httpImageUrl 服务器返回的HTTP图片URL
     */
    private fun finishAndReturnResultWithUrl(
        localPath: String,
        imageUrl: String?,
        httpImageUrl: String?
    ) {
        // 拍照成功，返回图片路径和服务器URL
        val resultIntent = Intent().apply {
            putExtra(EXTRA_SUCCESS, true)
            putExtra(EXTRA_IMAGE_PATH, localPath)
            putExtra("image_url", imageUrl)
            putExtra("http_image_url", httpImageUrl)
        }
        setResult(RESULT_OK, resultIntent)

        // 通知回调（包含服务器URL和预计算的Base64）
        CameraCallbackManager.notifySuccessWithUrl(
            imageUrl = imageUrl,
            httpImageUrl = httpImageUrl
        )

        resultLatch?.countDown()
        finish()
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
            }
        }
    }

    override fun onBackPressed() {
        finishAndReturnResult("")
        super.onBackPressed()
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

        // 恢复屏幕方向为默认
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}