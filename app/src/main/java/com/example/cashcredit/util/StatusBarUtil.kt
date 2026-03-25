package com.example.cashcredit.util

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowManager

/**
 * 状态栏工具类
 * 实现沉浸式状态栏
 */
object StatusBarUtil {

    /**
     * 设置沉浸式状态栏
     * @param activity Activity
     * @param isDark true: 深色文字(适合浅色背景), false: 浅色文字(适合深色背景)
     */
    fun setImmersiveStatusBar(activity: Activity, isDark: Boolean = false) {
        val window = activity.window
        val decorView = window.decorView

        // Android 5.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 清除FLAG_TRANSLUCENT_STATUS标志
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

            // 添加FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS标志
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

            // 设置状态栏颜色为透明
            window.statusBarColor = Color.TRANSPARENT

            // 设置系统UI可见性
            var visibility = decorView.systemUiVisibility
            visibility = visibility or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            visibility = visibility or View.SYSTEM_UI_FLAG_LAYOUT_STABLE

            // Android 6.0+ 设置状态栏文字颜色
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                visibility = if (isDark) {
                    visibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    visibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
            }

            decorView.systemUiVisibility = visibility
        }

        // Android 4.4 - 5.0 使用FLAG_TRANSLUCENT_STATUS
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }

    /**
     * 设置状态栏颜色
     * @param activity Activity
     * @param color 状态栏颜色
     * @param isDark true: 深色文字, false: 浅色文字
     */
    fun setStatusBarColor(activity: Activity, color: Int, isDark: Boolean = false) {
        val window = activity.window
        val decorView = window.decorView

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = color

            // Android 6.0+ 设置状态栏文字颜色
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                var visibility = decorView.systemUiVisibility
                visibility = if (isDark) {
                    visibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    visibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
                decorView.systemUiVisibility = visibility
            }
        }
    }

    /**
     * 获取状态栏高度
     * @param activity Activity
     * @return 状态栏高度(px)
     */
    fun getStatusBarHeight(activity: Activity): Int {
        var result = 0
        val resourceId = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = activity.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
}