package com.example.cashcredit.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
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

    private const val TAG = "ImageRepository"

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
        val error: String? = null,
        val errorCode: String? = null
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
            Log.d(TAG, "开始上传图片: imagePath=$imagePath, imageType=$imageType, maxSizeKB=$maxSizeKB")

            // 1. 检查文件是否存在
            val file = File(imagePath)
            if (!file.exists()) {
                Log.e(TAG, "图片文件不存在: $imagePath")
                return@withContext UploadResult(
                    success = false,
                    error = "Image file not exists"
                )
            }

            Log.d(TAG, "原始文件大小: ${file.length() / 1024}KB")

            // 2. 压缩并转换为Base64
            val base64Str = compressAndEncodeImage(imagePath, maxSizeKB, quality)
            if (base64Str == null) {
                Log.e(TAG, "图片压缩或编码失败")
                return@withContext UploadResult(
                    success = false,
                    error = "Failed to compress or encode image"
                )
            }

            Log.d(TAG, "Base64字符串长度: ${base64Str.length}, 估算大小: ${base64Str.length / 1024 / 1.37}KB")

            // 检查 Base64 是否过大（服务端可能有限制）
            val estimatedSizeKB = base64Str.length / 1024 / 1.37 // Base64膨胀率约1.37
            if (estimatedSizeKB > 5000) {
                Log.w(TAG, "警告: Base64数据可能过大(${estimatedSizeKB}KB)，可能导致400错误")
            }

            // 3. 调用上传接口
            val request = ApiInterface.UploadImageRequest(
                fileBase64Str = base64Str,
                imageType = imageType
            )

            Log.d(TAG, "发送上传请求...")

            val response = apiService.uploadImage(request)

            Log.d(TAG, "上传响应: success=${response.isSuccess()}, code=${response.code}, message=${response.message}")

            if (response.isSuccess() && response.data != null) {
                Log.d(TAG, "上传成功: imageUrl=${response.data.imageUrl}")
                UploadResult(
                    success = true,
                    imageUrl = response.data.imageUrl,
                    httpImageUrl = response.data.httpImageUrl,
                    localPath = imagePath
                )
            } else {
                Log.e(TAG, "上传失败: code=${response.code}, message=${response.message}")
                UploadResult(
                    success = false,
                    error = response.message ?: "Upload failed",
                    errorCode = response.code.toString()
                )
            }
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP错误: code=${e.code()}, message=${e.message()}", e)
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "错误响应体: $errorBody")
            UploadResult(
                success = false,
                error = "HTTP ${e.code()}: ${e.message()}",
                errorCode = e.code().toString()
            )
        } catch (e: Exception) {
            Log.e(TAG, "上传异常: ${e.message}", e)
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
            if (!file.exists()) {
                Log.e(TAG, "文件不存在: $imagePath")
                return null
            }

            Log.d(TAG, "开始压缩图片: 原始大小=${file.length()/1024}KB")

            // 解码图片
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(imagePath, options)

            Log.d(TAG, "图片尺寸: ${options.outWidth}x${options.outHeight}")

            // 计算采样率 - 更激进地压缩大图
            var inSampleSize = 1
            val targetSize = 800 // 目标最大边长，更小以减少数据量
            if (options.outHeight > targetSize || options.outWidth > targetSize) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= targetSize && halfWidth / inSampleSize >= targetSize) {
                    inSampleSize *= 2
                }
            }

            Log.d(TAG, "采样率: $inSampleSize")

            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            options.inPreferredConfig = Bitmap.Config.RGB_565

            val bitmap = BitmapFactory.decodeFile(imagePath, options)
            if (bitmap == null) {
                Log.e(TAG, "解码图片失败")
                return null
            }

            Log.d(TAG, "解码后尺寸: ${bitmap.width}x${bitmap.height}")

            // 压缩为JPEG并转Base64
            val outputStream = ByteArrayOutputStream()
            var currentQuality = quality

            // 更严格的压缩循环
            var iterations = 0
            do {
                outputStream.reset()
                bitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, outputStream)
                val currentSizeKB = outputStream.size() / 1024
                Log.d(TAG, "压缩迭代$iterations: quality=$currentQuality, size=${currentSizeKB}KB")
                currentQuality -= 10
                iterations++
            } while (outputStream.size() / 1024 > maxSizeKB && currentQuality >= 10)

            bitmap.recycle()

            val byteArray = outputStream.toByteArray()
            val finalSizeKB: Int = byteArray.size / 1024
            Log.d(TAG, "最终压缩大小: ${finalSizeKB}KB")

            // 如果压缩后仍然太大，返回null (允许超过限制50%)
            val maxAllowedSizeKB: Int = (maxSizeKB * 1.5).toInt()
            if (finalSizeKB > maxAllowedSizeKB) {
                Log.e(TAG, "压缩后仍然太大: ${finalSizeKB}KB > ${maxAllowedSizeKB}KB")
                return null
            }

            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "压缩图片异常: ${e.message}", e)
            null
        }
    }
}