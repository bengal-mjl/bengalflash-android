package com.example.cashcredit.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException

/**
 * 响应拦截器
 * 统一处理响应数据，包括错误码判断、Token过期处理等
 */
class ResponseInterceptor : Interceptor {

    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        // 如果响应不成功，直接返回
        if (!response.isSuccessful) {
            handleHttpError(response.code)
            return response
        }

        // 解析响应体
        val responseBody = response.body ?: return response

        try {
            val source = responseBody.source()
            source.request(Long.MAX_VALUE)
            val buffer = source.buffer
            val responseString = buffer.clone().readUtf8()

            // 尝试解析为JSON
            val jsonObject = gson.fromJson(responseString, JsonObject::class.java)

            // 获取业务状态码
            val code = jsonObject.get("code")?.asInt ?: 0
            val message = jsonObject.get("message")?.asString ?: "Unknown error"

            // 处理业务错误码
            handleBusinessCode(code, message)

            // 重新创建响应体
            val newResponseBody = ResponseBody.create(
                responseBody.contentType(),
                responseString
            )

            return response.newBuilder()
                .body(newResponseBody)
                .build()

        } catch (e: Exception) {
            // JSON解析失败，可能是非标准响应格式，直接返回原响应
            return response
        }
    }

    /**
     * 处理HTTP错误码
     */
    private fun handleHttpError(httpCode: Int) {
        when (httpCode) {
            401 -> {
                // Token过期
                TokenManager.clearToken()
                NetworkCallbackManager.notifyTokenExpired()
            }
            403 -> {
                // 无权限
                NetworkCallbackManager.notifyForbidden("无访问权限")
            }
            500 -> {
                // 服务器错误
                NetworkCallbackManager.notifyServerError(httpCode, "服务器错误")
            }
        }
    }

    /**
     * 处理业务错误码
     */
    private fun handleBusinessCode(code: Int, message: String) {
        when (code) {
            ApiResponse.Code.SUCCESS -> {
                // 成功，不做处理
            }
            ApiResponse.Code.TOKEN_EXPIRED -> {
                // Token过期
                TokenManager.clearToken()
                NetworkCallbackManager.notifyTokenExpired()
            }
            ApiResponse.Code.FORBIDDEN -> {
                // 无权限
                NetworkCallbackManager.notifyForbidden(message)
            }
            ApiResponse.Code.SERVER_ERROR -> {
                // 服务器错误
                NetworkCallbackManager.notifyServerError(code, message)
            }
        }
    }
}