package com.example.cashcredit.bridge

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cashcredit.config.AppConfig
import com.example.cashcredit.repository.ImageRepository
import com.example.cashcredit.ui.TakePhotoActivity
import com.example.cashcredit.util.AppDeviceInfo
import com.example.cashcredit.util.AppListUtil
import com.example.cashcredit.util.CameraCallbackManager
import com.example.cashcredit.util.ContactPickerManager
import com.example.cashcredit.util.ContactUtil
import com.example.cashcredit.util.DeviceUploadManager
import com.example.cashcredit.util.DeviceUtil
import com.example.cashcredit.util.NetworkUtil
import com.example.cashcredit.util.PermissionDialogManager
import com.example.cashcredit.util.PermissionHelper
import com.example.cashcredit.util.SmsUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import wendu.dsbridge.CompletionHandler
import wendu.dsbridge.DWebView

/**
 * DSBridge JS接口类
 * 使用DSBridge暴露方法给H5调用
 *
 * H5调用方式:
 * 同步: var result = dsBridge.call("methodName", arg)
 * 异步: dsBridge.call("methodName", arg, function(result) {})
 */
class DSBridgeInterface(private val context: Context) {

    /**
     * 显示Toast消息
     * H5调用: dsBridge.call("showToast", "消息内容")
     */
    @JavascriptInterface
    fun showToast(args: Any?) {
        val message = args?.toString() ?: ""
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * 显示长时间Toast消息
     * H5调用: dsBridge.call("showLongToast", "消息内容")
     */
    @JavascriptInterface
    fun showLongToast(args: Any?) {
        val message = args?.toString() ?: ""
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /**
     * 获取设备信息 (同步方法)
     * H5调用: var info = dsBridge.call("getDeviceInfo")
     */
    @JavascriptInterface
    fun getDeviceInfo(args: Any?): String {
        return DeviceUtil.getDeviceInfoJson(context)
    }

    /**
     * 获取完整设备信息 (同步方法)
     * H5调用: var info = dsBridge.call("getFullDeviceInfo")
     * 返回包含CPU、内存、存储、屏幕、网络等详细信息
     */
    @JavascriptInterface
    fun getFullDeviceInfo(args: Any?): String {
        return DeviceUtil.getFullDeviceInfoJson(context)
    }

    /**
     * 检查设备信息权限状态 (同步方法)
     * H5调用: var result = dsBridge.call("checkDevicePermission")
     * 返回格式: { "phoneGranted": true/false, "locationGranted": true/false }
     */
    @JavascriptInterface
    fun checkDevicePermission(args: Any?): String {
        val phoneGranted = DeviceUtil.hasPhonePermission(context)
        val locationGranted = DeviceUtil.hasLocationPermission(context)
        return """{"phoneGranted": $phoneGranted, "locationGranted": $locationGranted}"""
    }

    /**
     * 请求设备信息权限 (异步方法)
     * H5调用: dsBridge.call("requestDevicePermission", null, function(result) {})
     * 返回格式: { "phoneGranted": true/false, "locationGranted": true/false }
     */
    @JavascriptInterface
    fun requestDevicePermission(args: Any?, handler: CompletionHandler<String>) {
        if (context is android.app.Activity) {
            val activity = context as android.app.Activity
            PermissionHelper.requestDevicePermission(activity, handler)
        } else {
            handler.complete("""{"phoneGranted": false, "locationGranted": false, "error": "Context is not Activity"}""")
        }
    }

    /**
     * 获取网络状态 (同步方法)
     * H5调用: var state = dsBridge.call("getNetworkState")
     */
    @JavascriptInterface
    fun getNetworkState(args: Any?): String {
        return NetworkUtil.getNetworkStateJson(context)
    }

    /**
     * 检查网络是否连接 (同步方法)
     * H5调用: var connected = dsBridge.call("isNetworkConnected")
     */
    @JavascriptInterface
    fun isNetworkConnected(args: Any?): Boolean {
        return NetworkUtil.isNetworkConnected(context)
    }

    /**
     * 获取应用版本信息 (同步方法)
     * H5调用: var version = dsBridge.call("getAppVersion")
     */
    @JavascriptInterface
    fun getAppVersion(args: Any?): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            """
                {
                    "versionName": "${packageInfo.versionName}",
                    "versionCode": ${if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) packageInfo.longVersionCode else @Suppress("DEPRECATION") packageInfo.versionCode}
                }
            """.trimIndent()
        } catch (e: Exception) {
            """{"error": "${e.message}"}"""
        }
    }

