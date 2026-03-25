package com.example.cashcredit.network

import android.content.Context
import com.example.cashcredit.util.AppDeviceInfo
import com.example.cashcredit.util.DeviceUtil
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

        // 添加设备信息JSON
        val deviceInfoJson = buildDeviceInfoJson()
        requestBuilder.addHeader(HEADER_DEVICE_INFO, deviceInfoJson)

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
            } else {
                // 如果AppDeviceInfo未初始化，使用备用方式获取
                json.put("appTerminal", "ANDROID")
                json.put("appVersion", AppDeviceInfo.getAppVersion(context))
                json.put("uuid", DeviceUtil.getDeviceId(context))
                json.put("gid", "")
                json.put("phoneTerminal", "ANDROID")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return json.toString()
    }
}