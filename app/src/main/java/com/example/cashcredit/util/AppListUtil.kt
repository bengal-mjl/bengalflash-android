package com.example.cashcredit.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build

/**
 * 应用列表工具类
 * 用于获取设备上已安装的应用列表
 * 使用 queries 方式查询应用（Android 11+）
 */
object AppListUtil {

    /**
     * 预定义的查询包名列表
     */
    private val QUERY_PACKAGES = listOf(
        "com.android.vending",           // Google Play Store
        "com.android.chrome",            // Chrome
        "com.facebook.katana",           // Facebook
        "com.whatsapp",                  // WhatsApp
        "com.tencent.mm",                // 微信
        "com.eg.android.AlipayGphone",   // 支付宝
        "com.android.providers.calendar",// 日历
        "com.google.android.gms",        // Google Play Services
        "com.instagram.android"          // Instagram
    )

    /**
     * 获取已安装应用列表（使用 queries 方式）
     * @param context 上下文
     * @return JSON格式字符串
     */
    fun getInstalledAppsJson(context: Context): String {
        return try {
            val apps = getInstalledApps(context)
            buildAppsJson(apps)
        } catch (e: Exception) {
            """{"success": false, "error": "${e.message}", "count": 0, "apps": []}"""
        }
    }

    /**
     * 获取已安装应用列表（使用 queries 方式查询）
     * @param context 上下文
     * @return 应用信息列表
     */
    private fun getInstalledApps(context: Context): List<AppInfo> {
        val packageManager = context.packageManager
        val appMap = mutableMapOf<String, AppInfo>()

        // 方式1：查询预定义包名的应用
        queryPackagesByName(packageManager, appMap)

        // 方式2：通过 Intent 查询能处理特定 Action 的应用
        queryPackagesByIntent(packageManager, appMap)

        // 方式3：通过 MIME Type 查询
        queryPackagesByMimeType(packageManager, appMap)

        // 方式4：通过 Scheme 查询
        queryPackagesByScheme(packageManager, appMap)

        return appMap.values.sortedBy { it.appName.lowercase() }
    }

    /**
     * 通过包名查询预定义的应用
     */
    private fun queryPackagesByName(packageManager: PackageManager, appMap: MutableMap<String, AppInfo>) {
        for (packageName in QUERY_PACKAGES) {
            try {
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(packageName, 0)
                }

                val appInfo = packageInfo.applicationInfo
                if (appInfo != null && !appMap.containsKey(packageName)) {
                    appMap[packageName] = AppInfo(
                        packageName = packageName,
                        appName = appInfo.loadLabel(packageManager).toString(),
                        versionName = packageInfo.versionName ?: "Unknown",
                        versionCode = getVersionCode(packageInfo),
                        isSystemApp = isSystemApp(appInfo)
                    )
                }
            } catch (e: PackageManager.NameNotFoundException) {
                // 应用未安装，跳过
            } catch (e: Exception) {
                // 其他异常，跳过
            }
        }
    }

    /**
     * 通过 Intent Action 查询应用
     */
    private fun queryPackagesByIntent(packageManager: PackageManager, appMap: MutableMap<String, AppInfo>) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, 0)
            }
            processResolveInfos(packageManager, resolveInfos, appMap)
        } catch (e: Exception) {
            // 忽略异常
        }
    }

    /**
     * 通过 MIME Type 查询应用
     */
    private fun queryPackagesByMimeType(packageManager: PackageManager, appMap: MutableMap<String, AppInfo>) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = "*/*"
            }
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, 0)
            }
            processResolveInfos(packageManager, resolveInfos, appMap)
        } catch (e: Exception) {
            // 忽略异常
        }
    }

    /**
     * 通过 Scheme 查询应用
     */
    private fun queryPackagesByScheme(packageManager: PackageManager, appMap: MutableMap<String, AppInfo>) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com"))
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, 0)
            }
            processResolveInfos(packageManager, resolveInfos, appMap)
        } catch (e: Exception) {
            // 忽略异常
        }
    }

    /**
     * 处理 ResolveInfo 列表，提取应用信息
     */
    private fun processResolveInfos(
        packageManager: PackageManager,
        resolveInfos: List<ResolveInfo>,
        appMap: MutableMap<String, AppInfo>
    ) {
        for (resolveInfo in resolveInfos) {
            try {
                val packageName = resolveInfo.activityInfo?.packageName ?: continue
                if (appMap.containsKey(packageName)) continue

                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(packageName, 0)
                }

                val appInfo = packageInfo.applicationInfo ?: continue
                appMap[packageName] = AppInfo(
                    packageName = packageName,
                    appName = appInfo.loadLabel(packageManager).toString(),
                    versionName = packageInfo.versionName ?: "Unknown",
                    versionCode = getVersionCode(packageInfo),
                    isSystemApp = isSystemApp(appInfo)
                )
            } catch (e: Exception) {
                // 忽略单个应用的异常
            }
        }
    }

    /**
     * 判断是否为系统应用
     */
    private fun isSystemApp(applicationInfo: android.content.pm.ApplicationInfo): Boolean {
        return (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
    }

    /**
     * 获取版本号（兼容不同版本）
     */
    private fun getVersionCode(packageInfo: android.content.pm.PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }

    /**
     * 构建JSON字符串
     */
    private fun buildAppsJson(apps: List<AppInfo>): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("{\n")
        stringBuilder.append("    \"success\": true,\n")
        stringBuilder.append("    \"count\": ${apps.size},\n")
        stringBuilder.append("    \"apps\": [\n")

        apps.forEachIndexed { index, app ->
            stringBuilder.append("        {\n")
            stringBuilder.append("            \"packageName\": \"${escapeJson(app.packageName)}\",\n")
            stringBuilder.append("            \"appName\": \"${escapeJson(app.appName)}\",\n")
            stringBuilder.append("            \"versionName\": \"${escapeJson(app.versionName)}\",\n")
            stringBuilder.append("            \"versionCode\": ${app.versionCode},\n")
            stringBuilder.append("            \"isSystemApp\": ${app.isSystemApp}\n")
            stringBuilder.append("        }")
            if (index < apps.size - 1) {
                stringBuilder.append(",")
            }
            stringBuilder.append("\n")
        }

        stringBuilder.append("    ]\n")
        stringBuilder.append("}")
        return stringBuilder.toString()
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
     * 应用信息数据类
     */
    private data class AppInfo(
        val packageName: String,
        val appName: String,
        val versionName: String,
        val versionCode: Long,
        val isSystemApp: Boolean
    )
}