package com.example.cashcredit.network

import android.content.Context
import android.util.Log
import com.example.cashcredit.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit单例网络客户端
 * 提供统一的网络请求入口
 */
object RetrofitClient {

    private const val TAG = "RetrofitClient"

    // 默认超时配置
    private const val DEFAULT_CONNECT_TIMEOUT = 30L
    private const val DEFAULT_READ_TIMEOUT = 30L
    private const val DEFAULT_WRITE_TIMEOUT = 30L

    // 基础URL配置 (API地址，不是H5页面地址)
    var baseUrl: String = AppConfig.getApiBaseUrl()
        set(value) {
            field = value
            // 如果URL发生变化，需要重新创建Retrofit实例
            if (::retrofit.isInitialized) {
                retrofit = createRetrofit()
                // 清除API接口缓存，下次调用时会重新创建
                apiCache.clear()
            }
        }

    private lateinit var okHttpClient: OkHttpClient
    private lateinit var retrofit: Retrofit
    private lateinit var appContext: Context

    // API接口缓存，实现单例
    private val apiCache = mutableMapOf<Class<*>, Any>()

    // 是否为Debug模式
    var isDebugMode: Boolean = true
        set(value) {
            field = value
            // 重新创建OkHttpClient以更新日志级别
            if (::okHttpClient.isInitialized) {
                okHttpClient = createOkHttpClient()
            }
        }

    /**
     * 初始化网络客户端
     * 应在Application中调用
     */
    fun init(context: Context, debug: Boolean = true) {
        appContext = context.applicationContext
        isDebugMode = debug
        TokenManager.init(context)

        okHttpClient = createOkHttpClient()
        retrofit = createRetrofit()
    }

    /**
     * 创建OkHttpClient
     */
    private fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_WRITE_TIMEOUT, TimeUnit.SECONDS)

        // 添加请求头拦截器
        // 注意: 需要在ResponseInterceptor之前添加，确保请求头先被处理
        builder.addInterceptor(ApiInterceptor(getAppContext()))

        // 添加响应拦截器
        builder.addInterceptor(ResponseInterceptor())

        // 添加日志拦截器
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }
        loggingInterceptor.level = if (isDebugMode) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.BASIC
        }
        builder.addInterceptor(loggingInterceptor)

        return builder.build()
    }

    /**
     * 创建Retrofit实例
     */
    private fun createRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * 获取AppContext
     */
    private fun getAppContext(): Context {
        check(::appContext.isInitialized) { "RetrofitClient未初始化，请先调用init()方法" }
        return appContext
    }

    /**
     * 创建API服务接口实例（单例）
     * @param service API接口类
     * @return API接口实例
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> create(service: Class<T>): T {
        check(::retrofit.isInitialized) { "RetrofitClient未初始化，请先调用init()方法" }

        // 从缓存获取，如果不存在则创建并缓存
        val cached = apiCache[service]
        return if (cached != null) {
            cached as T
        } else {
            val instance = retrofit.create(service)
            apiCache[service] = instance as Any
            instance
        }
    }

    /**
     * 获取ApiInterface单例实例
     * 便捷方法，无需每次传递Class参数
     */
    val api: com.example.cashcredit.network.api.ApiInterface
        get() = create(com.example.cashcredit.network.api.ApiInterface::class.java)

    /**
     * 执行请求并返回Flow
     * 自动处理错误和线程切换
     */
    fun <T> request(block: suspend () -> ApiResponse<T>): Flow<Result<T>> = flow {
        val response = block()
        if (response.isSuccess()) {
            val data = response.data
            if (data != null) {
                emit(Result.success(data))
            } else {
                emit(Result.failure(NullPointerException("Response data is null")))
            }
        } else {
            val exception = when (response.code) {
                ApiResponse.Code.TOKEN_EXPIRED -> ApiException.TokenExpired
                else -> ApiException.BusinessError(response.code, response.message)
            }
            emit(Result.failure(exception))
        }
    }.catch { e ->
        val exception = when (e) {
            is ApiException -> e
            is retrofit2.HttpException -> {
                ApiException.HttpError(e.code(), e.message())
            }
            is java.net.UnknownHostException -> {
                ApiException.NetworkError(e)
            }
            is java.net.SocketTimeoutException -> {
                ApiException.NetworkError(e)
            }
            is java.io.IOException -> {
                ApiException.NetworkError(e)
            }
            is com.google.gson.JsonSyntaxException -> {
                ApiException.ParseError(e)
            }
            else -> ApiException.UnknownError(e)
        }
        emit(Result.failure(exception))
    }.flowOn(Dispatchers.IO)
}

/**
 * Flow扩展函数：处理API响应
 * 使用示例:
 * ```
 * repository.getData()
 *     .handleResponse()
 *     .collect { result ->
 *         result.onSuccess { data -> ... }
 *               .onFailure { error -> ... }
 *     }
 * ```
 */
fun <T> Flow<ApiResponse<T>>.handleResponse(): Flow<Result<T>> = flow {
    collect { response ->
        if (response.isSuccess()) {
            val data = response.data
            if (data != null) {
                emit(Result.success(data))
            } else {
                emit(Result.failure(NullPointerException("Response data is null")))
            }
        } else {
            val exception = when (response.code) {
                ApiResponse.Code.TOKEN_EXPIRED -> ApiException.TokenExpired
                else -> ApiException.BusinessError(response.code, response.message)
            }
            emit(Result.failure(exception))
        }
    }
}.catch { e ->
    val exception = when (e) {
        is ApiException -> e
        is retrofit2.HttpException -> ApiException.HttpError(e.code(), e.message())
        is java.net.UnknownHostException -> ApiException.NetworkError(e)
        is java.net.SocketTimeoutException -> ApiException.NetworkError(e)
        is java.io.IOException -> ApiException.NetworkError(e)
        is com.google.gson.JsonSyntaxException -> ApiException.ParseError(e)
        else -> ApiException.UnknownError(e)
    }
    emit(Result.failure(exception))
}