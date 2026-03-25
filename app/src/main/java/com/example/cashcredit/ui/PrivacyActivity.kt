package com.example.cashcredit.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cashcredit.MainActivity
import com.example.cashcredit.R
import com.example.cashcredit.config.AppConfig
import com.example.cashcredit.util.LanguageUtil
import com.example.cashcredit.util.StatusBarUtil

/**
 * 隐私政策页面
 * 首次启动时显示，用户同意后才能进入主页面
 */
class PrivacyActivity : AppCompatActivity() {

    private lateinit var cbAgree: CheckBox
    private lateinit var btnAgree: Button
    private lateinit var tvTitle: TextView
    private lateinit var tvPrivacyContent: TextView

    private lateinit var btnSure: Button
    private lateinit var etUrl: EditText
    private lateinit var etApiUrl: EditText
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageUtil.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置沉浸式状态栏（深色文字，适合浅色背景）
        StatusBarUtil.setImmersiveStatusBar(this, true)
        setContentView(R.layout.activity_privacity)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        cbAgree = findViewById(R.id.cb_agree)
        btnAgree = findViewById(R.id.btn_agree)
        tvTitle = findViewById(R.id.tv_title)
        tvPrivacyContent = findViewById(R.id.tv_privacy_content)
        etUrl = findViewById<EditText>(R.id.et_url)
        btnSure = findViewById<Button>(R.id.btn_sure)
        etApiUrl = findViewById<EditText>(R.id.et_api_url)

        // 读取缓存的地址，如果有则填充，否则使用默认值
        val sp = getSharedPreferences(AppConfig.SP_NAME, Context.MODE_PRIVATE)
        val cachedH5Url = sp.getString(AppConfig.KEY_CACHED_H5_URL, null)
        val cachedApiUrl = sp.getString(AppConfig.KEY_CACHED_API_URL, null)

        etUrl.setText(cachedH5Url ?: AppConfig.H5_URL_TEST)
        etApiUrl.setText(cachedApiUrl ?: AppConfig.API_BASE_URL_TEST)

        // 设置隐私政策内容
        tvPrivacyContent.text = getPrivacyContent()
    }

    private fun setupListeners() {
        cbAgree.setOnCheckedChangeListener { _, isChecked ->
            btnAgree.isEnabled = isChecked
        }

        btnAgree.setOnClickListener {
            if (cbAgree.isChecked) {
                savePrivacyAgreed()
                startMainActivity()
            }
        }
        btnSure.setOnClickListener {
            if (etUrl.text.trim().isNullOrEmpty()) {
                Toast.makeText(this, "请输入网页地址", Toast.LENGTH_SHORT).show()
            } else if (etApiUrl.text.trim().isNullOrEmpty()) {
                Toast.makeText(this, "请输入接口地址", Toast.LENGTH_SHORT).show()
            } else {
                val h5Url = etUrl.text.trim().toString()
                val apiUrl = etApiUrl.text.trim().toString()

                // 缓存地址
                saveCachedUrls(h5Url, apiUrl)

                // 更新全局配置
                AppConfig.H5_URL_TEST = h5Url
                AppConfig.API_BASE_URL_TEST = apiUrl

                startMainActivity()
            }
        }

    }

    /**
     * 保存用户已同意隐私政策
     */
    private fun savePrivacyAgreed() {
        val sp = getSharedPreferences(AppConfig.SP_NAME, Context.MODE_PRIVATE)
        sp.edit().putBoolean(AppConfig.KEY_PRIVACY_AGREED, true).apply()
    }

    /**
     * 保存缓存的URL地址
     */
    private fun saveCachedUrls(h5Url: String, apiUrl: String) {
        val sp = getSharedPreferences(AppConfig.SP_NAME, Context.MODE_PRIVATE)
        sp.edit().apply {
            putString(AppConfig.KEY_CACHED_H5_URL, h5Url)
            putString(AppConfig.KEY_CACHED_API_URL, apiUrl)
            apply()
        }
    }

    /**
     * 跳转到主页面
     */
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * 获取隐私政策内容
     */
    private fun getPrivacyContent(): String {
        return if (LanguageUtil.isChinese()) {
            """
                隐私政策

                欢迎使用CashCredit应用。在使用我们的服务之前，请仔细阅读以下隐私政策。

                1. 信息收集
                我们可能收集您的设备信息（如设备型号、操作系统版本）以提供更好的服务。

                2. 信息使用
                收集的信息将用于：
                - 提供和改进服务
                - 技术支持
                - 安全防护

                3. 信息保护
                我们采取适当的安全措施保护您的个人信息。

                4. Cookie使用
                本应用使用WebView加载网页内容，相关网站可能使用Cookie。

                5. 第三方服务
                我们的服务可能包含第三方链接或服务。

                6. 隐私政策更新
                我们可能不时更新本隐私政策，请定期查阅。

                如有疑问，请联系我们。

                最后更新日期：2024年
            """.trimIndent()
        } else {
            """
                Privacy Policy

                Welcome to CashCredit. Before using our services, please read the following privacy policy carefully.

                1. Information Collection
                We may collect your device information (such as device model, operating system version) to provide better services.

                2. Information Use
                The collected information will be used for:
                - Providing and improving services
                - Technical support
                - Security protection

                3. Information Protection
                We take appropriate security measures to protect your personal information.

                4. Cookie Usage
                This application uses WebView to load web content, and related websites may use cookies.

                5. Third-party Services
                Our services may contain third-party links or services.

                6. Privacy Policy Updates
                We may update this privacy policy from time to time, please check regularly.

                If you have any questions, please contact us.

                Last updated: 2024
            """.trimIndent()
        }
    }

    companion object {
        /**
         * 检查用户是否已同意隐私政策
         */
        fun isPrivacyAgreed(context: Context): Boolean {
            val sp = context.getSharedPreferences(AppConfig.SP_NAME, Context.MODE_PRIVATE)
            return sp.getBoolean(AppConfig.KEY_PRIVACY_AGREED, false)
        }
    }
}