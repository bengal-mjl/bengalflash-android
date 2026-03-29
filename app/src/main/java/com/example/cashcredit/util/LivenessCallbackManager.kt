package com.example.cashcredit.util

import wendu.dsbridge.CompletionHandler

/**
 * 活体检测回调管理器
 * 用于管理活体检测结果的回调
 */
object LivenessCallbackManager {

    private var livenessCallback: CompletionHandler<String>? = null

    /**
     * 请求码
     */
    const val REQUEST_CODE = 100

    /**
     * 保存回调
     */
    fun setCallback(handler: CompletionHandler<String>) {
        livenessCallback = handler
    }

    /**
     * 获取回调
     */
    fun getCallback(): CompletionHandler<String>? = livenessCallback

    /**
     * 清除回调
     */
    fun clearCallback() {
        livenessCallback = null
    }

    /**
     * 通知活体检测成功
     * @param imagePath 活体检测图片路径
     * @param livenessScore 活体检测分数
     */
    fun notifySuccess(imagePath: String?, livenessScore: Float?) {
        val callback = livenessCallback ?: return
        try {
            val result = buildString {
                append("{\"success\": true")
                if (!imagePath.isNullOrEmpty()) {
                    append(", \"imagePath\": \"$imagePath\"")
                }
                if (livenessScore != null) {
                    append(", \"livenessScore\": $livenessScore")
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
     * 通知活体检测成功（包含服务器URL）
     * @param imageUrl 服务器返回的图片URL
     * @param httpImageUrl 服务器返回的HTTP图片URL
     * @param livenessScore 活体检测分数
     */
    fun notifySuccessWithUrl(
        imageUrl: String?,
        httpImageUrl: String?,
        livenessScore: Float? = null
    ) {
        val callback = livenessCallback ?: return

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
                if (livenessScore != null) {
                    append(", \"livenessScore\": $livenessScore")
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
     * 通知活体检测失败
     */
    fun notifyFailure(error: String) {
        livenessCallback?.complete("{\"success\": false, \"error\": \"$error\"}")
        clearCallback()
    }

    /**
     * 通知用户取消
     */
    fun notifyCancelled() {
        livenessCallback?.complete("{\"success\": false, \"error\": \"User cancelled\", \"cancelled\": true}")
        clearCallback()
    }
}