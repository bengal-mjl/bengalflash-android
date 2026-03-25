package com.example.cashcredit.util

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 文件工具类
 */
object FileUtils {

    private const val IMAGE_PREFIX = "IMG_"
    private const val IMAGE_SUFFIX = ".jpg"
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * 创建图片文件
     */
    fun createImageFile(context: Context): File {
        val timeStamp = dateFormat.format(Date())
        val fileName = "$IMAGE_PREFIX$timeStamp$IMAGE_SUFFIX"

        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: context.filesDir

        return File(storageDir, fileName)
    }

    /**
     * 创建临时图片文件
     */
    fun createTempImageFile(context: Context): File {
        val timeStamp = dateFormat.format(Date())
        val fileName = "TEMP_$timeStamp$IMAGE_SUFFIX"

        val storageDir = context.cacheDir
        return File(storageDir, fileName)
    }

    /**
     * 获取缓存目录
     */
    fun getCacheDir(context: Context): File {
        return context.cacheDir
    }

    /**
     * 获取文件目录
     */
    fun getFilesDir(context: Context): File {
        return context.filesDir
    }

    /**
     * 删除文件
     */
    fun deleteFile(file: File?): Boolean {
        return file?.delete() ?: false
    }

    /**
     * 删除目录及其内容
     */
    fun deleteDirectory(directory: File?): Boolean {
        if (directory == null || !directory.exists()) {
            return false
        }

        if (directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    deleteDirectory(file)
                } else {
                    file.delete()
                }
            }
        }

        return directory.delete()
    }

    /**
     * 获取文件大小
     */
    fun getFileSize(file: File?): Long {
        return file?.length() ?: 0
    }

    /**
     * 格式化文件大小
     */
    fun formatFileSize(size: Long): String {
        if (size < 1024) {
            return "$size B"
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0)
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024))
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024))
        }
    }

    /**
     * 检查文件是否存在
     */
    fun fileExists(path: String?): Boolean {
        return path?.let { File(it).exists() } ?: false
    }

    /**
     * 获取文件扩展名
     */
    fun getFileExtension(fileName: String?): String {
        if (fileName.isNullOrEmpty()) return ""
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot >= 0) fileName.substring(lastDot + 1) else ""
    }
}