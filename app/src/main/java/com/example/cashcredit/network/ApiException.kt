package com.example.cashcredit.network

/**
 * API异常封装类
 * 用于统一处理网络请求过程中出现的各种异常
 */
sealed class ApiException : Exception() {

    /**
     * HTTP错误 (如 400, 404 等)
     */
    data class HttpError(
        val httpCode: Int,
        override val message: String
    ) : ApiException()

    /**
     * 网络错误 (如无网络连接、连接超时等)
     */
    data class NetworkError(
        val throwable: Throwable
    ) : ApiException() {
        override val message: String = throwable.message ?: "网络连接失败"
    }

    /**
     * 服务器错误 (如 500)
     */
    data class ServerError(
        val serverCode: Int,
        override val message: String
    ) : ApiException()

    /**
     * Token过期
     */
    data object TokenExpired : ApiException() {
        override val message: String = "登录已过期，请重新登录"
    }

    /**
     * 业务错误 (服务器返回的业务状态码非200)
     */
    data class BusinessError(
        val bizCode: Int,
        override val message: String
    ) : ApiException()

    /**
     * 数据解析错误
     */
    data class ParseError(
        val throwable: Throwable
    ) : ApiException() {
        override val message: String = "数据解析失败"
    }

    /**
     * 未知错误
     */
    data class UnknownError(
        val throwable: Throwable?
    ) : ApiException() {
        override val message: String = throwable?.message ?: "未知错误"
    }

    /**
     * 获取错误码
     */
    fun getErrorCode(): Int {
        return when (this) {
            is HttpError -> httpCode
            is ServerError -> serverCode
            is BusinessError -> bizCode
            is TokenExpired -> ApiResponse.Code.TOKEN_EXPIRED
            is NetworkError -> -1
            is ParseError -> -2
            is UnknownError -> -3
        }
    }
}