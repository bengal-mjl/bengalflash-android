package com.example.cashcredit.ui.dialog

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cashcredit.R

/**
 * JS权限说明弹窗
 * 用于requestPermissions方法，根据请求的权限动态显示对应的描述
 * 支持的权限：READ_SMS、READ_PHONE_STATE、ACCESS_FINE_LOCATION/ACCESS_COARSE_LOCATION
 */
class JsPermissionDescDialog(
    private val activity: Activity,
    private val permissions: List<String>,
    private val onAccessClick: () -> Unit
) : Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar) {

    companion object {
        // 支持的权限映射
        val PERMISSION_TYPE_LOCATION = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        const val PERMISSION_TYPE_SMS = Manifest.permission.READ_SMS
        const val PERMISSION_TYPE_DEVICE = Manifest.permission.READ_PHONE_STATE
    }

    private var btnAccess: Button? = null
    private var titleLocation: TextView? = null
    private var descLocation: TextView? = null
    private var titleSms: TextView? = null
    private var descSms: TextView? = null
    private var titleDevice: TextView? = null
    private var descDevice: TextView? = null

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.twairt_ortho_js_permission_dialog)

        // 设置弹窗属性
        window?.apply {
            val displayMetrics = context.resources.displayMetrics
            val dialogWidth = (displayMetrics.widthPixels * 0.85).toInt()
            setLayout(
                dialogWidth,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setGravity(android.view.Gravity.CENTER)
            setDimAmount(0.7f)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        // 初始化视图
        initViews()

        // 根据权限设置显示对应的描述
        setupPermissionViews()

        // 设置不可取消
        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }

    private fun initViews() {
        btnAccess = findViewById(R.id.btn_access)
        titleLocation = findViewById(R.id.title_location)
        descLocation = findViewById(R.id.desc_location)
        titleSms = findViewById(R.id.title_sms)
        descSms = findViewById(R.id.desc_sms)
        titleDevice = findViewById(R.id.title_device)
        descDevice = findViewById(R.id.desc_device)

        btnAccess?.setOnClickListener {
            dismiss()
            onAccessClick.invoke()
        }
    }

    /**
     * 根据请求的权限设置显示对应的描述区域
     */
    private fun setupPermissionViews() {
        for (permission in permissions) {
            when (permission) {
                // 位置权限
                in PERMISSION_TYPE_LOCATION -> {
                    titleLocation?.visibility = View.VISIBLE
                    descLocation?.visibility = View.VISIBLE
                }
                // SMS权限
                PERMISSION_TYPE_SMS -> {
                    titleSms?.visibility = View.VISIBLE
                    descSms?.visibility = View.VISIBLE
                }
                // 设备权限 (READ_PHONE_STATE)
                PERMISSION_TYPE_DEVICE -> {
                    titleDevice?.visibility = View.VISIBLE
                    descDevice?.visibility = View.VISIBLE
                }
            }
        }
    }

    /**
     * 检查权限是否属于此弹窗支持的权限类型
     */
    fun isSupportedPermission(permission: String): Boolean {
        return permission in PERMISSION_TYPE_LOCATION ||
                permission == PERMISSION_TYPE_SMS ||
                permission == PERMISSION_TYPE_DEVICE
    }

    /**
     * 检查权限列表中是否有需要显示弹窗的权限
     */
    fun hasSupportedPermissions(): Boolean {
        return permissions.any { isSupportedPermission(it) }
    }
}