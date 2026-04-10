package com.example.cashcredit.config

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * 应用配置常量
 */
object AppConfig {

    /**
     * API基础URL
     * 生产环境请替换为实际API地址
     */
    const val API_BASE_URL = "https://api.example.com/"

    /**
     * API基础测试URL
     * 注意：IP地址使用HTTP，因为没有SSL证书
     */
//    var API_BASE_URL_TEST = "http://192.168.2.111:8081/"
    var API_BASE_URL_TEST = "http://api-dev.bengalflash.com/"

    fun getApiBaseUrl(): String {
        return if (USE_TEST_URL) API_BASE_URL_TEST else API_BASE_URL
    }

    /**
     * H5页面URL配置
     * 生产环境请替换为实际URL
     */
    const val H5_URL = "https://www.baidu.com/"

    /**
     * 测试H5页面URL（用于调试）
     * 可以指向本地HTML文件或测试服务器
     */
    var H5_URL_TEST = "file:///android_asset/test.html"//本地
//    var H5_URL_TEST = "http://192.168.31.111:3003"//帆本地测试
//    var H5_URL_TEST = "http://h5-dev.bengalflash.com"//H5测试环境

    /**
     * 是否使用测试URL
     * 发布时设为false
     */
    const val USE_TEST_URL = true

    /**
     * 获取当前使用的H5 URL
     */
    fun getH5Url(): String {
        return if (USE_TEST_URL) H5_URL_TEST else H5_URL
    }

    /**
     * JS接口名称
     * H5通过 window.AndroidBridge 调用原生方法
     */
    const val JS_INTERFACE_NAME = "AndroidBridge"

    /**
     * SharedPreferences文件名
     */
    const val SP_NAME = "cashcredit_prefs"

    /**
     * 隐私政策已同意的Key
     */
    const val KEY_PRIVACY_AGREED = "privacy_agreed"

    /**
     * 缓存的H5 URL Key
     */
    const val KEY_CACHED_H5_URL = "cached_h5_url"

    /**
     * 缓存的API URL Key
     */
    const val KEY_CACHED_API_URL = "cached_api_url"

    /**
     * 隐私政策URL（Google Play要求）
     * TODO: 替换为实际隐私政策URL
     */
    const val PRIVACY_POLICY_URL = "https://example.com/privacy"
}