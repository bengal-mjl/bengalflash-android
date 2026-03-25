package com.example.cashcredit.util

import android.content.Context
import android.util.Log
import com.example.cashcredit.model.DeviceUploadData
import com.example.cashcredit.model.UploadDeviceResponse
import com.example.cashcredit.network.ApiResponse
import com.example.cashcredit.network.RetrofitClient
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * 设备数据上传管理类
 */
object DeviceUploadManager {

    private const val TAG = "DeviceUploadManager"

    private val gson = Gson()

    /**
     * 上传设备数据
     * @param context 上下文
     * @param sceneType 场景类型
     */
    fun uploadDeviceData(
        context: Context,
        sceneType: String
    ): Flow<Result<UploadDeviceResponse>> = flow {
        Log.d(TAG, "Starting device data upload...")

        // 收集设备数据
        val deviceData = DeviceDataCollector.collectAllData(context)
        Log.d(TAG, "Device data collected")

        // 转换为JSON
        val jsonData = gson.toJson(deviceData)
        Log.d(TAG, "Device data JSON size: ${jsonData.length}")

        // 创建临时JSON文件 - 保存到内部存储
        val tempFile = File(context.filesDir, "device_json_${System.currentTimeMillis()}.json")
        tempFile.writeText(jsonData)

        // 打印临时文件内容用于调试
        Log.d(TAG, "Temp file path: ${tempFile.absolutePath}")
        Log.d(TAG, "Temp file content: $jsonData")

        // 创建请求体
        val sceneTypeBody = sceneType.toRequestBody("text/plain".toMediaTypeOrNull())

        val requestFile = tempFile.asRequestBody("application/json".toMediaTypeOrNull())
        val deviceJsonPart = MultipartBody.Part.createFormData("deviceJson", tempFile.name, requestFile)

        // 发送请求
        val response = RetrofitClient.api.uploadDevice( sceneTypeBody, deviceJsonPart)

        // 删除临时文件
        tempFile.delete()

        if (response.isSuccess()) {
            val data = response.data
            if (data != null) {
                Log.d(TAG, "Upload success: $data")
                emit(Result.success(data))
            } else {
                emit(Result.failure(NullPointerException("Response data is null")))
            }
        } else {
            Log.e(TAG, "Upload failed: ${response.message}")
            emit(Result.failure(Exception(response.message)))
        }
    }.catch { e ->
        Log.e(TAG, "Upload error", e)
        emit(Result.failure(e))
    }.flowOn(Dispatchers.IO)

    /**
     * 上传设备数据（带回调）
     * @param context 上下文
     * @param recordNo 记录编号
     * @param sceneType 场景类型
     * @param onSuccess 成功回调
     * @param onFailure 失败回调
     */
    suspend fun uploadDeviceDataWithCallback(
        context: Context,
        sceneType: String,
        onSuccess: (UploadDeviceResponse) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        uploadDeviceData(context, sceneType)
            .collect { result ->
                result.onSuccess { data -> onSuccess(data) }
                    .onFailure { error -> onFailure(error) }
            }
    }
}