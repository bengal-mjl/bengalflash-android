package com.example.cashcredit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cashcredit.bridge.DSBridgeInterface
import com.example.cashcredit.config.AppConfig
import com.example.cashcredit.network.RetrofitClient
import com.example.cashcredit.repository.ImageRepository
import com.example.cashcredit.ui.TakePhotoActivity
import com.example.cashcredit.util.BarUtil
import com.example.cashcredit.util.LanguageUtil
import com.example.cashcredit.util.ContactPickerManager
import com.example.cashcredit.util.LivenessCallbackManager
import com.example.cashcredit.util.PermissionDialogManager
import com.example.cashcredit.util.PermissionHelper
import com.example.cashcredit.util.StatusBarUtil
import com.izilab.liveness.api.LivenessPhoto
import com.izilab.liveness.api.LivenessResult
import com.example.cashcredit.webview.DWebViewManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import wendu.dsbridge.DWebView
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var dWebView: DWebView
    private lateinit var progressBar: ProgressBar
    private lateinit var webViewManager: DWebViewManager

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageUtil.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置沉浸式状态栏
        initNetworkClient()
        StatusBarUtil.setImmersiveStatusBar(this, false)
        setContentView(R.layout.activity_main)

        initViews()
        // 延迟初始化WebView，避免启动卡顿
        dWebView.post {
            initWebView()
            loadH5()
        }
        Log.e("xxxxx", BarUtil.getStatusBarHeight().toString())
    }
    /**
     * 初始化网络请求客户端
     */
    private fun initNetworkClient() {
        val isDebug = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            true
        }

        RetrofitClient.init(
            context = this,
            debug = isDebug
        )
        // 设置API基础URL
        RetrofitClient.baseUrl = AppConfig.getApiBaseUrl()
    }
    private fun initViews() {
        dWebView = findViewById(R.id.dWebView)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun initWebView() {
        // 初始化DWebView管理器
        webViewManager = DWebViewManager(this, dWebView, progressBar)
        webViewManager.init()

        // 注册DSBridge接口
        DSBridgeInterface.register(dWebView, this)
    }

    private fun loadH5() {
        // 加载H5页面
        val url = AppConfig.getH5Url()
        webViewManager.loadUrl(url)
    }

    /**
     * 处理返回键
     * 如果WebView可以后退，则后退；否则退出应用
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && ::webViewManager.isInitialized && webViewManager.canGoBack()) {
            webViewManager.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        dWebView.onResume()
    }

    override fun onPause() {
        super.onPause()
        dWebView.onPause()
    }

    override fun onDestroy() {
        if (::webViewManager.isInitialized) {
            webViewManager.destroy()
        }
        PermissionDialogManager.dismissDialog()
        super.onDestroy()
    }

    /**
     * 处理权限请求结果
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // 先让PermissionDialogManager处理
        if (!PermissionDialogManager.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            // 如果不是权限弹窗的请求，交给PermissionHelper处理
            PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    /**
     * 处理Activity结果
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 处理联系人选择结果
        if (requestCode == ContactPickerManager.getRequestCode()) {
            if (resultCode == RESULT_OK && data != null) {
                ContactPickerManager.handlePickResult(contentResolver, data)
            } else {
                ContactPickerManager.notifyCancelled()
            }
            return
        }

        // 处理活体检测结果
        if (requestCode == LivenessCallbackManager.REQUEST_CODE) {
            if (LivenessResult.isSuccess()) {
                val livenessPhoto = LivenessResult.getFacePhoto()
//                val livenessScore = LivenessResult.getLivenessScore()
                val livenessScore = 95f
                if (livenessPhoto != null) {
                    // 活体检测成功，上传人脸照片
                    uploadLivenessImage(livenessPhoto, livenessScore)
                } else {
                    LivenessCallbackManager.notifyFailure("Failed to get face photo")
                }
            } else {
                val errorMsg = LivenessResult.getErrorMsg() ?: "Liveness detection failed"
                LivenessCallbackManager.notifyFailure(errorMsg)
            }
            return
        }

        // 处理从设置界面返回的结果
        PermissionDialogManager.onActivityResult(requestCode, resultCode, data)
        PermissionHelper.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * 上传活体检测图片到服务器
     * @param livenessPhoto 活体检测照片
     * @param livenessScore 活体检测分数
     */
    private fun uploadLivenessImage(livenessPhoto: LivenessPhoto, livenessScore: Float?) {
        lifecycleScope.launch {
            try {
                // 1. 从 LivenessPhoto 获取 Bitmap
                val bitmap = livenessPhoto.bitmap
                if (bitmap == null) {
                    LivenessCallbackManager.notifyFailure("Failed to get face photo")
                    return@launch
                }

                // 2. 将Bitmap保存为临时文件
                val imagePath = livenessPhoto.filePath
//                val imagePath = saveBitmapToTempFile(bitmap)
//                if (imagePath == null) {
//                    LivenessCallbackManager.notifyFailure("Failed to save face photo")
//                    return@launch
//                }

                // 3. 上传图片到服务器
                withContext(Dispatchers.IO) {
                    val result = ImageRepository.uploadImage(
                        imagePath = imagePath,
                        imageType = TakePhotoActivity.FACE,
                        maxSizeKB = 2048,
                        quality = 80
                    )
                    result
                }.let { result ->
                    if (result.success) {
                        // 上传成功，返回服务器URL
                        LivenessCallbackManager.notifySuccessWithUrl(
                            imageUrl = result.imageUrl,
                            httpImageUrl = result.httpImageUrl
                        )
                    } else {
                        // 上传失败
                        LivenessCallbackManager.notifyFailure(result.error ?: "Upload failed")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Upload liveness image failed", e)
                LivenessCallbackManager.notifyFailure(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * 将Bitmap保存为临时文件
     * @param bitmap 要保存的Bitmap
     * @return 临时文件路径，失败返回null
     */
    private fun saveBitmapToTempFile(bitmap: Bitmap): String? {
        return try {
            val tempFile = File(cacheDir, "liveness_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
            tempFile.absolutePath
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to save bitmap to temp file", e)
            null
        }
    }
}