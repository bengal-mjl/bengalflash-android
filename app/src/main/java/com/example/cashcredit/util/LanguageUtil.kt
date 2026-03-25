package com.example.cashcredit.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * 语言工具类
 * 处理应用语言国际化，跟随系统语言
 */
object LanguageUtil {

    /**
     * 附加BaseContext配置，用于Activity
     * 在Activity的attachBaseContext中调用
     */
    fun attachBaseContext(context: Context): Context {
        val locale = getSystemLocale()
        Locale.setDefault(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val config = Configuration()
            config.setLocale(locale)
            config.setLocales(LocaleList(locale))
            context.createConfigurationContext(config)
        } else {
            val config = Configuration()
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }

    /**
     * 更新应用的语言配置
     * 在Application和Activity中调用
     */
    fun updateConfiguration(context: Context): Context {
        val locale = getSystemLocale()
        Locale.setDefault(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val config = Configuration()
            config.setLocale(locale)
            config.setLocales(LocaleList(locale))
            context.createConfigurationContext(config)
        } else {
            val config = Configuration()
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }

    /**
     * 获取系统当前语言Locale
     */
    private fun getSystemLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList.getDefault()
            localeList.get(0)
        } else {
            @Suppress("DEPRECATION")
            Locale.getDefault()
        }
    }

    /**
     * 获取当前语言代码
     * @return 语言代码，如 "zh", "en"
     */
    fun getCurrentLanguage(): String {
        return getSystemLocale().language
    }

    /**
     * 判断是否为中文环境
     */
    fun isChinese(): Boolean {
        return getCurrentLanguage().startsWith("zh")
    }
}