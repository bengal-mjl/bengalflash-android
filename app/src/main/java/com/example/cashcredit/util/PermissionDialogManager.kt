package com.example.cashcredit.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import com.example.cashcredit.ui.dialog.PermissionDescDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import wendu.dsbridge.CompletionHandler

/**
 * 权限弹窗管理类
 * 用于在多个Activity中管理权限弹窗
 */
@SuppressLint("StaticFieldLeak")
object PermissionDialogManager {

    private var currentDialog: PermissionDescDialog? = null
    private var currentRecordNo: String? = null
    private var currentSceneType: String? = null
    private var currentContext: android.content.Context? = null

    /**
     * 显示权限说明弹窗
     * @param activity 当前Activity
     * @param handler DSBridge回调
     * @param title 弹窗标题（可选）
     */
    fun showDialog(
        activity: Activity,
        onPermissionsGranted:()-> Unit,
        onPermissionsDenied:()-> Unit
    ) {
        // 先关闭之前的弹窗
        dismissDialog()
        currentRecordNo = null
        currentSceneType = null
        currentContext = null

        currentDialog = PermissionDescDialog(
            activity = activity,
            onPermissionsGranted = {
                currentDialog = null
                onPermissionsGranted.invoke()
            },
            onPermissionsDenied = {
                currentDialog = null
                onPermissionsDenied.invoke()
            }
        ).apply {
            show()
        }
    }

    /**
     * 清除状态
     */
    private fun clearState() {
        currentDialog = null
        currentRecordNo = null
        currentSceneType = null
        currentContext = null
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
        return currentDialog?.onRequestPermissionsResult(requestCode, permissions, grantResults) ?: false
    }

    /**
     * 处理Activity结果
     * 在Activity的onActivityResult中调用
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        return currentDialog?.onActivityResult(requestCode, resultCode, data) ?: false
    }

    /**
     * 关闭当前弹窗
     */
    fun dismissDialog() {
        currentDialog?.dismiss()
        currentDialog = null
    }

    /**
     * 检查是否有正在显示的弹窗
     */
    fun isShowing(): Boolean {
        return currentDialog?.isShowing == true
    }

}