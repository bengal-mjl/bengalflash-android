package com.example.cashcredit.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.cashcredit.repository.ImageRepository
import com.example.cashcredit.ui.TakePhotoActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import wendu.dsbridge.CompletionHandler
import java.io.File
import java.io.FileOutputStream

/**
 * 相册选择管理器
 * 用于管理从相册选择照片并上传的回调
 */
object AlbumPickerManager {

    private const val TAG = "AlbumPickerManager"
    private var albumCallback: CompletionHandler<String>? = null
    private var pendingImageType: String? = null

    /**
     * 请求码（用于传统相册选择）
     */
    const val REQUEST_CODE = 201

    /**
     * 保存回调和图片类型
     */
    fun setCallback(handler: CompletionHandler<String>, imageType: String) {
        albumCallback = handler
        pendingImageType = imageType
    }

    /**
     * 获取回调
     */
    fun getCallback(): CompletionHandler<String>? = albumCallback

    /**
     * 获取待处理的图片类型
     */
    fun getPendingImageType(): String? = pendingImageType

    /**
     * 清除回调
     */
    fun clearCallback() {
        albumCallback = null
        pendingImageType = null
    }

    /**
     * 处理相册选择结果
     * @param context Context
     * @param uri 选择的图片Uri
     */
    fun handlePickResult(context: Context, uri: Uri) {
        val callback = albumCallback
        val imageType = pendingImageType

        if (callback == null || imageType == null) {
            Log.e(TAG, "Callback or imageType is null")
            clearCallback()
            return
        }

        val lifecycleOwner = context as? LifecycleOwner
        if (lifecycleOwner == null) {
            callback.complete("{\"success\": false, \"error\": \"Context is not LifecycleOwner\"}")
            clearCallback()
            return
        }

        lifecycleOwner.lifecycleScope.launch {
            try {
                // 1. 将Uri转换为本地文件
                val imagePath = withContext(Dispatchers.IO) {
                    uriToLocalFile(context, uri)
                }

                if (imagePath == null) {
                    callback.complete("{\"success\": false, \"error\": \"Failed to get image from album\"}")
                    clearCallback()
                    return@launch
                }

                // 2. 上传图片到服务器
                val result = withContext(Dispatchers.IO) {
                    ImageRepository.uploadImage(
                        imagePath = imagePath,
                        imageType = imageType,
                        maxSizeKB = 2048,
                        quality = 80
                    )
                }

                if (result.success) {
                    // 上传成功，返回服务器URL
                    val resultJson = buildString {
                        append("{\"success\": true")
                        if (!result.imageUrl.isNullOrEmpty()) {
                            append(", \"imageUrl\": \"${result.imageUrl}\"")
                        }
                        if (!result.httpImageUrl.isNullOrEmpty()) {
                            append(", \"httpImageUrl\": \"${result.httpImageUrl}\"")
                        }
                        append("}")
                    }
                    callback.complete(resultJson)
                } else {
                    callback.complete("{\"success\": false, \"error\": \"${result.error ?: "Upload failed"}\"}")
                }

                // 清理临时文件
                withContext(Dispatchers.IO) {
                    try {
                        File(imagePath).delete()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to delete temp file", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Handle album pick result failed", e)
                callback.complete("{\"success\": false, \"error\": \"${e.message}\"}")
            }

            clearCallback()
        }
    }

    /**
     * 通知选择失败
     */
    fun notifyFailure(error: String) {
        albumCallback?.complete("{\"success\": false, \"error\": \"$error\"}")
        clearCallback()
    }

    /**
     * 通知用户取消
     */
    fun notifyCancelled() {
        albumCallback?.complete("{\"success\": false, \"error\": \"User cancelled\", \"cancelled\": true}")
        clearCallback()
    }

    /**
     * 将Uri转换为本地文件路径
     */
    private fun uriToLocalFile(context: Context, uri: Uri): String? {
        return try {
            // 创建临时文件
            val tempFile = File(context.cacheDir, "album_${System.currentTimeMillis()}.jpg")

            // 从Uri读取数据并写入临时文件
            val inputStream = context.contentResolver.openInputStream(uri)
            Log.d(TAG, "inputStream opened: ${inputStream != null}")

            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for uri: $uri")
                return null
            }

            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val bytesCopied = input.copyTo(output)
                    Log.d(TAG, "Bytes copied: $bytesCopied")
                }
            }

            Log.d(TAG, "File created successfully: ${tempFile.absolutePath}, size=${tempFile.length()}")
            tempFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert Uri to local file", e)
            null
        }
    }

    /**
     * 检查是否有相册/存储权限
     * Android 13+ 不需要权限（使用Photo Picker）
     * Android 12及以下需要 READ_EXTERNAL_STORAGE 权限
     */
    fun hasAlbumPermission(context: Context): Boolean {
        // Android 13+ 使用 Photo Picker 不需要权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        // Android 12及以下需要存储权限
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 获取需要的相册权限数组
     * Android 13+ 返回空数组（不需要权限，使用Photo Picker）
     * Android 12及以下返回 READ_EXTERNAL_STORAGE
     */
    fun getRequiredPermissions(): Array<String> {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 使用 Photo Picker 不需要权限
            return emptyArray()
        }
        return arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}