package com.example.cashcredit.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.cashcredit.network.RetrofitClient
import com.example.cashcredit.network.api.ApiInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * 图片上传仓库
 * 处理图片压缩、Base64编码和上传逻辑
 */
object ImageRepository {

    private val apiService: ApiInterface by lazy {
        RetrofitClient.create(ApiInterface::class.java)
    }

    /**
     * 图片类型枚举
     */
    object ImageType {
      const val imageType = "imageType"//照片类型
    }

    /**
     * 上传图片结果
     */
    data class UploadResult(
        val success: Boolean,
        val imageUrl: String? = null,
        val httpImageUrl: String? = null,
        val localPath: String? = null,
        val error: String? = null
    )

    /**
     * 上传图片
     * @param imagePath 本地图片路径
     * @param imageType 图片类型
     * @param maxSizeKB 最大尺寸KB，默认2048KB
     * @param quality 压缩质量，默认80
     */
    suspend fun uploadImage(
        imagePath: String,
        imageType: String,
        maxSizeKB: Int = 2048,
        quality: Int = 80
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            // 1. 压缩并转换为Base64
            val base64Str = compressAndEncodeImage(imagePath, maxSizeKB, quality)
                ?: return@withContext UploadResult(
                    success = false,
                    error = "Failed to compress or encode image"
                )

            // 2. 调用上传接口
            val request = ApiInterface.UploadImageRequest(
                fileBase64Str = base64Str,
                imageType = imageType
            )

            val response = apiService.uploadImage(request)

            if (response.isSuccess() && response.data != null) {
                UploadResult(
                    success = true,
                    imageUrl = response.data.imageUrl,
                    httpImageUrl = response.data.httpImageUrl,
                    localPath = imagePath
                )
            } else {
                UploadResult(
                    success = false,
                    error = response.message
                )
            }
        } catch (e: Exception) {
            UploadResult(
                success = false,
                error = e.message ?: "Unknown error"
            )
        }
    }

    /**
     * 压缩图片并转换为Base64字符串
     * @param imagePath 图片路径
     * @param maxSizeKB 最大尺寸KB
     * @param quality 初始压缩质量
     * @return Base64编码字符串，失败返回null
     */
    private fun compressAndEncodeImage(
        imagePath: String,
        maxSizeKB: Int,
        quality: Int
    ): String? {
        return try {
            val file = File(imagePath)
            if (!file.exists()) return null

            // 解码图片
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(imagePath, options)

            // 计算采样率
            var inSampleSize = 1
            if (options.outHeight > 1200 || options.outWidth > 1200) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= 1200 && halfWidth / inSampleSize >= 1200) {
                    inSampleSize *= 2
                }
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            options.inPreferredConfig = Bitmap.Config.RGB_565

            val bitmap = BitmapFactory.decodeFile(imagePath, options) ?: return null

            // 压缩为JPEG并转Base64
            val outputStream = ByteArrayOutputStream()
            var currentQuality = quality

            do {
                outputStream.reset()
                bitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, outputStream)
                currentQuality -= 10
            } while (outputStream.size() / 1024 > maxSizeKB && currentQuality >= 10)

            bitmap.recycle()

            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }
}