    /**
     * 获取已安装应用列表 (同步方法)
     * 只返回用户安装的应用，过滤系统预装应用
     * H5调用: var apps = dsBridge.call("getInstalledApps")
     * 返回格式: { "success": true, "count": 25, "apps": [...] }
     */
    @JavascriptInterface
    fun getInstalledApps(args: Any?): String {
        return AppListUtil.getInstalledAppsJson(context)
    }


    /**
     * 请求短信权限 (异步方法)
     * H5调用: dsBridge.call("requestSmsPermission", null, function(result) {})
     * 返回格式: { "granted": true/false }
     */
    @JavascriptInterface
    fun requestSmsPermission(args: Any?, handler: CompletionHandler<String>) {
        if (context is android.app.Activity) {
            val activity = context as android.app.Activity
            if (SmsUtil.hasSmsPermission(context)) {
                handler.complete("""{"granted": true}""")
            } else {
                // 存储回调，等待权限请求结果
                PermissionHelper.requestSmsPermission(activity, handler)
            }
        } else {
            handler.complete("""{"granted": false, "error": "Context is not Activity"}""")
        }
    }

    /**
     * 获取短信列表 (同步方法)
     * H5调用: var sms = dsBridge.call("getSmsList") 或 dsBridge.call("getSmsList", {"maxCount": 50})
     * 返回格式: { "success": true, "count": 20, "smsList": [...] }
     */
    @JavascriptInterface
    fun getSmsList(args: Any?): String {
        val maxCount = try {
            val jsonObj = args as? org.json.JSONObject
            jsonObj?.optInt("maxCount", 20) ?: 20
        } catch (e: Exception) {
            20
        }
        return SmsUtil.getSmsListJson(context, maxCount)
    }

    /**
     * 检查短信权限状态 (同步方法)
     * H5调用: var result = dsBridge.call("checkSmsPermission")
     * 返回格式: { "granted": true/false }
     */
    @JavascriptInterface
    fun checkSmsPermission(args: Any?): String {
        val granted = SmsUtil.hasSmsPermission(context)
        return """{"granted": $granted}"""
    }

    /**
     * 检查联系人权限状态 (同步方法)
     * H5调用: var result = dsBridge.call("checkContactsPermission")
     * 返回格式: { "granted": true/false }
     */
    @JavascriptInterface
    fun checkContactsPermission(args: Any?): String {
        val granted = ContactUtil.hasContactPermission(context)
        return """{"granted": $granted}"""
    }

    /**
     * 请求联系人权限 (异步方法)
     * H5调用: dsBridge.call("requestContactsPermission", null, function(result) {})
     * 返回格式: { "granted": true/false }
     */
    @JavascriptInterface
    fun requestContactsPermission(args: Any?, handler: CompletionHandler<String>) {
        if (context is android.app.Activity) {
            val activity = context as android.app.Activity
            PermissionHelper.requestContactsPermission(activity, handler)
        } else {
            handler.complete("""{"granted": false, "error": "Context is not Activity"}""")
        }
    }

    /**
     * 获取联系人列表 (同步方法)
     * H5调用: var contacts = dsBridge.call("getContactsList") 或 dsBridge.call("getContactsList", {"maxCount": 100})
     * 返回格式: { "success": true, "count": 50, "contacts": [...] }
     */
    @JavascriptInterface
    fun getContactsList(args: Any?): String {
        val maxCount = try {
            val jsonObj = args as? org.json.JSONObject
            jsonObj?.optInt("maxCount", 50) ?: 50
        } catch (e: Exception) {
            50
        }
        return ContactUtil.getContactListJson(context, maxCount)
    }

    /**
     * 关闭应用
     * H5调用: dsBridge.call("closeApp")
     */
    @JavascriptInterface
    fun closeApp(args: Any?) {
        if (context is android.app.Activity) {
            context.finish()
        }
    }

