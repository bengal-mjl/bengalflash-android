package com.example.cashcredit.model

import com.google.gson.annotations.SerializedName

/**
 * 设备上传数据模型
 */
data class DeviceUploadData(
    val basic: BasicInfo,
    val location: LocationInfo?,
    val networkInfo: NetworkInfoData,
    val installedApps: List<AppInfoData>,
    val sensorList: List<SensorInfoData>,
    val sms: List<SmsInfoData>
)

/**
 * 基本信息部分
 */
data class BasicInfo(
    val deviceBase: DeviceBaseInfo,
    val general: GeneralInfo,
    val locale: LocaleInfo,
    val screen: ScreenInfoData,
    val storage: StorageInfoData,
    val battery: BatteryInfoData
)

/**
 * 设备基础信息
 */
data class DeviceBaseInfo(
    @SerializedName("build_name")
    val buildName: String,
    val brand: String,
    val model: String,
    @SerializedName("device_name")
    val deviceName: String,
    @SerializedName("sdk_version")
    val sdkVersion: Int,
    val release: String,
    @SerializedName("serial_number")
    val serialNumber: String,
    @SerializedName("android_id")
    val androidId: String,
    @SerializedName("phone_type")
    val phoneType: Int,
    val MANUFACTURER: String,
    val FINGERPRINT: String,
    val gaid: String?,
    val cores: Int,
    @SerializedName("build_time")
    val buildTime: Long,
    val board: String,
    val isEmulator: Int,
    val deviceUUID: String,
    @SerializedName("currentSystemTime")
    val currentSystemTime: Long,
    val createTime: Long
)

/**
 * 通用信息
 */
data class GeneralInfo(
    @SerializedName("is_root_jailbreak")
    val isRootJailbreak: Int,
    @SerializedName("can_call_phone")
    val canCallPhone: Int,
    @SerializedName("keyboard_type")
    val keyboardType: Int,
    @SerializedName("is_using_proxy_port")
    val isUsingProxyPort: Int,
    @SerializedName("is_using_vpn")
    val isUsingVpn: Int,
    @SerializedName("is_usb_debug")
    val isUsbDebug: Int,
    @SerializedName("last_boot_time")
    val lastBootTime: Long,
    @SerializedName("is_mock_location")
    val isMockLocation: Int,
    val elapsedRealtime: Long,
    val uptimeMillis: Long
)

/**
 * 本地化信息
 */
data class LocaleInfo(
    @SerializedName("locale_iso_3_language")
    val localeIso3Language: String,
    @SerializedName("locale_display_language")
    val localeDisplayLanguage: String,
    @SerializedName("locale_iso_3_country")
    val localeIso3Country: String,
    val language: String,
    @SerializedName("time_zone_id")
    val timeZoneId: String,
    @SerializedName("sim_country_iso")
    val simCountryIso: String,
    @SerializedName("network_operator_name")
    val networkOperatorName: String
)

/**
 * 屏幕信息
 */
data class ScreenInfoData(
    val widthPixels: Int,
    val heightPixels: Int,
    val xdpi: String,
    val ydpi: String,
    val densityDpi: Int,
    val density: String,
    val scaledDensity: String,
    @SerializedName("physicalSize")
    val physicalSize: Double
)

/**
 * 存储信息
 */
data class StorageInfoData(
    @SerializedName("ram_total_size")
    val ramTotalSize: Long,
    @SerializedName("ram_usable_size")
    val ramUsableSize: Long,
    @SerializedName("internal_storage_usable")
    val internalStorageUsable: Long,
    @SerializedName("internal_storage_total")
    val internalStorageTotal: Long,
    @SerializedName("memory_card_size")
    val memoryCardSize: Long,
    @SerializedName("memory_card_size_use")
    val memoryCardSizeUse: Long,
    @SerializedName("storage_dir_size")
    val storageDirSize: Long,
    @SerializedName("storage_dir_size_usable")
    val storageDirSizeUsable: Long
)

/**
 * 电池信息
 */
data class BatteryInfoData(
    @SerializedName("is_charging")
    val isCharging: Int,
    @SerializedName("battery_pct")
    val batteryPct: Int,
    @SerializedName("is_usb_charge")
    val isUsbCharge: Int,
    @SerializedName("is_ac_charge")
    val isAcCharge: Int,
    @SerializedName("battery_level")
    val batteryLevel: Int,
    @SerializedName("battery_max")
    val batteryMax: Int
)

/**
 * 位置信息
 */
data class LocationInfo(
    val latitude: String,
    val longitude: String,
    val accuracy: String,
    val time: Long,
    val address: String,
    val addressObject: AddressObject?
)

/**
 * 地址对象
 */
data class AddressObject(
    @SerializedName("country_name")
    val countryName: String?,
    @SerializedName("country_code")
    val countryCode: String?,
    @SerializedName("admin_area")
    val adminArea: String?,
    val locality: String?,
    @SerializedName("sub_admin_area")
    val subAdminArea: String?,
    @SerializedName("feature_name")
    val featureName: String?,
    val address: List<String>?
)

/**
 * 网络信息
 */
data class NetworkInfoData(
    @SerializedName("network_type")
    val networkType: String,
    val ip: String?,
    val mac: String?,
    @SerializedName("currentWifi")
    val currentWifi: CurrentWifiInfo?,
    @SerializedName("configuredWifi")
    val configuredWifi: List<ConfiguredWifiInfo>?
)

/**
 * 当前WiFi信息
 */
data class CurrentWifiInfo(
    val ssid: String,
    val bssid: String
)

/**
 * 已配置WiFi信息
 */
data class ConfiguredWifiInfo(
    val ssid: String,
    val bssid: String?
)

/**
 * 应用信息数据
 */
data class AppInfoData(
    @SerializedName("app_name")
    val appName: String,
    val `package`: String,
    @SerializedName("in_time")
    val inTime: Long,
    @SerializedName("up_time")
    val upTime: Long,
    @SerializedName("version_name")
    val versionName: String,
    @SerializedName("version_code")
    val versionCode: Long,
    @SerializedName("app_type")
    val appType: Int,
    val flags: Int
)

/**
 * 传感器信息数据
 */
data class SensorInfoData(
    val type: Int,
    val name: String,
    val version: Int,
    val vendor: String,
    val maxRange: String,
    val minDelay: Int,
    val power: String,
    val resolution: String
)

/**
 * 短信信息数据
 */
data class SmsInfoData(
    @SerializedName("_id")
    val id: Long,
    @SerializedName("thread_id")
    val threadId: Long,
    val address: String,
    val person: String,
    val body: String,
    val date: Long,
    val type: Int,
    val protocol: Int?,
    val read: Int,
    val status: Int?,
    val subject: String?,
    @SerializedName("date_sent")
    val dateSent: Long?,
    val seen: Int?
)

/**
 * 上传设备数据响应
 */
data class UploadDeviceResponse(
    @SerializedName("recordNo")
    val recordNo: String?,
    @SerializedName("customerNo")
    val customerNo: String?
)