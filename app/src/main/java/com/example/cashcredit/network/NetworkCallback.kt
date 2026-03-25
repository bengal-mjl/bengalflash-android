package com.example.cashcredit.network

/**
 * 网络请求全局回调接口
 * 用于处理全局性的网络事件，如Token过期跳转登录、错误提示等
 */
interface NetworkCallback {

    /**
     * Token过期回调
     * 通常需要跳转到登录页面
     */
    fun onTokenExpired()

    /**
     * 无权限回调
     * @param message 错误信息
     */
    fun onForbidden(message: String)

    /**
     * 服务器错误回调
     * @param code 错误码
     * @param message 错误信息
     */
    fun onServerError(code: Int, message: String)

    /**
     * 网络错误回调
     * @param message 错误信息
     */
    fun onNetworkError(message: String)

    /**
     * 显示Loading
     * @param message 提示信息 (可选)
     */
    fun showLoading(message: String? = null)

    /**
     * 隐藏Loading
     */
    fun hideLoading()

    /**
     * 显示Toast提示
     * @param message 提示信息
     */
    fun showToast(message: String)
}

/**
 * NetworkCallback的空实现，方便使用方选择实现需要的方法
 */
open class SimpleNetworkCallback : NetworkCallback {
    override fun onTokenExpired() {}
    override fun onForbidden(message: String) {}
    override fun onServerError(code: Int, message: String) {}
    override fun onNetworkError(message: String) {}
    override fun showLoading(message: String?) {}
    override fun hideLoading() {}
    override fun showToast(message: String) {}
}

/**
 * 全局网络回调管理器
 */
object NetworkCallbackManager {
    private var callback: NetworkCallback? = null

    /**
     * 注册全局回调
     */
    fun register(callback: NetworkCallback) {
        this.callback = callback
    }

    /**
     * 注销全局回调
     */
    fun unregister() {
        callback = null
    }

    /**
     * 获取当前回调
     */
    fun getCallback(): NetworkCallback? = callback

    /**
     * Token过期
     */
    fun notifyTokenExpired() {
        callback?.onTokenExpired()
    }

    /**
     * 无权限
     */
    fun notifyForbidden(message: String) {
        callback?.onForbidden(message)
    }

    /**
     * 服务器错误
     */
    fun notifyServerError(code: Int, message: String) {
        callback?.onServerError(code, message)
    }

    /**
     * 网络错误
     */
    fun notifyNetworkError(message: String) {
        callback?.onNetworkError(message)
    }

    /**
     * 显示Toast
     */
    fun showToast(message: String) {
        callback?.showToast(message)
    }
}