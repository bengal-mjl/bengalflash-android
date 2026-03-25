package com.example.cashcredit.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.format.Formatter
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * 设备信息工具类
 * 用于获取设备硬件和系统信息
 */
object DeviceUtil {

    /**
     * 检查是否有电话状态权限
     */
    fun hasPhonePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查是否有位置权限
     */
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 获取设备型号
     */
    fun getDeviceModel(): String {
        return Build.MODEL ?: "Unknown"
    }

    /**
     * 获取设备品牌
     */
    fun getDeviceBrand(): String {
        return Build.BRAND ?: "Unknown"
    }

    /**
     * 获取设备制造商
     */
    fun getDeviceManufacturer(): String {
        return Build.MANUFACTURER ?: "Unknown"
    }

    /**
     * 获取Android系统版本
     */
    fun getAndroidVersion(): String {
        return Build.VERSION.RELEASE ?: "Unknown"
    }

    /**
     * 获取Android SDK版本号
     */
    fun getSdkVersion(): Int {
        return Build.VERSION.SDK_INT
    }

    /**
     * 获取设备唯一标识
     * 注意：此方法使用ANDROID_ID，在某些设备上可能为null或重复
     */
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        return androidId ?: UUID.randomUUID().toString()
    }

    /**
     * 获取设备基本信息（JSON格式字符串）
     */
    fun getDeviceInfoJson(context: Context): String {
        return """
            {
                "brand": "${getDeviceBrand()}",
                "model": "${getDeviceModel()}",
                "manufacturer": "${getDeviceManufacturer()}",
                "androidVersion": "${getAndroidVersion()}",
                "sdkVersion": ${getSdkVersion()},
                "deviceId": "${getDeviceId(context)}"
            }
        """.trimIndent()
    }

    /**
     * 获取完整设备信息JSON
     */
    fun getFullDeviceInfoJson(context: Context): String {
        return try {
            val info = getFullDeviceInfo(context)
            buildDeviceInfoJson(info)
        } catch (e: Exception) {
            """{"success": false, "error": "${e.message}"}"""
        }
    }

    /**
     * 获取完整设备信息
     */
    private fun getFullDeviceInfo(context: Context): DeviceInfo {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return DeviceInfo(
            // 基本信息
            brand = Build.BRAND ?: "Unknown",
            model = Build.MODEL ?: "Unknown",
            manufacturer = Build.MANUFACTURER ?: "Unknown",

            // CPU信息
            cpuArchitecture = getCpuArchitecture(),
            cpuCoreCount = Runtime.getRuntime().availableProcessors(),
            cpuFrequency = getCpuFrequency(),

            // 内存信息
            totalMemory = getTotalMemory(context, activityManager),

            // 存储信息
            romTotalSize = getRomTotalSize(context),
            sdCardTotalSize = getSdCardTotalSize(context),

            // 系统信息
            osVersion = Build.VERSION.RELEASE ?: "Unknown",
            sdkVersion = Build.VERSION.SDK_INT,
            bootTime = getBootTime(),

            // 设备标识
            imei = getImei(context, telephonyManager),
            meid = getMeid(context, telephonyManager),
            androidId = getAndroidId(context),
            oaid = "需要MSA SDK支持", // OAID需要移动安全联盟SDK
            aaid = "需要Google Play Services",
            macAddress = getMacAddress(context),
            guid = getGuid(context),

            // 屏幕信息
            screenWidth = displayMetrics.widthPixels,
            screenHeight = displayMetrics.heightPixels,
            screenDensity = displayMetrics.densityDpi,

            // 网络信息
            ipAddress = getIpAddress(),
            networkType = getNetworkType(connectivityManager),
            carrierName = getCarrierName(telephonyManager),
            wifiEnabled = isWifiEnabled(wifiManager),
            wifiSsid = getWifiSsid(context, wifiManager),
            wifiBssid = getWifiBssid(context, wifiManager)
        )
    }

    /**
     * 获取CPU架构
     */
    private fun getCpuArchitecture(): String {
        return when (Build.SUPPORTED_ABIS?.firstOrNull()) {
            "arm64-v8a" -> "ARM64"
            "armeabi-v7a" -> "ARM32"
            "x86_64" -> "X86_64"
            "x86" -> "X86"
            else -> Build.SUPPORTED_ABIS?.firstOrNull() ?: "Unknown"
        }
    }

    /**
     * 获取CPU频率
     */
    private fun getCpuFrequency(): String {
        return try {
            val maxFreq = BufferedReader(FileReader("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")).use {
                it.readLine()?.trim()
            }
            if (maxFreq != null && maxFreq.isNotEmpty()) {
                val freq = maxFreq.toLong() / 1000 // 转换为MHz
                "${freq} MHz"
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * 获取总内存大小
     */
    private fun getTotalMemory(context: Context, activityManager: ActivityManager): String {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return formatFileSize(context, memoryInfo.totalMem)
    }

    /**
     * 获取ROM总大小
     */
    private fun getRomTotalSize(context: Context): String {
        return try {
            val statFs = StatFs(Environment.getDataDirectory().path)
            val totalBytes = statFs.blockCountLong * statFs.blockSizeLong
            formatFileSize(context, totalBytes)
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * 获取SD卡总大小
     */
    private fun getSdCardTotalSize(context: Context): String {
        return try {
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                val statFs = StatFs(Environment.getExternalStorageDirectory().path)
                val totalBytes = statFs.blockCountLong * statFs.blockSizeLong
                formatFileSize(context, totalBytes)
            } else {
                "未挂载"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * 获取开机时间
     */
    private fun getBootTime(): String {
        val bootTime = System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime()
        return Date(bootTime).toString()
    }

    /**
     * 获取IMEI
     */
    @SuppressLint("HardwareIds")
    private fun getImei(context: Context, telephonyManager: TelephonyManager): String {
        return try {
            if (!hasPhonePermission(context)) {
                return "需要权限"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telephonyManager.imei ?: "Unknown"
            } else {
                @Suppress("DEPRECATION")
                telephonyManager.deviceId ?: "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * 获取MEID
     */
    @SuppressLint("HardwareIds")
    private fun getMeid(context: Context, telephonyManager: TelephonyManager): String {
        return try {
            if (!hasPhonePermission(context)) {
                return "需要权限"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telephonyManager.meid ?: "Unknown"
            } else {
                "Android 8.0以下不支持"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * 获取Android ID
     */
    @SuppressLint("HardwareIds")
    private fun getAndroidId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "Unknown"
    }

    /**
     * 获取MAC地址
     */
    @SuppressLint("HardwareIds")
    private fun getMacAddress(context: Context): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            wifiInfo?.macAddress ?: "02:00:00:00:00:00"
        } catch (e: Exception) {
            // Android 6.0+ 无法获取真实MAC地址
            "02:00:00:00:00:00"
        }
    }

    /**
     * 获取GUID
     */
    private fun getGuid(context: Context): String {
        val sp = context.getSharedPreferences("device_info", Context.MODE_PRIVATE)
        var guid = sp.getString("guid", null)
        if (guid == null) {
            guid = UUID.randomUUID().toString()
            sp.edit().putString("guid", guid).apply()
        }
        return guid
    }

    /**
     * 获取IP地址
     */
    private fun getIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress ?: "Unknown"
                    }
                }
            }
            "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * 获取网络类型
     */
    private fun getNetworkType(connectivityManager: ConnectivityManager): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return "无网络"
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Unknown"
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "移动网络"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "以太网"
                    else -> "Unknown"
                }
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo ?: return "无网络"
                when (networkInfo.type) {
                    ConnectivityManager.TYPE_WIFI -> "Wi-Fi"
                    ConnectivityManager.TYPE_MOBILE -> "移动网络"
                    ConnectivityManager.TYPE_ETHERNET -> "以太网"
                    else -> "Unknown"
                }
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * 获取运营商名称
     */
    private fun getCarrierName(telephonyManager: TelephonyManager): String {
        return try {
            telephonyManager.networkOperatorName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * 判断WiFi是否开启
     */
    private fun isWifiEnabled(wifiManager: WifiManager): Boolean {
        return try {
            wifiManager.isWifiEnabled
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取WiFi SSID
     */
    @SuppressLint("MissingPermission")
    private fun getWifiSsid(context: Context, wifiManager: WifiManager): String {
        return try {
            if (!hasLocationPermission(context)) {
                return "需要位置权限"
            }
            val wifiInfo = wifiManager.connectionInfo
            val ssid = wifiInfo?.ssid
            if (ssid != null && ssid != "<unknown ssid>") {
                ssid.replace("\"", "")
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * 获取WiFi BSSID
     */
    @SuppressLint("MissingPermission")
    private fun getWifiBssid(context: Context, wifiManager: WifiManager): String {
        return try {
            if (!hasLocationPermission(context)) {
                return "需要位置权限"
            }
            wifiManager.connectionInfo?.bssid ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * 格式化文件大小
     */
    private fun formatFileSize(context: Context, bytes: Long): String {
        return Formatter.formatFileSize(context, bytes)
    }

    /**
     * 构建JSON字符串
     */
    private fun buildDeviceInfoJson(info: DeviceInfo): String {
        return """
            {
                "success": true,
                "basicInfo": {
                    "brand": "${escapeJson(info.brand)}",
                    "model": "${escapeJson(info.model)}",
                    "manufacturer": "${escapeJson(info.manufacturer)}"
                },
                "cpuInfo": {
                    "architecture": "${escapeJson(info.cpuArchitecture)}",
                    "coreNumber": ${info.cpuCoreCount},
                    "frequency": "${escapeJson(info.cpuFrequency)}"
                },
                "memoryInfo": {
                    "totalMemory": "${escapeJson(info.totalMemory)}"
                },
                "storageInfo": {
                    "romTotalSize": "${escapeJson(info.romTotalSize)}",
                    "sdCardTotalSize": "${escapeJson(info.sdCardTotalSize)}"
                },
                "systemInfo": {
                    "version": {
                        "release": "${escapeJson(info.osVersion)}",
                        "sdkInt": ${info.sdkVersion}
                    },
                    "bootTime": "${escapeJson(info.bootTime)}"
                },
                "identifiers": {
                    "imei": "${escapeJson(info.imei)}",
                    "meid": "${escapeJson(info.meid)}",
                    "androidId": "${escapeJson(info.androidId)}",
                    "oaid": "${escapeJson(info.oaid)}",
                    "aaid": "${escapeJson(info.aaid)}",
                    "macAddress": "${escapeJson(info.macAddress)}",
                    "guid": "${escapeJson(info.guid)}"
                },
                "screenInfo": {
                    "width": ${info.screenWidth},
                    "height": ${info.screenHeight},
                    "densityDpi": ${info.screenDensity}
                },
                "networkInfo": {
                    "ipAddress": "${escapeJson(info.ipAddress)}",
                    "networkType": "${escapeJson(info.networkType)}",
                    "carrierName": "${escapeJson(info.carrierName)}",
                    "wifi": {
                        "enabled": ${info.wifiEnabled},
                        "ssid": "${escapeJson(info.wifiSsid)}",
                        "bssid": "${escapeJson(info.wifiBssid)}"
                    }
                }
            }
        """.trimIndent()
    }

    /**
     * 转义JSON特殊字符
     */
    private fun escapeJson(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * 设备信息数据类
     */
    private data class DeviceInfo(
        val brand: String,
        val model: String,
        val manufacturer: String,
        val cpuArchitecture: String,
        val cpuCoreCount: Int,
        val cpuFrequency: String,
        val totalMemory: String,
        val romTotalSize: String,
        val sdCardTotalSize: String,
        val osVersion: String,
        val sdkVersion: Int,
        val bootTime: String,
        val imei: String,
        val meid: String,
        val androidId: String,
        val oaid: String,
        val aaid: String,
        val macAddress: String,
        val guid: String,
        val screenWidth: Int,
        val screenHeight: Int,
        val screenDensity: Int,
        val ipAddress: String,
        val networkType: String,
        val carrierName: String,
        val wifiEnabled: Boolean,
        val wifiSsid: String,
        val wifiBssid: String
    )
}