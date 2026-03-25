package com.example.cashcredit.webview

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import com.example.cashcredit.util.AppDeviceInfo
import wendu.dsbridge.DWebView

/**
 * DWebView管理类
 * 使用DSBridge的DWebView，支持双向通信
 */
class DWebViewManager(
    private val context: Context,
    private val dWebView: DWebView,
    private val progressBar: ProgressBar? = null
) {

    companion object {
        private const val TAG = "DWebViewManager"

        /**
         * H5定义的接收设备信息的JS方法名
         * H5需要在window对象上定义此方法
         */
        const val JS_METHOD_RECEIVE_DEVICE_INFO = "receiveNativeDeviceInfo"
    }

    /**
     * 初始化WebView配置
     */
    fun init() {
        configureSettings()
        configureWebViewClient()
        configureWebChromeClient()
        configureDownloadListener()
        configureCookieManager()
        // DWebView内部已处理JS Bridge，无需额外配置
    }

    /**
     * 配置WebView设置
     */
    private fun configureSettings() {
        dWebView.settings.apply {
            // 启用JavaScript
            javaScriptEnabled = true

            // 启用DOM存储
            domStorageEnabled = true

            // 启用数据库存储
            databaseEnabled = true

            // 设置缓存模式
            cacheMode = WebSettings.LOAD_DEFAULT

            // 允许混合内容（HTTP和HTTPS）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            // 支持缩放
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false

            // 自适应屏幕
            useWideViewPort = true
            loadWithOverviewMode = true

            // 设置布局
            layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING

            // 设置编码
            defaultTextEncodingName = "UTF-8"

            // 允许文件访问
            allowFileAccess = true
            allowContentAccess = true

            // 禁止通过file协议加载的页面访问本地文件
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
            }
        }
    }

    /**
     * 配置WebViewClient
     */
    private fun configureWebViewClient() {
        dWebView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false

                // 处理tel:和mailto:等特殊协议
                when {
                    url.startsWith("tel:") -> {
                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
                        intent.data = Uri.parse(url)
                        context.startActivity(intent)
                        return true
                    }
                    url.startsWith("mailto:") -> {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO)
                        intent.data = Uri.parse(url)
                        context.startActivity(intent)
                        return true
                    }
                }

                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar?.visibility = View.GONE

                // H5页面加载完成后，推送设备信息给H5
                pushDeviceInfoToH5()
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: android.net.http.SslError?
            ) {
                // 生产环境建议不要忽略SSL错误
                handler?.proceed()
            }
        }
    }

    /**
     * 配置WebChromeClient
     */
    private fun configureWebChromeClient() {
        dWebView.webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar?.apply {
                    progress = newProgress
                    visibility = if (newProgress == 100) View.GONE else View.VISIBLE
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: android.webkit.ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                return super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
            }
        }
    }

    /**
     * 配置下载监听
     */
    private fun configureDownloadListener() {
        dWebView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(url), mimeType)
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 配置Cookie管理器
     */
    private fun configureCookieManager() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(dWebView, true)
        }
    }

    /**
     * 加载URL
     */
    fun loadUrl(url: String) {
        dWebView.loadUrl(url)
    }

    /**
     * 加载HTML内容
     */
    fun loadData(data: String, mimeType: String = "text/html", encoding: String = "UTF-8") {
        dWebView.loadData(data, mimeType, encoding)
    }

    /**
     * 加载HTML内容带Base URL
     */
    fun loadDataWithBaseURL(baseUrl: String?, data: String, mimeType: String = "text/html", encoding: String = "UTF-8", historyUrl: String? = null) {
        dWebView.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl)
    }

    /**
     * 刷新页面
     */
    fun reload() {
        dWebView.reload()
    }

    /**
     * 返回上一页
     * @return true表示已返回上一页，false表示已经是第一页
     */
    fun goBack(): Boolean {
        return if (dWebView.canGoBack()) {
            dWebView.goBack()
            true
        } else {
            false
        }
    }

    /**
     * 是否可以返回
     */
    fun canGoBack(): Boolean {
        return dWebView.canGoBack()
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        dWebView.clearCache(true)
        CookieManager.getInstance().removeAllCookies(null)
    }

    /**
     * 清除历史记录
     */
    fun clearHistory() {
        dWebView.clearHistory()
    }

    /**
     * 销毁WebView
     */
    fun destroy() {
        dWebView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
        dWebView.clearHistory()
        dWebView.destroy()
    }

    /**
     * 获取DWebView实例（用于注册JS接口）
     */
    fun getDWebView(): DWebView = dWebView

    /**
     * 推送设备信息给H5
     * 在页面加载完成后自动调用
     */
    private fun pushDeviceInfoToH5() {
        try {
            if (!AppDeviceInfo.isInitialized()) {
                Log.w(TAG, "AppDeviceInfo not initialized, skipping device info push")
                return
            }

            val deviceInfoJson = AppDeviceInfo.getDeviceInfoJson()
            Log.d(TAG, "Pushing device info to H5: $deviceInfoJson")

            // 调用H5定义的JS方法
            // H5需要在window对象上定义 receiveNativeDeviceInfo 方法
            val jsCode = "javascript:if(typeof window.$JS_METHOD_RECEIVE_DEVICE_INFO === 'function')" +
                    "{window.$JS_METHOD_RECEIVE_DEVICE_INFO($deviceInfoJson)}"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                dWebView.evaluateJavascript(jsCode) { result ->
                    Log.d(TAG, "JS call result: $result")
                }
            } else {
                dWebView.loadUrl(jsCode)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push device info to H5", e)
        }
    }

    /**
     * 手动推送设备信息给H5
     * 供外部调用，如需要重新推送设备信息时
     */
    fun pushDeviceInfo() {
        pushDeviceInfoToH5()
    }
}