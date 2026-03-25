package com.example.cashcredit.network

import android.content.Context
import com.example.cashcredit.config.AppConfig

/**
 * Token管理器
 * 负责Token的存储、读取和清除
 */
object TokenManager {

    private const val KEY_TOKEN = "token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_TOKEN_EXPIRE_TIME = "token_expire_time"

    private lateinit var appContext: Context

    /**
     * 初始化TokenManager
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val sharedPreferences by lazy {
        appContext.getSharedPreferences(AppConfig.SP_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 保存Token
     * @param token 访问令牌
     * @param refreshToken 刷新令牌 (可选)
     * @param expireTime 过期时间戳 (毫秒，可选)
     */
    fun saveToken(token: String, refreshToken: String? = null, expireTime: Long? = null) {
        sharedPreferences.edit().apply {
            putString(KEY_TOKEN, token)
            refreshToken?.let { putString(KEY_REFRESH_TOKEN, it) }
            expireTime?.let { putLong(KEY_TOKEN_EXPIRE_TIME, it) }
            apply()
        }
    }

    /**
     * 获取Token
     * @return Token字符串，未登录返回null
     */
    fun getToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    /**
     * 获取RefreshToken
     * @return RefreshToken字符串，不存在返回null
     */
    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    /**
     * 检查Token是否过期
     * @return true表示已过期
     */
    fun isTokenExpired(): Boolean {
        val expireTime = sharedPreferences.getLong(KEY_TOKEN_EXPIRE_TIME, 0)
        if (expireTime <= 0) return false
        return System.currentTimeMillis() >= expireTime
    }

    /**
     * 检查是否已登录
     */
    fun isLoggedIn(): Boolean {
        val token = getToken()
        return !token.isNullOrEmpty() && !isTokenExpired()
    }

    /**
     * 清除Token (退出登录时调用)
     */
    fun clearToken() {
        sharedPreferences.edit().apply {
            remove(KEY_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_TOKEN_EXPIRE_TIME)
            apply()
        }
    }

    /**
     * Token刷新回调接口
     */
    interface TokenRefreshCallback {
        /**
         * 刷新Token
         * @param refreshToken 刷新令牌
         * @return 刷新成功返回true
         */
        suspend fun onRefreshToken(refreshToken: String): Boolean
    }

    private var tokenRefreshCallback: TokenRefreshCallback? = null

    /**
     * 设置Token刷新回调
     */
    fun setTokenRefreshCallback(callback: TokenRefreshCallback?) {
        tokenRefreshCallback = callback
    }

    /**
     * 执行Token刷新
     */
    suspend fun refreshToken(): Boolean {
        val refreshToken = getRefreshToken() ?: return false
        return tokenRefreshCallback?.onRefreshToken(refreshToken) ?: false
    }
}