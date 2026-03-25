package com.example.cashcredit.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import wendu.dsbridge.CompletionHandler
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * 相机回调管理器
 * 用于管理拍照结果的回调
 */
object CameraCallbackManager {

    private var cameraCallback: CompletionHandler<String>? = null

    /**
     * 保存回调
     */
    fun setCallback(handler: CompletionHandler<String>) {
        cameraCallback = handler
    }

    /**
     * 获取回调
     */
    fun getCallback(): CompletionHandler<String>? {
        return cameraCallback
    }

    /**
     * 清除回调
     */
    fun clearCallback() {
        cameraCallback = null
    }

    /**
     * 通知拍照成功
     */
    fun notifySuccess(imagePath: String, includeBase64: Boolean = true) {
        val callback = cameraCallback ?: return

        try {
            val result = buildString {
                append("{\"success\": true")
                append(", \"imagePath\": \"$imagePath\"")

                if (includeBase64) {
                    val base64 = imageToBase64(imagePath)
                    if (base64 != null) {
                        append(", \"imageBase64\": \"$base64\"")
                    }
                }
                append("}")
            }
            callback.complete(result)
        } catch (e: Exception) {
            callback.complete("{\"success\": false, \"error\": \"${e.message}\"}")
        }

        clearCallback()
    }

    /**
     * 通知拍照成功（使用预计算的Base64）
     */
    fun notifySuccessWithBase64(imagePath: String, preComputedBase64: String?) {
        val callback = cameraCallback ?: return

        try {
            val result = buildString {
                append("{\"success\": true")
                append(", \"imagePath\": \"$imagePath\"")

                // 优先使用预计算的Base64，否则尝试从文件读取
                val base64 = preComputedBase64 ?: imageToBase64(imagePath)
                if (base64 != null) {
                    append(", \"imageBase64\": \"$base64\"")
                }
                append("}")
            }
            callback.complete(result)
        } catch (e: Exception) {
            callback.complete("{\"success\": false, \"error\": \"${e.message}\"}")
        }

        clearCallback()
    }

    /**
     * 通知拍照成功（包含服务器URL）
     */
    fun notifySuccessWithUrl(
        imageUrl: String?,
        httpImageUrl: String?
    ) {
        val callback = cameraCallback ?: return

        try {
            val result = buildString {
                append("{\"success\": true")

                // 添加服务器返回的URL
                if (!imageUrl.isNullOrEmpty()) {
                    append(", \"imageUrl\": \"$imageUrl\"")
                }
                if (!httpImageUrl.isNullOrEmpty()) {
                    append(", \"httpImageUrl\": \"$httpImageUrl\"")
                }
                append("}")
            }
            callback.complete(result)
        } catch (e: Exception) {
            callback.complete("{\"success\": false, \"error\": \"${e.message}\"}")
        }

        clearCallback()
    }

    /**
     * 通知拍照失败
     */
    fun notifyFailure(error: String) {
        cameraCallback?.complete("{\"success\": false, \"error\": \"$error\"}")
        clearCallback()
    }

    /**
     * 通知用户取消
     */
    fun notifyCancelled() {
        cameraCallback?.complete("{\"success\": false, \"error\": \"User cancelled\", \"cancelled\": true}")
        clearCallback()
    }

    /**
     * 图片转Base64
     */
    private fun imageToBase64(imagePath: String): String? {
        return try {
            val file = File(imagePath)
            if (!file.exists()) return null

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

            val bitmap = BitmapFactory.decodeFile(imagePath, options) ?: return null

            // 转换为Base64
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()
            bitmap.recycle()

            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }
}