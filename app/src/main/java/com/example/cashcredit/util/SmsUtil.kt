package com.example.cashcredit.util

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import androidx.core.content.ContextCompat

/**
 * 短信工具类
 * 用于读取手机短信信息
 */
object SmsUtil {

    /**
     * 检查是否有短信读取权限
     */
    fun hasSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 获取短信列表
     * @param context 上下文
     * @param maxCount 最大获取数量，默认20条
     * @return JSON格式字符串
     */
    fun getSmsListJson(context: Context, maxCount: Int = 20): String {
        return try {
            if (!hasSmsPermission(context)) {
                return """{"success": false, "error": "Permission denied", "code": "PERMISSION_DENIED", "count": 0, "smsList": []}"""
            }

            val smsList = getSmsList(context, maxCount)
            buildSmsJson(smsList)
        } catch (e: Exception) {
            """{"success": false, "error": "${e.message}", "count": 0, "smsList": []}"""
        }
    }

    /**
     * 读取短信列表
     */
    private fun getSmsList(context: Context, maxCount: Int): List<SmsInfo> {
        val smsList = mutableListOf<SmsInfo>()
        val uri: Uri = Telephony.Sms.CONTENT_URI
        val resolver: ContentResolver = context.contentResolver

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ
        )

        val cursor: Cursor? = resolver.query(
            uri,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )

        cursor?.use {
            var count = 0
            while (it.moveToNext() && count < maxCount) {
                val id = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: ""
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))
                val read = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.READ))

                smsList.add(
                    SmsInfo(
                        id = id,
                        address = address,
                        body = body,
                        date = date,
                        type = getSmsTypeName(type),
                        read = read == 1
                    )
                )
                count++
            }
        }

        return smsList
    }

    /**
     * 获取短信类型名称
     */
    private fun getSmsTypeName(type: Int): String {
        return when (type) {
            Telephony.Sms.MESSAGE_TYPE_INBOX -> "received"
            Telephony.Sms.MESSAGE_TYPE_SENT -> "sent"
            Telephony.Sms.MESSAGE_TYPE_DRAFT -> "draft"
            Telephony.Sms.MESSAGE_TYPE_OUTBOX -> "outbox"
            Telephony.Sms.MESSAGE_TYPE_FAILED -> "failed"
            Telephony.Sms.MESSAGE_TYPE_QUEUED -> "queued"
            else -> "unknown"
        }
    }

    /**
     * 构建JSON字符串
     */
    private fun buildSmsJson(smsList: List<SmsInfo>): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("{\n")
        stringBuilder.append("    \"success\": true,\n")
        stringBuilder.append("    \"count\": ${smsList.size},\n")
        stringBuilder.append("    \"smsList\": [\n")

        smsList.forEachIndexed { index, sms ->
            stringBuilder.append("        {\n")
            stringBuilder.append("            \"id\": ${sms.id},\n")
            stringBuilder.append("            \"address\": \"${escapeJson(sms.address)}\",\n")
            stringBuilder.append("            \"body\": \"${escapeJson(sms.body)}\",\n")
            stringBuilder.append("            \"date\": ${sms.date},\n")
            stringBuilder.append("            \"type\": \"${sms.type}\",\n")
            stringBuilder.append("            \"read\": ${sms.read}\n")
            stringBuilder.append("        }")
            if (index < smsList.size - 1) {
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
     * 短信信息数据类
     */
    private data class SmsInfo(
        val id: Long,
        val address: String,
        val body: String,
        val date: Long,
        val type: String,
        val read: Boolean
    )
}