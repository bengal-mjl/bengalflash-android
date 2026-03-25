package com.example.cashcredit.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * 证件拍照遮罩视图
 * 在相机预览上显示一个框，用于引导用户对准证件
 */
class IdCardOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 框的类型
    enum class FrameType {
        ID_CARD,        // 身份证 (横向矩形)
        ID_CARD_BACK,   // 身份证背面
        FACE            // 人脸 (圆形头像)
    }

    // 画笔
    private val maskPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val framePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cornerPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val avatarPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)  // 头像轮廓画笔

    // 框的类型
    var frameType: FrameType = FrameType.ID_CARD
        set(value) {
            field = value
            invalidate()
        }

    // 框的矩形区域
    private var frameRect: RectF = RectF()

    // 人脸框的圆形半径
    private var faceCircleRadius: Float = 0f
    private var faceCircleCenterX: Float = 0f
    private var faceCircleCenterY: Float = 0f

    // 框的宽高比
    private var aspectRatio: Float = ID_CARD_RATIO

    // 框距离边缘的边距
    private val frameMarginHorizontal = 40f
    private val frameMarginTop = 120f

    // 角的长度
    private val cornerLength = 30f
    private val cornerWidth = 6f

    // 框的线宽
    private val frameStrokeWidth = 2f

    // 提示文字
    private var hintText: String = "请将证件对准框内"

    companion object {
        // 身份证宽高比 (约 85.6mm / 54mm ≈ 1.586)
        private const val ID_CARD_RATIO = 1.586f
        // 人脸框占屏幕宽度的比例
        private const val FACE_SIZE_RATIO = 0.85f
    }

    init {
        // 设置背景透明
        setLayerType(LAYER_TYPE_HARDWARE, null)

        // 遮罩画笔 - 半透明黑色
        maskPaint.apply {
            color = Color.parseColor("#80000000")
            style = Paint.Style.FILL
        }

        // 框线画笔 - 白色虚线
        framePaint.apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = frameStrokeWidth
            pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        }

        // 角落画笔 - 白色实线
        cornerPaint.apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = cornerWidth
            strokeCap = Paint.Cap.ROUND
        }

        // 头像轮廓画笔 - 白色实线，较粗
        avatarPaint.apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }

        // 文字画笔
        textPaint.apply {
            color = Color.WHITE
            textSize = 36f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 计算框的位置
        calculateFrameRect()

        // 绘制遮罩
        drawMask(canvas)

        // 绘制框
        drawFrame(canvas)

        // 绘制提示文字
        drawHintText(canvas)
    }

    /**
     * 计算框的位置和大小
     */
    private fun calculateFrameRect() {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        if (frameType == FrameType.FACE) {
            // 人脸框 - 圆形，占屏幕宽度的一定比例，位置靠上
            faceCircleRadius = viewWidth * FACE_SIZE_RATIO / 2
            faceCircleCenterX = viewWidth / 2
            // 圆形位置靠上，距离顶部约 1/4 屏幕高度
            faceCircleCenterY = viewHeight * 0.28f

            // 也设置frameRect用于兼容裁剪逻辑
            frameRect.set(
                faceCircleCenterX - faceCircleRadius,
                faceCircleCenterY - faceCircleRadius,
                faceCircleCenterX + faceCircleRadius,
                faceCircleCenterY + faceCircleRadius
            )
        } else {
            // 身份证框 - 根据屏幕方向计算合适的尺寸
            val isLandscape = viewWidth > viewHeight

            if (isLandscape) {
                // 横屏模式：基于屏幕高度计算框的高度，留出上下空间显示提示文案
                // 同时右边留出空间给拍照按钮
                val topBottomMargin = viewHeight * 0.12f  // 上下各留12%空间
                val rightMargin = viewWidth * 0.12f  // 右边留12%空间给拍照按钮

                val frameHeight = viewHeight - topBottomMargin * 2
                val frameWidth = frameHeight * aspectRatio

                // 确保框的宽度不超过可用宽度
                val availableWidth = viewWidth - rightMargin
                val finalWidth = minOf(frameWidth, availableWidth - frameMarginHorizontal * 2)
                val finalHeight = finalWidth / aspectRatio

                // 边框在左边可用区域内水平居中
                val left = (availableWidth - finalWidth) / 2
                val top = (viewHeight - finalHeight) / 2

                frameRect.set(left, top, left + finalWidth, top + finalHeight)
            } else {
                // 竖屏模式：基于屏幕宽度计算框的宽度
                val frameWidth = viewWidth - frameMarginHorizontal * 2
                val frameHeight = frameWidth / aspectRatio

                val left = frameMarginHorizontal
                val top = (viewHeight - frameHeight) / 2 - 50f

                frameRect.set(left, top, left + frameWidth, top + frameHeight)
            }
        }
    }

    /**
     * 绘制遮罩 (中间透明，周围半透明)
     */
    private fun drawMask(canvas: Canvas) {
        // 保存画布状态
        val saveCount = canvas.saveLayer(null, null)

        // 先绘制整个遮罩
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), maskPaint)

        // 使用CLEAR模式清除中间的框区域
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

        if (frameType == FrameType.FACE) {
            // 绘制圆形
            canvas.drawCircle(faceCircleCenterX, faceCircleCenterY, faceCircleRadius, maskPaint)
        } else {
            // 绘制圆角矩形
            canvas.drawRoundRect(frameRect, 16f, 16f, maskPaint)
        }

        // 恢复画笔
        maskPaint.xfermode = null

        // 恢复画布状态
        canvas.restoreToCount(saveCount)
    }

    /**
     * 绘制框和角
     */
    private fun drawFrame(canvas: Canvas) {
        if (frameType == FrameType.FACE) {
            // 绘制圆形头像框
            drawAvatarFrame(canvas)
        } else {
            // 绘制矩形框
            canvas.drawRoundRect(frameRect, 16f, 16f, framePaint)
            // 绘制四个角
            drawCorners(canvas)
        }
    }

    /**
     * 绘制圆形头像框
     */
    private fun drawAvatarFrame(canvas: Canvas) {
        // 只绘制圆形边框
        canvas.drawCircle(faceCircleCenterX, faceCircleCenterY, faceCircleRadius, avatarPaint)
    }

    /**
     * 绘制四个角
     */
    private fun drawCorners(canvas: Canvas) {
        val left = frameRect.left
        val top = frameRect.top
        val right = frameRect.right
        val bottom = frameRect.bottom

        // 左上角
        canvas.drawLine(left, top + cornerLength, left, top, cornerPaint)
        canvas.drawLine(left, top, left + cornerLength, top, cornerPaint)

        // 右上角
        canvas.drawLine(right - cornerLength, top, right, top, cornerPaint)
        canvas.drawLine(right, top, right, top + cornerLength, cornerPaint)

        // 左下角
        canvas.drawLine(left, bottom - cornerLength, left, bottom, cornerPaint)
        canvas.drawLine(left, bottom, left + cornerLength, bottom, cornerPaint)

        // 右下角
        canvas.drawLine(right - cornerLength, bottom, right, bottom, cornerPaint)
        canvas.drawLine(right, bottom, right, bottom - cornerLength, cornerPaint)
    }

    /**
     * 绘制提示文字
     */
    private fun drawHintText(canvas: Canvas) {
        val isLandscape = width > height

        if (frameType == FrameType.FACE) {
            val textY = faceCircleCenterY + faceCircleRadius + 60f
            canvas.drawText(hintText, width / 2f, textY, textPaint)
        } else if (isLandscape) {
            // 横屏模式：文字显示在边框下方居中
            val textX = frameRect.left + (frameRect.right - frameRect.left) / 2
            val textY = frameRect.bottom + 50f
            canvas.drawText(hintText, textX, textY, textPaint)
        } else {
            val textY = frameRect.bottom + 60f
            canvas.drawText(hintText, width / 2f, textY, textPaint)
        }
    }

    /**
     * 设置提示文字
     */
    fun setHintText(text: String) {
        hintText = text
        invalidate()
    }

    /**
     * 设置框类型
     */
    fun setFrameType(type: String) {
        when (type) {
            "IdFront", "ID_CARD_FRONT" -> {
                frameType = FrameType.ID_CARD
                hintText = "请将身份证人像面对准框内"
            }
            "IdBack", "ID_CARD_BACK" -> {
                frameType = FrameType.ID_CARD_BACK
                hintText = "请将身份证国徽面对准框内"
            }
            "FACE" -> {
                frameType = FrameType.FACE
                hintText = "请将人脸对准框内"
            }
            else -> {
                frameType = FrameType.ID_CARD
                hintText = "请将证件对准框内"
            }
        }
        invalidate()
    }

    /**
     * 获取框的矩形区域 (用于裁剪图片)
     */
    fun getFrameRect(): RectF = frameRect

    /**
     * 获取框的类型
     */
    fun getFrameTypeEnum(): FrameType = frameType

    /**
     * 获取边框相对于View的比例位置 (用于图片裁剪)
     * @return RectF 包含left, top, right, bottom的比例值 (0.0-1.0)
     */
    fun getFrameRectRatio(): RectF {
        if (width == 0 || height == 0) return RectF(0f, 0f, 1f, 1f)
        return RectF(
            frameRect.left / width,
            frameRect.top / height,
            frameRect.right / width,
            frameRect.bottom / height
        )
    }
}