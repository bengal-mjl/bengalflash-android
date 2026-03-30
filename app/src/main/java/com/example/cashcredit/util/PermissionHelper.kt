package com.example.cashcredit.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import wendu.dsbridge.CompletionHandler

/**
 * 权限请求帮助类
 * 用于处理运行时权限请求
 */
@SuppressLint("StaticFieldLeak")
object PermissionHelper {

    /**
     * 应用核心权限列表
     * 包含：相机、精确位置、粗略位置、短信、联系人
     * 用于权限弹窗和权限检查
     */
    val REQUIRED_PERMISSIONS: Array<String> = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CONTACTS
    )

    private const val REQUEST_CODE_SMS = 1001
    private const val REQUEST_CODE_DEVICE = 1002
    private const val REQUEST_CODE_CONTACTS = 1003
    private const val REQUEST_CODE_CAMERA = 1004
    private const val REQUEST_CODE_DYNAMIC = 1005
    private const val REQUEST_CODE_SETTINGS = 1006

    // 存储权限请求回调
    private var smsPermissionCallback: CompletionHandler<String>? = null
    private var devicePermissionCallback: CompletionHandler<String>? = null
    private var contactsPermissionCallback: CompletionHandler<String>? = null
    private var cameraPermissionCallback: CompletionHandler<String>? = null
    private var dynamicPermissionCallback: CompletionHandler<String>? = null

    // 存储动态权限请求的权限列表
    private var dynamicPermissions: List<String> = emptyList()

    // 存储当前Activity，用于权限拒绝后引导去设置
    private var currentActivity: Activity? = null

    /**
     * 请求短信权限
     */
    fun requestSmsPermission(activity: Activity, handler: CompletionHandler<String>) {
        smsPermissionCallback = handler
        currentActivity = activity

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.READ_SMS),
                REQUEST_CODE_SMS
            )
        } else {
            // Android 6.0 以下自动授权
            handler.complete("""{"granted": true}""")
        }
    }

    /**
     * 请求设备信息权限（电话状态权限和位置权限）
     */
    fun requestDevicePermission(activity: Activity, handler: CompletionHandler<String>) {
        devicePermissionCallback = handler
        currentActivity = activity

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = mutableListOf<String>()

            // 检查电话权限
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_PHONE_STATE)
            }

            // 检查位置权限（用于获取WiFi信息）
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            if (permissions.isEmpty()) {
                // 已有所有权限
                handler.complete("""{"phoneGranted": true, "locationGranted": true}""")
            } else {
                ActivityCompat.requestPermissions(
                    activity,
                    permissions.toTypedArray(),
                    REQUEST_CODE_DEVICE
                )
            }
        } else {
            // Android 6.0 以下自动授权
            handler.complete("""{"phoneGranted": true, "locationGranted": true}""")
        }
    }

    /**
     * 请求联系人权限
     */
    fun requestContactsPermission(activity: Activity, handler: CompletionHandler<String>) {
        contactsPermissionCallback = handler
        currentActivity = activity

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
                // 已有权限
                handler.complete("""{"granted": true}""")
            } else {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    REQUEST_CODE_CONTACTS
                )
            }
        } else {
            // Android 6.0 以下自动授权
            handler.complete("""{"granted": true}""")
        }
    }

    /**
     * 请求相机权限
     */
    fun requestCameraPermission(activity: Activity, handler: CompletionHandler<String>) {
        cameraPermissionCallback = handler
        currentActivity = activity

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                // 已有权限
                handler.complete("""{"granted": true}""")
            } else {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CODE_CAMERA
                )
            }
        } else {
            // Android 6.0 以下自动授权
            handler.complete("""{"granted": true}""")
        }
    }

    /**
     * 根据权限全称数组请求权限 (动态权限请求)
     * @param activity Activity
     * @param permissions 权限全称数组，如 ["android.permission.CAMERA", "android.permission.ACCESS_FINE_LOCATION"]
     * @param handler 回调
     *
     * 返回格式: {"success": true, "results": {"android.permission.CAMERA": true, "android.permission.ACCESS_FINE_LOCATION": false}}
     */
    fun requestPermissionsByNames(
        activity: Activity,
        permissions: List<String>,
        handler: CompletionHandler<String>
    ) {
        dynamicPermissionCallback = handler
        dynamicPermissions = permissions
        currentActivity = activity

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Android 6.0 以下自动授权
            val results = permissions.associateWith { true }
            handler.complete(buildPermissionsResultJson(true, results))
            dynamicPermissionCallback = null
            dynamicPermissions = emptyList()
            return
        }

        // 筛选出需要请求的权限（未授权的）
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        // 如果所有权限都已授权
        if (permissionsToRequest.isEmpty()) {
            val results = permissions.associateWith { true }
            handler.complete(buildPermissionsResultJson(true, results))
            dynamicPermissionCallback = null
            dynamicPermissions = emptyList()
            return
        }

        // 请求权限
        ActivityCompat.requestPermissions(
            activity,
            permissionsToRequest.toTypedArray(),
            REQUEST_CODE_DYNAMIC
        )
    }

    /**
     * 构建权限结果JSON
     */
    private fun buildPermissionsResultJson(success: Boolean, results: Map<String, Boolean>): String {
        val json = JSONObject()
        json.put("success", success)
        val resultsJson = JSONObject()
        results.forEach { (permission, granted) ->
            resultsJson.put(permission, granted)
        }
        json.put("results", resultsJson)
        return json.toString()
    }

    /**
     * 处理权限请求结果
     * 在 Activity 的 onRequestPermissionsResult 中调用
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_SMS -> {
                val granted = grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (!granted && shouldShowPermissionRationale(permissions)) {
                    // 用户拒绝了权限但没有选择"不再询问"，可以再次请求
                    smsPermissionCallback?.complete("""{"granted": false}""")
                } else if (!granted) {
                    // 用户拒绝了权限并选择了"不再询问"，引导去设置
                    showPermissionGuideDialog(
                        activity = currentActivity,
                        permissionName = "短信",
                        handler = smsPermissionCallback
                    )
                } else {
                    smsPermissionCallback?.complete("""{"granted": true}""")
                }
                smsPermissionCallback = null
            }
            REQUEST_CODE_DEVICE -> {
                var phoneGranted = false
                var locationGranted = false

                permissions.forEachIndexed { index, permission ->
                    val granted = grantResults.getOrNull(index) == PackageManager.PERMISSION_GRANTED
                    when (permission) {
                        Manifest.permission.READ_PHONE_STATE -> phoneGranted = granted
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION -> locationGranted = granted
                    }
                }

                devicePermissionCallback?.complete(
                    """{"phoneGranted": $phoneGranted, "locationGranted": $locationGranted}"""
                )
                devicePermissionCallback = null
            }
            REQUEST_CODE_CONTACTS -> {
                val granted = grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    contactsPermissionCallback?.complete("""{"granted": true}""")
                } else if (shouldShowPermissionRationale(permissions)) {
                    // 用户拒绝了权限但没有选择"不再询问"，返回结果让调用方处理
                    contactsPermissionCallback?.complete("""{"granted": false, "shouldAskAgain": true}""")
                } else {
                    // 用户拒绝了权限并选择了"不再询问"，返回结果让调用方弹出引导对话框
                    contactsPermissionCallback?.complete("""{"granted": false, "shouldAskAgain": false}""")
                }
                contactsPermissionCallback = null
            }
            REQUEST_CODE_CAMERA -> {
                val granted = grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    cameraPermissionCallback?.complete("""{"granted": true}""")
                } else if (shouldShowPermissionRationale(permissions)) {
                    // 用户拒绝了权限但没有选择"不再询问"，返回结果让调用方处理
                    cameraPermissionCallback?.complete("""{"granted": false, "shouldAskAgain": true}""")
                } else {
                    // 用户拒绝了权限并选择了"不再询问"，返回结果让调用方弹出引导对话框
                    cameraPermissionCallback?.complete("""{"granted": false, "shouldAskAgain": false}""")
                }
                cameraPermissionCallback = null
            }
            REQUEST_CODE_DYNAMIC -> {
                // 处理动态权限请求结果
                val results = mutableMapOf<String, Boolean>()

                // 构建权限授权状态映射（请求的权限及其结果）
                val permissionResults = mutableMapOf<String, Boolean>()
                permissions.forEachIndexed { index, permission ->
                    permissionResults[permission] = grantResults.getOrNull(index) == PackageManager.PERMISSION_GRANTED
                }

                // 检查每个请求的权限
                for (permission in dynamicPermissions) {
                    // 如果权限在请求结果中，使用结果；否则说明之前已授权
                    results[permission] = permissionResults[permission] ?: true
                }

                dynamicPermissionCallback?.complete(buildPermissionsResultJson(true, results))
                dynamicPermissionCallback = null
                dynamicPermissions = emptyList()
            }
        }
    }

    /**
     * 检查是否应该显示权限请求说明
     * 如果返回false且权限未授予，说明用户选择了"不再询问"
     */
    private fun shouldShowPermissionRationale(permissions: Array<out String>): Boolean {
        val activity = currentActivity ?: return false
        return permissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }

    /**
     * 显示权限引导对话框，引导用户去设置界面开启权限
     */
    fun showPermissionGuideDialog(
        activity: Activity?,
        permissionName: String,
        handler: CompletionHandler<String>?
    ) {
        if (activity == null) {
            handler?.complete("""{"granted": false, "error": "Activity is null"}""")
            return
        }

        AlertDialog.Builder(activity)
            .setTitle("权限申请")
            .setMessage("${permissionName}权限已被拒绝，请在设置中手动开启权限")
            .setPositiveButton("去设置") { dialog, _ ->
                dialog.dismiss()
                openPermissionSettings(activity)
                handler?.complete("""{"granted": false, "needManualGrant": true}""")
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
                handler?.complete("""{"granted": false}""")
            }
            .setCancelable(false)
            .show()
    }

    /**
     * 打开应用权限设置界面
     */
    fun openPermissionSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        activity.startActivityForResult(intent, REQUEST_CODE_SETTINGS)
    }

    /**
     * 处理从设置界面返回的结果
     * 在Activity的onActivityResult中调用
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == REQUEST_CODE_SETTINGS) {
            // 用户从设置界面返回
            return true
        }
        return false
    }
}