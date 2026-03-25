package com.example.cashcredit.network

/**
 * 统一API响应数据模型
 * @param code 业务状态码
 * @param message 提示信息
 * @param data 业务数据
 */
data class ApiResponse<T>(
    val code: Int = 0,
    val message: String = "",
    val data: T? = null
) {
    /**
     * 判断请求是否成功
     */
    fun isSuccess(): Boolean = code == Code.SUCCESS

    /**
     * 获取数据，如果为空则抛出异常
     */
    fun requireData(): T {
        return data ?: throw NullPointerException("Response data is null")
    }

    /**
     * 业务状态码常量
     */
    object Code {
        const val SUCCESS = 200
        const val TOKEN_EXPIRED = 401
        const val FORBIDDEN = 403
        const val SERVER_ERROR = 500
    }
}