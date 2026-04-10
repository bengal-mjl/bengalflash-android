package com.example.cashcredit.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import java.util.UUID

/**
 * 应用设备信息管理类
 * 用于管理H5和API通用的设备信息
 *
 * 设备信息格式：
 * {
 *   "appTerminal": "ANDROID",
 *   "appVersion": "1.0.0.0",
 *   "uuid": "唯一设备标识",
 *   "gid": "全局唯一标识",
 *   "phoneTerminal": "ANDROID"
 * }
 */
object AppDeviceInfo {

    /**
     * 应用终端类型
     */
    const val APP_TERMINAL = "ANDROID"

    /**
     * 手机终端类型
     */
    const val PHONE_TERMINAL = "Android_Office"

    /**
     * 渠道号
     */
    const val CHANNEL_NO = "BD_ANDROID"

    /**
     * SharedPreferences文件名
     */
    private const val SP_NAME = "app_device_info"

    /**
     * UUID存储Key
     */
    private const val KEY_UUID = "device_uuid"

    /**
     * GID存储Key
     */
    private const val KEY_GID = "device_gid"

    /**
     * 语言设置存储Key
     */
    private const val KEY_LANGUAGE_SET = "language_set"

    // 缓存的设备信息
    private var cachedDeviceInfo: DeviceInfo? = null

    // 缓存的语言设置
    private var cachedLanguageSet: String? = null

    /**
     * 设备信息数据类
     */
    data class DeviceInfo(
        val appTerminal: String,
        val appVersion: String,
        val uuid: String,
        val gid: String,
        val phoneTerminal: String,
        val channelNo: String
    )

    /**
     * 初始化设备信息
     * 在Application中调用，提前缓存信息
     */
    fun init(context: Context) {
        cachedDeviceInfo = DeviceInfo(
            appTerminal = APP_TERMINAL,
            appVersion = getAppVersion(context),
            uuid = getOrCreateUuid(context),
            gid = getOrCreateGid(context),
            phoneTerminal = PHONE_TERMINAL,
            channelNo = CHANNEL_NO
        )

        // 初始化时也加载语言设置
        val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        cachedLanguageSet = sp.getString(KEY_LANGUAGE_SET, null)
    }

    /**
     * 获取设备信息对象
     */
    fun getDeviceInfo(): DeviceInfo {
        return cachedDeviceInfo ?: throw IllegalStateException("AppDeviceInfo not initialized. Call init() first.")
    }

    /**
     * 获取设备信息JSON字符串
     * 用于H5和API调用
     */
    fun getDeviceInfoJson(): String {
        val info = getDeviceInfo()
        return """
            {
                "appTerminal": "${info.appTerminal}",
                "appVersion": "${info.appVersion}",
                "uuid": "${info.uuid}",
                "gid": "${info.gid}",
                "phoneTerminal": "${info.phoneTerminal}",
                "barHeight": "${BarUtil.getStatusBarHeight()}",
                "channel": "Android_Official",
                "channelNo": "$CHANNEL_NO"
            }
        """.trimIndent()
    }

    /**
     * 获取应用版本号
     */
    fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    /**
     * 获取或创建UUID
     * 基于Android ID生成，首次生成后持久化存储
     */
    fun getOrCreateUuid(context: Context): String {
        val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

        // 先尝试从缓存获取
        cachedDeviceInfo?.uuid?.let { return it }

        // 尝试从存储中获取
        var uuid = sp.getString(KEY_UUID, null)

        if (uuid.isNullOrEmpty()) {
            // 生成新的UUID
            val androidId = try {
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                )
            } catch (e: Exception) {
                null
            }

            uuid = if (!androidId.isNullOrEmpty() && androidId != "9774d56d682e549c") {
                // 基于Android ID生成稳定的UUID
                UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()
            } else {
                // 生成随机UUID并存储
                UUID.randomUUID().toString()
            }

            // 持久化存储
            sp.edit().putString(KEY_UUID, uuid).apply()
        }

        return uuid
    }

    /**
     * 获取或创建GID（全局唯一标识）
     * 用于追踪用户设备，首次生成后持久化存储
     */
    fun getOrCreateGid(context: Context): String {
        val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

        // 先尝试从缓存获取
        cachedDeviceInfo?.gid?.let { return it }

        // 尝试从存储中获取
        var gid = sp.getString(KEY_GID, null)

        if (gid.isNullOrEmpty()) {
            // 生成新的GID
            gid = UUID.randomUUID().toString().replace("-", "")

            // 持久化存储
            sp.edit().putString(KEY_GID, gid).apply()
        }

        return gid
    }

    /**
     * 获取appTerminal
     */
    fun getAppTerminal(): String = APP_TERMINAL

    /**
     * 获取appVersion
     */
    fun getAppVersion(): String = getDeviceInfo().appVersion

    /**
     * 获取uuid
     */
    fun getUuid(): String = getDeviceInfo().uuid

    /**
     * 获取gid
     */
    fun getGid(): String = getDeviceInfo().gid

    /**
     * 获取phoneTerminal
     */
    fun getPhoneTerminal(): String = PHONE_TERMINAL

    /**
     * 获取channelNo
     */
    fun getChannelNo(): String = CHANNEL_NO

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = cachedDeviceInfo != null

    /**
     * 设置语言
     * 由H5调用，存储语言设置
     */
    fun setLanguageSet(context: Context, language: String) {
        cachedLanguageSet = language
        val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_LANGUAGE_SET, language).apply()
    }

    /**
     * 获取语言设置
     */
    fun getLanguageSet(context: Context): String {
        // 先从缓存获取
        cachedLanguageSet?.let { return it }

        // 从存储中获取
        val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        val language = sp.getString(KEY_LANGUAGE_SET, "") ?: ""
        cachedLanguageSet = language
        return language
    }

    /**
     * 获取语言设置（需要已初始化）
     */
    fun getLanguageSet(): String {
        return cachedLanguageSet ?: ""
    }

    /**
     * 检查是否有语言设置
     */
    fun hasLanguageSet(): Boolean = !cachedLanguageSet.isNullOrEmpty()
}