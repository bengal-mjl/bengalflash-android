package com.example.cashcredit.network

import android.content.Context
import com.example.cashcredit.util.AppDeviceInfo
import com.example.cashcredit.util.DeviceUtil
import android.util.Base64
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject

/**
 * 统一请求头拦截器
 * 自动添加公共请求头到每个请求中
 */
class ApiInterceptor(
    private val context: Context
) : Interceptor {

    companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        const val HEADER_DEVICE_INFO = "deviceInfo"
        const val HEADER_PLATFORM = "Platform"
        const val HEADER_CONTENT_TYPE = "Content-Type"
        const val HEADER_ACCEPT = "Accept"
        const val PLATFORM_ANDROID = "Android"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // 添加Authorization头 (如果有Token)
        TokenManager.getToken()?.let { token ->
            requestBuilder.addHeader(HEADER_AUTHORIZATION, "Bearer $token")
        }

        // 添加设备信息JSON (Base64编码，避免特殊字符导致的请求头解析问题)
        val deviceInfoJson = buildDeviceInfoJson()
        val deviceInfoBase64 = Base64.encodeToString(
            deviceInfoJson.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )
        requestBuilder.addHeader(HEADER_DEVICE_INFO, deviceInfoBase64)

        // 添加Content-Type (仅对POST/PUT/PATCH请求)
        val method = originalRequest.method
        if (method == "POST" || method == "PUT" || method == "PATCH") {
            requestBuilder.addHeader(HEADER_CONTENT_TYPE, "application/json")
        }

        // 添加Accept头
        requestBuilder.addHeader(HEADER_ACCEPT, "application/json")

        val newRequest = requestBuilder.build()
        return chain.proceed(newRequest)
    }

    /**
     * 构建设备信息JSON字符串
     */
    private fun buildDeviceInfoJson(): String {
        val json = JSONObject()
        try {
            if (AppDeviceInfo.isInitialized()) {
                json.put("appTerminal", AppDeviceInfo.getAppTerminal())
                json.put("appVersion", AppDeviceInfo.getAppVersion())
                json.put("uuid", AppDeviceInfo.getUuid())
                json.put("gid", AppDeviceInfo.getGid())
                json.put("phoneTerminal", AppDeviceInfo.getPhoneTerminal())
                json.put("channelNo", AppDeviceInfo.getChannelNo())
            } else {
                // 如果AppDeviceInfo未初始化，使用备用方式获取
                json.put("appTerminal", "ANDROID")
                json.put("appVersion", AppDeviceInfo.getAppVersion(context))
                json.put("uuid", DeviceUtil.getDeviceId(context))
                json.put("gid", "")
                json.put("phoneTerminal", "ANDROID")
                json.put("channelNo", AppDeviceInfo.CHANNEL_NO )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return json.toString()
    }
}