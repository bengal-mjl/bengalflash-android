package com.example.cashcredit.ui.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.cashcredit.R
import com.example.cashcredit.util.PermissionHelper

/**
 * 权限说明弹窗
 * 显示需要申请的权限说明，点击确认后请求权限
 */
class PermissionDescDialog(
    private val activity: Activity,
    private val onPermissionsGranted: (() -> Unit)? = null,
    private val onPermissionsDenied: (() -> Unit)? = null
) : Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar) {

    companion object {
        private const val REQUEST_CODE_ALL_PERMISSIONS = 2001
        private const val REQUEST_CODE_APP_SETTINGS = 2002
    }

    private var btnConfirm: Button? = null

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.twairt_ortho_dialog_agreement)

        // 设置弹窗属性
        window?.apply {
            // 设置宽度为屏幕宽度的85%
            val displayMetrics = context.resources.displayMetrics
            val dialogWidth = (displayMetrics.widthPixels * 0.85).toInt()
            setLayout(
                dialogWidth,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setGravity(android.view.Gravity.CENTER)
            // 设置背景透明度为更暗
            setDimAmount(0.7f)
            // 添加窗口背景变暗效果
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        // 初始化视图
        initViews()

        // 设置不可取消
        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }

    private fun initViews() {
        btnConfirm = findViewById(R.id.btn_confirm)

        btnConfirm?.setOnClickListener {
            requestAllPermissions()
        }
    }

    /**
     * 请求所有权限
     */
    private fun requestAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notGrantedPermissions = PermissionHelper.REQUIRED_PERMISSIONS.filter {
                ActivityCompat.checkSelfPermission(activity, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
            }.toTypedArray()

            if (notGrantedPermissions.isEmpty()) {
                // 所有权限已授予
                dismiss()
                onPermissionsGranted?.invoke()
            } else {
                // 请求未授予的权限
                ActivityCompat.requestPermissions(
                    activity,
                    notGrantedPermissions,
                    REQUEST_CODE_ALL_PERMISSIONS
                )
            }
        } else {
            // Android 6.0 以下自动授权
            dismiss()
            onPermissionsGranted?.invoke()
        }
    }

    /**
     * 处理权限请求结果
     * 在Activity的onRequestPermissionsResult中调用
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode == REQUEST_CODE_ALL_PERMISSIONS) {
            val allGranted = grantResults.isNotEmpty() &&
                    grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }

            if (allGranted) {
                // 所有权限已授予
                dismiss()
                onPermissionsGranted?.invoke()
            } else {
                // 有权限被拒绝
                val shouldShowRationale = permissions.any {
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
                }

                if (!shouldShowRationale) {
                    // 用户选择了"不再询问"，引导去设置界面
                    showGoToSettingsDialog()
                } else {
                    // 用户拒绝了权限，但可以再次请求
                    dismiss()
                    onPermissionsDenied?.invoke()
                    Toast.makeText(
                        activity,
                        R.string.permission_denied_guide,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            return true
        }
        return false
    }

    /**
     * 显示引导去设置的弹窗
     */
    private fun showGoToSettingsDialog() {
        android.app.AlertDialog.Builder(activity)
            .setMessage(R.string.permission_denied_guide)
            .setPositiveButton(R.string.go_to_settings) { dialog, _ ->
                dialog.dismiss()
                openAppSettings()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                this@PermissionDescDialog.dismiss()
                onPermissionsDenied?.invoke()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * 打开应用设置界面
     */
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivityForResult(intent, REQUEST_CODE_APP_SETTINGS)
    }

    /**
     * 处理从设置界面返回的结果
     * 在Activity的onActivityResult中调用
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == REQUEST_CODE_APP_SETTINGS) {
            // 从设置界面返回，重新检查权限
            val allGranted = PermissionHelper.REQUIRED_PERMISSIONS.all {
                ActivityCompat.checkSelfPermission(
                    activity,
                    it
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }

            if (allGranted) {
                dismiss()
                // 添加延迟，确保权限完全生效后再执行回调
                Handler(Looper.getMainLooper()).postDelayed({
                    onPermissionsGranted?.invoke()
                }, 300)
            } else {
                // 用户没有授予权限
                dismiss()
                Toast.makeText(
                    activity,
                    R.string.permission_denied_guide,
                    Toast.LENGTH_LONG
                ).show()
                onPermissionsDenied?.invoke()
            }
            return true
        }
        return false
    }

    /**
     * 设置确认按钮文字
     */
    fun setConfirmText(text: String) {
        btnConfirm?.text = text
    }
}