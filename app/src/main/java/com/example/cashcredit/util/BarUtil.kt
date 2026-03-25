package com.example.cashcredit.util

import android.annotation.SuppressLint
import android.content.res.Resources



object BarUtil {
    @SuppressLint("DiscouragedApi")
    fun getStatusBarHeight(): Int {
        val resources = Resources.getSystem()
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

}