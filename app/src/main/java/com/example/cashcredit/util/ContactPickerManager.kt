package com.example.cashcredit.util

import android.content.ContentResolver
import android.database.Cursor
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import wendu.dsbridge.CompletionHandler

/**
 * 联系人选择器回调管理器
 * 用于管理联系人选择结果的回调
 */
object ContactPickerManager {

    private var pickerCallback: CompletionHandler<String>? = null
    private const val REQUEST_CODE_PICK_CONTACT = 10001

    /**
     * 获取请求码
     */
    fun getRequestCode(): Int = REQUEST_CODE_PICK_CONTACT

    /**
     * 保存回调
     */
    fun setCallback(handler: CompletionHandler<String>) {
        pickerCallback = handler
    }

    /**
     * 清除回调
     */
    fun clearCallback() {
        pickerCallback = null
    }

    /**
     * 处理联系人选择结果
     * @param resolver ContentResolver
     * @param data Intent data
     */
    fun handlePickResult(resolver: ContentResolver, data: android.content.Intent?) {
        val callback = pickerCallback ?: return

        if (data == null || data.data == null) {
            callback.complete("""{"success": false, "error": "No contact selected", "cancelled": true}""")
            clearCallback()
            return
        }

        try {
            val contactUri = data.data!!

            // 直接从返回的URI查询联系人信息
            // 当使用 Phone.CONTENT_TYPE 时，URI指向的是特定电话记录
            val contactInfo = getContactInfoFromPhoneUri(resolver, contactUri)

            if (contactInfo != null) {
                val result = buildContactJson(contactInfo)
                callback.complete(result)
            } else {
                callback.complete("""{"success": false, "error": "Failed to read contact info"}""")
            }
        } catch (e: Exception) {
            callback.complete("""{"success": false, "error": "${escapeJson(e.message ?: "Unknown error")}"}""")
        }

        clearCallback()
    }

    /**
     * 从Phone URI直接获取联系人信息
     * ACTION_PICK with Phone.CONTENT_TYPE 返回的URI包含电话号码信息
     */
    private fun getContactInfoFromPhoneUri(resolver: ContentResolver, phoneUri: android.net.Uri): ContactPickInfo? {
        var contactId: Long = -1
        var name = ""
        var selectedPhone = ""
        var phoneNumbers = mutableListOf<String>()
        var emails = mutableListOf<String>()

        // 从返回的URI查询基本信息（选中的电话号码和联系人ID）
        val cursor: Cursor? = resolver.query(
            phoneUri,
            arrayOf(
                Phone.CONTACT_ID,
                Phone.DISPLAY_NAME,
                Phone.NUMBER
            ),
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                contactId = it.getLong(it.getColumnIndexOrThrow(Phone.CONTACT_ID))
                name = it.getString(it.getColumnIndexOrThrow(Phone.DISPLAY_NAME)) ?: ""
                selectedPhone = it.getString(it.getColumnIndexOrThrow(Phone.NUMBER)) ?: ""
            }
        }

        if (contactId == -1L) {
            return null
        }

        // 获取该联系人的所有电话号码
        phoneNumbers = getPhoneNumbers(resolver, contactId).toMutableList()

        // 如果选中的号码不在列表中，添加它
        if (selectedPhone.isNotBlank() && !phoneNumbers.contains(selectedPhone)) {
            phoneNumbers.add(0, selectedPhone)
        }

        // 获取邮箱
        emails = getEmails(resolver, contactId).toMutableList()

        return ContactPickInfo(
            id = contactId,
            name = name,
            phoneNumbers = phoneNumbers,
            emails = emails
        )
    }

    /**
     * 通知选择取消
     */
    fun notifyCancelled() {
        pickerCallback?.complete("""{"success": false, "error": "User cancelled", "cancelled": true}""")
        clearCallback()
    }

    /**
     * 获取联系人的电话号码
     */
    private fun getPhoneNumbers(resolver: ContentResolver, contactId: Long): List<String> {
        val numbers = mutableListOf<String>()
        val cursor: Cursor? = resolver.query(
            Phone.CONTENT_URI,
            arrayOf(Phone.NUMBER, Phone.TYPE),
            "${Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val number = it.getString(it.getColumnIndexOrThrow(Phone.NUMBER))
                if (number.isNotBlank()) {
                    numbers.add(number)
                }
            }
        }

        return numbers
    }

    /**
     * 获取联系人的邮箱
     */
    private fun getEmails(resolver: ContentResolver, contactId: Long): List<String> {
        val emails = mutableListOf<String>()
        val cursor: Cursor? = resolver.query(
            Email.CONTENT_URI,
            arrayOf(Email.ADDRESS),
            "${Email.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val email = it.getString(it.getColumnIndexOrThrow(Email.ADDRESS))
                if (email.isNotBlank()) {
                    emails.add(email)
                }
            }
        }

        return emails
    }

    /**
     * 构建联系人JSON
     */
    private fun buildContactJson(contact: ContactPickInfo): String {
        return buildString {
            append("{\n")
            append("    \"success\": true,\n")
            append("    \"id\": ${contact.id},\n")
            append("    \"name\": \"${escapeJson(contact.name)}\",\n")
            append("    \"phoneNumbers\": [")
            contact.phoneNumbers.forEachIndexed { index, phone ->
                append("\"${escapeJson(phone)}\"")
                if (index < contact.phoneNumbers.size - 1) {
                    append(", ")
                }
            }
            append("],\n")
            append("    \"emails\": [")
            contact.emails.forEachIndexed { index, email ->
                append("\"${escapeJson(email)}\"")
                if (index < contact.emails.size - 1) {
                    append(", ")
                }
            }
            append("]\n")
            append("}")
        }
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
     * 联系人信息数据类
     */
    private data class ContactPickInfo(
        val id: Long,
        val name: String,
        val phoneNumbers: List<String>,
        val emails: List<String>
    )
}