package com.example.cashcredit.network.api

import com.example.cashcredit.model.UploadDeviceResponse
import com.example.cashcredit.network.ApiResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API接口定义示例
 * 所有API接口都在这里定义
 */
interface ApiInterface {

    // ==================== 示例接口 ====================

    /**
     * 获取用户信息
     */
    @GET("api/user/info")
    suspend fun getUserInfo(): ApiResponse<UserInfo>

    /**
     * 获取用户详情
     * @param userId 用户ID
     */
    @GET("api/user/{userId}")
    suspend fun getUserDetail(@Path("userId") userId: String): ApiResponse<UserDetail>

    /**
     * 更新用户信息
     * @param request 更新请求体
     */
    @POST("api/user/update")
    suspend fun updateUser(@Body request: UpdateUserRequest): ApiResponse<Unit>

    /**
     * 获取列表数据
     * @param page 页码
     * @param size 每页数量
     */
    @GET("api/list")
    suspend fun getList(
        @Query("page") page: Int,
        @Query("size") size: Int = 20
    ): ApiResponse<ListData>

    // ==================== 登录相关 ====================

    /**
     * 登录 (H5调用)
     * @param request 登录请求
     */
    @POST("api/web/login/login")
    suspend fun webLogin(@Body request: WebLoginRequest): ApiResponse<WebLoginResponse>

    /**
     * 登录
     * @param request 登录请求
     */
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    /**
     * 登出
     */
    @POST("api/auth/logout")
    suspend fun logout(): ApiResponse<Unit>

    /**
     * 刷新Token
     * @param refreshToken 刷新令牌
     */
    @POST("api/auth/refresh")
    suspend fun refreshToken(@Query("refreshToken") refreshToken: String): ApiResponse<LoginResponse>

    // ==================== 图片上传 ====================

    /**
     * 上传图片
     * @param request 上传请求
     */
    @POST("api/web/customer/upload-image")
    suspend fun uploadImage(@Body request: UploadImageRequest): ApiResponse<UploadImageResponse>

    // ==================== 设备数据上传 ====================

    /**
     * 上传设备信息
     * @param sceneType 场景类型
     * @param deviceJson 设备信息JSON文件
     */
    @Multipart
    @POST("api/web/customer/device/upload")
    suspend fun uploadDevice(
        @Part("sceneType") sceneType: RequestBody,
        @Part deviceJson: MultipartBody.Part
    ): ApiResponse<UploadDeviceResponse>

    // ==================== 数据模型示例 ====================

    /**
     * 用户信息
     */
    data class UserInfo(
        val id: String,
        val name: String,
        val avatar: String?,
        val phone: String?
    )

    /**
     * 用户详情
     */
    data class UserDetail(
        val id: String,
        val name: String,
        val avatar: String?,
        val phone: String?,
        val email: String?,
        val createTime: Long
    )

    /**
     * 更新用户请求
     */
    data class UpdateUserRequest(
        val name: String?,
        val avatar: String?
    )

    /**
     * 列表数据
     */
    data class ListData(
        val items: List<Item>,
        val total: Int,
        val page: Int,
        val size: Int
    ) {
        data class Item(
            val id: String,
            val title: String,
            val content: String?
        )
    }

    /**
     * 登录请求
     */
    data class LoginRequest(
        val phone: String,
        val password: String
    )

    /**
     * 登录响应
     */
    data class LoginResponse(
        val token: String,
        val refreshToken: String,
        val expireTime: Long,
        val userInfo: UserInfo
    )

    /**
     * H5登录请求
     */
    data class WebLoginRequest(
        val mobile: String,
        val password: String
    )

    /**
     * H5登录响应
     */
    data class WebLoginResponse(
        val token: String
    )

    /**
     * 图片上传请求
     * @param fileBase64Str 图片文件base64编码字符串
     * @param imageType 图片类型: ID_CARD_FRONT, ID_CARD_BACK, FACE
     */
    data class UploadImageRequest(
        val fileBase64Str: String,
        val imageType: String
    )

    /**
     * 图片上传响应
     */
    data class UploadImageResponse(
        val imageUrl: String,
        val httpImageUrl: String
    )
}