    /**
     * 检查相机权限状态 (同步方法)
     * H5调用: var result = dsBridge.call("checkCameraPermission")
     * 返回格式: { "granted": true/false }
     */
    @JavascriptInterface
    fun checkCameraPermission(args: Any?): String {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        return """{"granted": $granted}"""
    }

    /**
     * 请求相机权限 (异步方法)
     * H5调用: dsBridge.call("requestCameraPermission", null, function(result) {})
     * 返回格式: { "granted": true/false }
     */
    @JavascriptInterface
    fun requestCameraPermission(args: Any?, handler: CompletionHandler<String>) {
        if (context is android.app.Activity) {
            val activity = context as android.app.Activity
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                handler.complete("""{"granted": true}""")
            } else {
                PermissionHelper.requestCameraPermission(activity, handler)
            }
        } else {
            handler.complete("""{"granted": false, "error": "Context is not Activity"}""")
        }
    }



    /**
     * 获取设备ID (同步方法)
     * H5调用: var deviceId = dsBridge.call("getDeviceId")
     */
    @JavascriptInterface
    fun getDeviceId(args: Any?): String {
        return DeviceUtil.getDeviceId(context)
    }

    /**
     * 设置本地存储数据
     * H5调用: dsBridge.call("setLocalStorage", {key: "key", value: "value"})
     */
    @JavascriptInterface
    fun setLocalStorage(args: Any?) {
        try {
            val jsonObj = args as? org.json.JSONObject
            val key = jsonObj?.optString("key") ?: return
            val value = jsonObj?.optString("value") ?: return
            val sp = context.getSharedPreferences(AppConfig.SP_NAME, Context.MODE_PRIVATE)
            sp.edit().putString(key, value).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 获取本地存储数据 (同步方法)
     * H5调用: var value = dsBridge.call("getLocalStorage", "key")
     */
    @JavascriptInterface
    fun getLocalStorage(args: Any?): String {
        val key = args?.toString() ?: ""
        val sp = context.getSharedPreferences(AppConfig.SP_NAME, Context.MODE_PRIVATE)
        return sp.getString(key, "") ?: ""
    }

    /**
     * 删除本地存储数据
     * H5调用: dsBridge.call("removeLocalStorage", "key")
     */
    @JavascriptInterface
    fun removeLocalStorage(args: Any?) {
        val key = args?.toString() ?: ""
        val sp = context.getSharedPreferences(AppConfig.SP_NAME, Context.MODE_PRIVATE)
        sp.edit().remove(key).apply()
    }

    /**
     * 清空本地存储
     * H5调用: dsBridge.call("clearLocalStorage")
     */
    @JavascriptInterface
    fun clearLocalStorage(args: Any?) {
        val sp = context.getSharedPreferences(AppConfig.SP_NAME, Context.MODE_PRIVATE)
        sp.edit().clear().apply()
    }

    /**
     * 异步方法示例 - 带回调的方法
     * H5调用: dsBridge.call("asyncMethod", args, function(result) {})
     */
    @JavascriptInterface
    fun asyncMethod(args: Any?, handler: CompletionHandler<String>) {
        handler.complete("""{"status": "success", "args": "$args"}""")
    }

    /**
     * 异步获取设备信息
     * H5调用: dsBridge.call("getDeviceInfoAsync", null, function(result) {})
     */
    @JavascriptInterface
    fun getDeviceInfoAsync(args: Any?, handler: CompletionHandler<String>) {
        handler.complete(DeviceUtil.getDeviceInfoJson(context))
    }

    /**
     * 获取当前语言设置 (同步方法)
     * H5调用: var result = dsBridge.call("getLanguageSet")
     * 返回格式: { "language": "en" }
     */
    @JavascriptInterface
    fun getLanguageSet(args: Any?): String {
        val language = AppDeviceInfo.getLanguageSet(context)
        return """{"language": "$language"}"""
    }
    /**
     * 上传设备数据 (异步方法)
     * H5调用: dsBridge.call("uploadDeviceData", {"sceneType": "xxx"}, function(result) {})
     * 返回格式: { "success": true/false, "message": "..." }
     *
     * 此方法直接上传设备数据，不显示权限弹窗
     */

    @JavascriptInterface
    fun checkAllPermissions(args: Any?): String {
        val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val smsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

        return """{"camera": $cameraGranted, "location": $locationGranted, "sms": $smsGranted}"""
    }

//===============================================

    /**
     * H5推送语言设置给原生 (同步方法)
     * H5调用: dsBridge.call("pushLanguageSet", {"language": "en"}) 或 dsBridge.call("pushLanguageSet", "en")
     * 返回格式: { "success": true/false }
     */
    @JavascriptInterface
    fun pushLanguageSet(args: Any?): String {
        return try {
            val language = when (args) {
                is String -> args
                is org.json.JSONObject -> args.optString("language", "")
                else -> args?.toString() ?: ""
            }

            if (language.isNotEmpty()) {
                AppDeviceInfo.setLanguageSet(context, language)
                """{"success": true, "language": "$language"}"""
            } else {
                """{"success": false, "error": "Language is empty"}"""
            }
        } catch (e: Exception) {
            """{"success": false, "error": "${e.message}"}"""
        }
    }

    /**
     * 获取应用设备信息 (异步方法)
     * H5调用: dsBridge.call("getAppDeviceInfo", null, function(result) {})
     * 返回格式: { "appTerminal": "ANDROID", "appVersion": "1.0.0", "uuid": "...", "gid": "...", "phoneTerminal": "ANDROID", "barHeight": 24, "channel": "default" }
     * 此信息用于H5和API通用的设备标识
     */
    @JavascriptInterface
    fun getAppDeviceInfo(args: Any?, handler: CompletionHandler<String>) {
        try {
            handler.complete(AppDeviceInfo.getDeviceInfoJson())
        } catch (e: Exception) {
            handler.complete("""{"error": "${e.message}"}""")
        }
    }

    /**
     * 登录成功,需要H5传递token及用户信息
     * H5调用: dsBridge.call("loginSuccess", {"token": "xxx", "refreshToken": "xxx"}, function(result) {})
     * 或: dsBridge.call("loginSuccess", {"token": "xxx"}, function(result) {})
     * 返回格式: { "success": true/false, "message": "..." }
     */
    @JavascriptInterface
    fun loginSuccess(args: Any?, handler: CompletionHandler<String>) {
        try {
            // 先检查是否为 JSONObject.NULL 或 null
            if (args == null || args == org.json.JSONObject.NULL) {
                handler.complete("""{"success": false, "message": "args is null"}""")
                return
            }
            // DSBridge可能传递String(JSON字符串)或JSONObject
            val jsonObj = when (args) {
                is org.json.JSONObject -> {
                    // 再次检查是否是 JSONObject.NULL
                    if (args == org.json.JSONObject.NULL) null else args
                }
                is String -> {
                    if (args.isNotEmpty()) {
                        org.json.JSONObject(args)
                    } else {
                        null
                    }
                }
                else -> null
            }

            val token = jsonObj?.optString("token", "") ?: ""
            val refreshToken = jsonObj?.optString("refreshToken", null)
            val expireTime = jsonObj?.optLong("expireTime", 0)?.takeIf { it > 0 }

            if (token.isEmpty()) {
                handler.complete("""{"success": false, "message": "Token is empty"}""")
                return
            }

            // 使用TokenManager保存token
            com.example.cashcredit.network.TokenManager.saveToken(token, refreshToken, expireTime)

            handler.complete("""{"success": true, "message": "Token saved successfully"}""")
        } catch (e: Exception) {
            handler.complete("""{"success": false, "message": "${e.message}"}""")
        }
    }

    /**
     * 退出登录，需要H5告知退出登录,原生清除用户信息
     * H5调用: dsBridge.call("logOut", null, function(result) {})
     * 返回格式: { "success": true }
     */
    @JavascriptInterface
    fun logOut(args: Any?, handler: CompletionHandler<String>) {
        // 清除Token
        com.example.cashcredit.network.TokenManager.clearToken()
        handler.complete("""{"success": true}""")
    }

    /**
     * 原生登录接口 (异步方法)
     * H5调用: dsBridge.call("nativeLogin", {"mobile": "手机号", "password": "密码"}, function(result) {})
     * 返回格式: { "success": true/false, "message": "..." }
     *
     * 登录成功后自动保存Token，供后续接口调用
     */
    @JavascriptInterface
    fun nativeLogin(args: Any?, handler: CompletionHandler<String>) {
        // 解析参数
        var mobile = ""
        var password = ""

        try {
            // 先检查是否为 JSONObject.NULL 或 null
            if (args == null || args == org.json.JSONObject.NULL) {
                handler.complete("""{"success": false, "message": "args is null"}""")
                return
            }
            val jsonObj = when (args) {
                is org.json.JSONObject -> {
                    // 再次检查是否是 JSONObject.NULL
                    if (args == org.json.JSONObject.NULL) null else args
                }
                is String -> if (args.isNotEmpty()) org.json.JSONObject(args) else null
                else -> null
            }
            mobile = jsonObj?.optString("mobile", "") ?: ""
            password = jsonObj?.optString("password", "") ?: ""
        } catch (e: Exception) {
            handler.complete("""{"success": false, "message": "Invalid parameters: ${e.message}"}""")
            return
        }

        if (mobile.isEmpty() || password.isEmpty()) {
            handler.complete("""{"success": false, "message": "mobile or password is empty"}""")
            return
        }

        // 使用 lifecycleScope 启动协程
        val lifecycleOwner = context as? LifecycleOwner
        if (lifecycleOwner == null) {
            handler.complete("""{"success": false, "message": "Context is not LifecycleOwner"}""")
            return
        }

        lifecycleOwner.lifecycleScope.launch {
            try {
                val request = com.example.cashcredit.network.api.ApiInterface.WebLoginRequest(
                    mobile = mobile,
                    password = password
                )
                val response = com.example.cashcredit.network.RetrofitClient.api.webLogin(request)

                if (response.isSuccess() && response.data != null) {
                    // 保存Token
                    com.example.cashcredit.network.TokenManager.saveToken(response.data.token)
                    handler.complete("""{"success": true, "message": "Login successful"}""")
                } else {
                    handler.complete("""{"success": false, "message": "${response.message}"}""")
                }
            } catch (e: Exception) {
                handler.complete("""{"success": false, "message": "${e.message}"}""")
            }
        }
    }

    /**
     * 根据权限全称数组请求权限 (异步方法)
     * H5调用: dsBridge.call("requestPermissions", ["android.permission.CAMERA", "android.permission.READ_SMS"], function(result) {})
     * 或: dsBridge.call("requestPermissions", {"permissions": ["android.permission.CAMERA"]}, function(result) {})
     *
     * 权限全称示例:
     * - android.permission.CAMERA: 相机权限
     * - android.permission.ACCESS_FINE_LOCATION: 精确位置权限
     * - android.permission.ACCESS_COARSE_LOCATION: 粗略位置权限
     * - android.permission.READ_SMS: 短信读取权限
     * - android.permission.READ_CONTACTS: 联系人读取权限
     * - android.permission.READ_PHONE_STATE: 电话状态权限
     *
     * 返回格式: {"success": true, "results": {"android.permission.CAMERA": true, "android.permission.READ_SMS": false}}
     */
    @JavascriptInterface
    fun requestPermissions(args: Any?, handler: CompletionHandler<String>) {
        if (context !is android.app.Activity) {
            handler.complete("""{"success": false, "error": "Context is not Activity"}""")
            return
        }

        val activity = context as android.app.Activity

        // 解析权限数组
        val permissions = mutableListOf<String>()

        try {
            when (args) {
                is org.json.JSONArray -> {
                    for (i in 0 until args.length()) {
                        args.optString(i)?.takeIf { it.isNotEmpty() }?.let { permissions.add(it) }
                    }
                }
                is org.json.JSONObject -> {
                    val permissionsArray = args.optJSONArray("permissions")
                    if (permissionsArray != null) {
                        for (i in 0 until permissionsArray.length()) {
                            permissionsArray.optString(i)?.takeIf { it.isNotEmpty() }?.let { permissions.add(it) }
                        }
                    }
                }
                is String -> {
                    // 尝试解析为JSON数组
                    if (args.startsWith("[")) {
                        val jsonArray = org.json.JSONArray(args)
                        for (i in 0 until jsonArray.length()) {
                            jsonArray.optString(i)?.takeIf { it.isNotEmpty() }?.let { permissions.add(it) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            handler.complete("""{"success": false, "error": "Invalid permissions array: ${e.message}"}""")
            return
        }

        if (permissions.isEmpty()) {
            handler.complete("""{"success": false, "error": "No permissions specified"}""")
            return
        }

        // 请求权限
        PermissionHelper.requestPermissionsByNames(activity, permissions, handler)
    }

    /**
     * 检查指定权限状态 (同步方法)
     * H5调用: var result = dsBridge.call("checkPermissions", ["android.permission.CAMERA", "android.permission.READ_SMS"])
     * 返回格式: {"success": true, "results": {"android.permission.CAMERA": true, "android.permission.READ_SMS": false}}
     */
    @JavascriptInterface
    fun checkPermissions(args: Any?): String {
        val permissions = mutableListOf<String>()

        try {
            when (args) {
                is org.json.JSONArray -> {
                    for (i in 0 until args.length()) {
                        args.optString(i)?.takeIf { it.isNotEmpty() }?.let { permissions.add(it) }
                    }
                }
                is String -> {
                    if (args.startsWith("[")) {
                        val jsonArray = org.json.JSONArray(args)
                        for (i in 0 until jsonArray.length()) {
                            jsonArray.optString(i)?.takeIf { it.isNotEmpty() }?.let { permissions.add(it) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return """{"success": false, "error": "${e.message}"}"""
        }

        if (permissions.isEmpty()) {
            return """{"success": false, "error": "No permissions specified"}"""
        }

        // 直接检查权限状态
        val results = mutableMapOf<String, Boolean>()
        for (permission in permissions) {
            results[permission] = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        val json = org.json.JSONObject()
        json.put("success", true)
        val resultsJson = org.json.JSONObject()
        results.forEach { (permission, granted) -> resultsJson.put(permission, granted) }
        json.put("results", resultsJson)
        return json.toString()
    }

    /**
     * 设备信息上传
     */
    @JavascriptInterface
    fun uploadDeviceData(args: Any?, handler: CompletionHandler<String>) {
        // 解析参数 - DSBridge可能传递String(JSON字符串)、JSONObject或JSONObject.NULL
        var sceneType = ""

        try {
            // 先检查是否为 JSONObject.NULL 或 null
            if (args == null || args == org.json.JSONObject.NULL) {
                sceneType = ""
            } else {
                val jsonObj = when (args) {
                    is org.json.JSONObject -> {
                        // 再次检查是否是 JSONObject.NULL（它可能匹配 is JSONObject）
                        if (args == org.json.JSONObject.NULL) null else args
                    }
                    is String -> {
                        if (args.isNotEmpty()) {
                            org.json.JSONObject(args)
                        } else {
                            null
                        }
                    }
                    else -> null
                }
                // 安全获取 sceneType，处理 JSONObject.NULL 的情况
                sceneType = if (jsonObj != null && !jsonObj.isNull("sceneType")) {
                    jsonObj.optString("sceneType", "")
                } else {
                    ""
                }
            }
        } catch (e: Exception) {
            handler.complete("""{"success": false, "error": "Invalid parameters: ${e.message}"}""")
            return
        }

        if (sceneType.isEmpty()) {
            handler.complete("""{"success": false, "error": "sceneType is empty"}""")
            return
        }
        localUploadDeviceInfo(sceneType,handler)
    }

    /**
     * 显示权限说明弹窗 (异步方法)
     * H5调用: dsBridge.call("showPermissionDescDialog", null, function(result) {})
     * 或带可选参数: dsBridge.call("showPermissionDescDialog", {"title": "自定义标题"}, function(result) {})
     * 返回格式: { "success": true/false, "allGranted": true/false }
     *
     * 弹窗会显示以下权限说明：
     * - 相机权限：用于拍摄照片完成身份验证
     * - 地理位置权限：用于风控和服务定位
     * - 短信权限：用于身份验证和风控
     * - 应用列表权限：用于风控评估
     *
     * 点击确认按钮后自动请求权限，权限结果通过回调返回给H5
     * 如果权限被拒绝会引导用户去设置界面开启权限
     *
     * 注意：此方法只获取权限结果，如需上传设备数据，请调用 uploadDeviceData 方法
     */
    @JavascriptInterface
    fun showPermissionDescDialog(args: Any?, handler: CompletionHandler<String>) {
        if (context !is android.app.Activity) {
            handler.complete("""{"success": false, "allGranted": false, "error": "Context is not Activity"}""")
            return
        }

        val activity = context as android.app.Activity

        // 解析参数 - DSBridge可能传递String(JSON字符串)、JSONObject或JSONObject.NULL
        var sceneType = ""
        try {
            // 先检查是否为 JSONObject.NULL 或 null
            if (args == null || args == org.json.JSONObject.NULL) {
                sceneType = ""
            } else {
                val jsonObj = when (args) {
                    is org.json.JSONObject -> {
                        // 再次检查是否是 JSONObject.NULL（它可能匹配 is JSONObject）
                        if (args == org.json.JSONObject.NULL) null else args
                    }
                    is String -> {
                        if (args.isNotEmpty()) {
                            org.json.JSONObject(args)
                        } else {
                            null
                        }
                    }
                    else -> null
                }
                // 安全获取 sceneType，处理 JSONObject.NULL 的情况
                sceneType = if (jsonObj != null && !jsonObj.isNull("sceneType")) {
                    jsonObj.optString("sceneType", "")
                } else {
                    ""
                }
            }
        } catch (e: Exception) {
            handler.complete("""{"success": false, "error": "Invalid parameters: ${e.message}"}""")
            return
        }

        if (sceneType.isEmpty()) {
            handler.complete("""{"success": false, "error": "sceneType is empty"}""")
            return
        }
        // 使用PermissionHelper中统一管理的权限列表
        val requiredPermissions = PermissionHelper.REQUIRED_PERMISSIONS

        // 检查是否所有权限都已授予
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            localUploadDeviceInfo(sceneType,handler)
        } else {
            // 有未授予的权限，显示权限弹窗
            PermissionDialogManager.showDialog(
                activity,
                onPermissionsGranted = {
                    localUploadDeviceInfo(sceneType,handler)
                },
                onPermissionsDenied = {
                    val resultJson = org.json.JSONObject().apply {
                        put("success", true)
                        put("recordNo", "")
                        put("customerNo", "")
                    }.toString()
                    handler.complete(resultJson)
                }
            )
        }
    }

    /**
     * 发送设备信息的方法
     */
    fun localUploadDeviceInfo(sceneType: String, handler: CompletionHandler<String>){
        // 使用 lifecycleScope 启动协程
        val lifecycleOwner = context as? LifecycleOwner
        if (lifecycleOwner == null) {
            handler.complete("""{"success": false, "error": "Context is not LifecycleOwner"}""")
            return
        }

        lifecycleOwner.lifecycleScope.launch {
            try {
                DeviceUploadManager.uploadDeviceDataWithCallback(
                    context = context,
                    sceneType = sceneType,
                    onSuccess = { response ->
                        val resultJson = org.json.JSONObject().apply {
                            put("success", true)
                            put("recordNo", response.recordNo ?: "")
                            put("customerNo", response.customerNo ?: "")
                        }.toString()
                        handler.complete(resultJson)
                    },
                    onFailure = { error ->
                        handler.complete("""{"success": false, "error": "${error.message}"}""")
                    }
                )
            } catch (e: Exception) {
                handler.complete("""{"success": false, "error": "${e.message}"}""")
            }
        }
    }
    /**
     * 检查所有权限状态 (同步方法)
     * H5调用: var result = dsBridge.call("checkAllPermissions")
     * 返回格式: { "camera": true/false, "location": true/false, "sms": true/false }
     */




    /**
     * 打开证件拍照 (异步方法)
     * H5调用: dsBridge.call("openIdCardCamera", null, function(result) {})
     * 或指定证件类型: dsBridge.call("openIdCardCamera", {"imageType": "ID_CARD_FRONT"}, function(result) {})
     * imageType可选值: ID_CARD_FRONT(身份证正面), ID_CARD_BACK(身份证背面), FACE(人脸)
     */
    @JavascriptInterface
    fun openIdCardCamera(args: Any?, handler: CompletionHandler<String>) {
        if (context !is android.app.Activity) {
            handler.complete("""{"success": false, "error": "Context is not Activity"}""")
            return
        }

        val activity = context as android.app.Activity

        // 获取证件类型 (兼容String和JSONObject两种参数类型)
        val imageType = try {
            // 先检查是否为 JSONObject.NULL 或 null
            if (args == null || args == org.json.JSONObject.NULL) {
                TakePhotoActivity.ID_FRONT
            } else {
                val jsonObj = when (args) {
                    is org.json.JSONObject -> {
                        // 再次检查是否是 JSONObject.NULL
                        if (args == org.json.JSONObject.NULL) null else args
                    }
                    is String -> if (args.isNotEmpty()) org.json.JSONObject(args) else null
                    else -> null
                }
                jsonObj?.optString(ImageRepository.ImageType.imageType, TakePhotoActivity.ID_FRONT) ?: TakePhotoActivity.ID_FRONT
            }
        } catch (e: Exception) {
            TakePhotoActivity.ID_FRONT
        }

        // 检查相机权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // 保存回调，请求权限后再打开相机
            val pendingHandler = handler
            PermissionHelper.requestCameraPermission(activity, object : CompletionHandler<String> {
                override fun complete(result: String?) {
                    try {
                        val json = org.json.JSONObject(result ?: "{}")
                        if (json.optBoolean("granted", false)) {
                            // 权限已授予，打开相机
                            startCameraActivity(activity, imageType, pendingHandler)
                        } else {
                            // 权限被拒绝
                            pendingHandler.complete("""{"success": false, "error": "Camera permission denied"}""")
                        }
                    } catch (e: Exception) {
                        pendingHandler.complete("""{"success": false, "error": "Permission result parse error"}""")
                    }
                }

                override fun complete() {

                }

                override fun setProgressData(value: String?) {

                }
            })
        } else {
            // 已有权限，直接打开相机
            startCameraActivity(activity, imageType, handler)
        }
    }

    /**
     * 启动拍照Activity
     */
    private fun startCameraActivity(activity: android.app.Activity, imageType: String, handler: CompletionHandler<String>) {
        // 保存回调
        CameraCallbackManager.setCallback(handler)

        // 启动拍照Activity
        val intent = Intent(activity, TakePhotoActivity::class.java).apply {
            putExtra(ImageRepository.ImageType.imageType, imageType)
        }
        activity.startActivity(intent)
    }

    /**
     * 选择联系人 (异步方法)
     * H5调用: dsBridge.call("pickContact", null, function(result) {})
     * 返回格式: { "success": true/false, "id": 123, "name": "联系人名称", "phoneNumbers": [...], "emails": [...] }
     *
     * 使用系统联系人选择器选择单个联系人
     * 需要READ_CONTACTS权限才能读取联系人详情
     */
    @JavascriptInterface
    fun pickContact(args: Any?, handler: CompletionHandler<String>) {
        if (context !is android.app.Activity) {
            handler.complete("""{"success": false, "error": "Context is not Activity"}""")
            return
        }

        val activity = context as android.app.Activity

        // 检查联系人权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            // 保存回调，请求权限后再打开选择器
            val pendingHandler = handler
            PermissionHelper.requestContactsPermission(activity, object : CompletionHandler<String> {
                override fun complete(result: String?) {
                    try {
                        val json = org.json.JSONObject(result ?: "{}")
                        if (json.optBoolean("granted", false)) {
                            // 权限已授予，打开选择器
                            startContactPicker(activity, pendingHandler)
                        } else {
                            // 权限被拒绝
                            pendingHandler.complete("""{"success": false, "error": "Contacts permission denied"}""")
                        }
                    } catch (e: Exception) {
                        pendingHandler.complete("""{"success": false, "error": "Permission result parse error"}""")
                    }
                }

                override fun complete() {}

                override fun setProgressData(value: String?) {}
            })
        } else {
            // 已有权限，直接打开选择器
            startContactPicker(activity, handler)
        }
    }

    /**
     * 启动联系人选择器
     */
    private fun startContactPicker(activity: android.app.Activity, handler: CompletionHandler<String>) {
        // 保存回调
        ContactPickerManager.setCallback(handler)

        // 启动系统联系人选择器
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
        }

        try {
            activity.startActivityForResult(intent, ContactPickerManager.getRequestCode())
        } catch (e: Exception) {
            handler.complete("""{"success": false, "error": "No contacts app available"}""")
            ContactPickerManager.clearCallback()
        }
    }

    companion object {
        /**
         * 注册DSBridge接口到DWebView
         */
        fun register(dWebView: DWebView, context: Context) {
            dWebView.addJavascriptObject(DSBridgeInterface(context), null)
        }
    }
}