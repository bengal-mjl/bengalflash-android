package com.example.cashcredit

import android.app.Application
import android.content.Context
import android.os.Build
import android.webkit.WebView
import com.example.cashcredit.config.AppConfig
import com.example.cashcredit.network.RetrofitClient
import com.example.cashcredit.util.AppDeviceInfo
import com.example.cashcredit.util.LanguageUtil

/**
 * 应用程序入口类
 * 负责全局初始化和WebView预热
 */
class AppApplication : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LanguageUtil.attachBaseContext(base))
    }

    override fun onCreate() {
        super.onCreate()
        initDeviceInfo()

    }

    /**
     * 初始化设备信息
     * 必须在其他初始化之前调用，确保设备信息可用
     */
    private fun initDeviceInfo() {
        AppDeviceInfo.init(this)
    }



    companion object {
        /**
         * 获取Application实例
         */
        fun getInstance(context: Context): AppApplication {
            return context.applicationContext as AppApplication
        }
    }
}