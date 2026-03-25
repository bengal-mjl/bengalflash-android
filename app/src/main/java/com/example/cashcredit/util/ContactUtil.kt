package com.example.cashcredit.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Email
import androidx.core.content.ContextCompat

/**
 * 联系人工具类
 * 用于读取手机联系人信息
 */
object ContactUtil {

    /**
     * 检查是否有联系人读取权限
     */
    fun hasContactPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 获取联系人列表
     * @param context 上下文
     * @param maxCount 最大获取数量，默认50
     * @return JSON格式字符串
     */
    fun getContactListJson(context: Context, maxCount: Int = 50): String {
        return try {
            if (!hasContactPermission(context)) {
                return """{"success": false, "error": "Permission denied", "code": "PERMISSION_DENIED", "count": 0, "contacts": []}"""
            }

            val contacts = getContactList(context, maxCount)
            buildContactsJson(contacts)
        } catch (e: Exception) {
            """{"success": false, "error": "${e.message}", "count": 0, "contacts": []}"""
        }
    }

    /**
     * 读取联系人列表
     */
    private fun getContactList(context: Context, maxCount: Int): List<ContactInfo> {
        val contactsList = mutableListOf<ContactInfo>()
        val contentResolver = context.contentResolver

        // 查询联系人
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.HAS_PHONE_NUMBER
            ),
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )

        cursor?.use {
            var count = 0
            while (it.moveToNext() && count < maxCount) {
                val id = it.getLong(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)) ?: "Unknown"
                val hasPhoneNumber = it.getInt(it.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0

                // 获取电话号码
                val phoneNumbers = getPhoneNumbers(contentResolver, id)

                // 获取邮箱
                val emails = getEmails(contentResolver, id)

                contactsList.add(
                    ContactInfo(
                        id = id,
                        name = name,
                        phoneNumbers = phoneNumbers,
                        emails = emails
                    )
                )
                count++
            }
        }

        return contactsList
    }

    /**
     * 获取联系人的电话号码
     */
    private fun getPhoneNumbers(contentResolver: android.content.ContentResolver, contactId: Long): List<String> {
        val numbers = mutableListOf<String>()
        val cursor: Cursor? = contentResolver.query(
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
    private fun getEmails(contentResolver: android.content.ContentResolver, contactId: Long): List<String> {
        val emails = mutableListOf<String>()
        val cursor: Cursor? = contentResolver.query(
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
     * 构建JSON字符串
     */
    private fun buildContactsJson(contacts: List<ContactInfo>): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("{\n")
        stringBuilder.append("    \"success\": true,\n")
        stringBuilder.append("    \"count\": ${contacts.size},\n")
        stringBuilder.append("    \"contacts\": [\n")

        contacts.forEachIndexed { index, contact ->
            stringBuilder.append("        {\n")
            stringBuilder.append("            \"id\": ${contact.id},\n")
            stringBuilder.append("            \"name\": \"${escapeJson(contact.name)}\",\n")
            stringBuilder.append("            \"phoneNumbers\": [")
            contact.phoneNumbers.forEachIndexed { phoneIndex, phone ->
                stringBuilder.append("\"${escapeJson(phone)}\"")
                if (phoneIndex < contact.phoneNumbers.size - 1) {
                    stringBuilder.append(", ")
                }
            }
            stringBuilder.append("],\n")
            stringBuilder.append("            \"emails\": [")
            contact.emails.forEachIndexed { emailIndex, email ->
                stringBuilder.append("\"${escapeJson(email)}\"")
                if (emailIndex < contact.emails.size - 1) {
                    stringBuilder.append(", ")
                }
            }
            stringBuilder.append("]\n")
            stringBuilder.append("        }")
            if (index < contacts.size - 1) {
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
     * 联系人信息数据类
     */
    private data class ContactInfo(
        val id: Long,
        val name: String,
        val phoneNumbers: List<String>,
        val emails: List<String>
    )
}