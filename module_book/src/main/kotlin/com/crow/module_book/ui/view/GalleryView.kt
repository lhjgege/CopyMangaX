package com.crow.module_book.ui.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.crow.base.tools.extensions.logMsg
import com.crow.module_book.R
import kotlin.math.max
import kotlin.math.min

/**
 * Created by Avalon on 2016/6/22.
 */
/**
 * Created by Avalon on 2016/6/22.
 */
class GalleryView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : RecyclerView(context, attrs, defStyle) {

    companion object {
        private val TAG = GalleryView::class.java.simpleName

        // 非法的手指ID
        private const val INVALID_POINTER_ID = -1
    }

    private val mContext: Context

    // 第一根按下的手指的ID,进行拖动事件处理
    private var mMainPointerId = INVALID_POINTER_ID

    // 缩放因子
    var mScaleFactor = 0f

    // 上次触摸点坐标
    private var mLastDownX = 0f
    private var mLastDownY = 0f

    // canvas的偏移量
    private var mDeltaX = 0f
    private var mDeltaY = 0f

    // 缩放中心
    private var centerX = 0f
    private var centerY = 0f

    // 缩放因子
    var minScaleFactor = 1.0f
    var midScaleFactor = minScaleFactor * 2
    var maxScaleFactor = minScaleFactor * 4

    // 双击自动缩放
    private var isAutoScale = false
    var autoTime = 5
    var autoBigger = 1.07f
    var autoSmall = 0.93f

    // 单击、双击手势
    private var mGestureDetector: GestureDetector? = null

    // 缩放手势
    private var mScaleGestureDetector: ScaleGestureDetector? = null

    // 开放监听接口
    var onGestureListener: OnGestureListener? = null

    interface OnGestureListener {
        fun onScale(detector: ScaleGestureDetector?): Boolean
        fun onSingleTapConfirmed(e: MotionEvent?): Boolean
        fun onDoubleTap(e: MotionEvent?): Boolean
    }

    // 自动缩放的核心类
    private inner class AutoScaleRunnable(
        // 目标Scale
        private val mTargetScale: Float, x: Float, y: Float,
        // Scale变化梯度
        private val mGrad: Float
    ) : Runnable {
        /** 缩放中心  */
        private val x = 0f
        private val y = 0f
        override fun run() {
            if (mGrad > 1.0f && mScaleFactor < mTargetScale || mGrad < 1.0f && mScaleFactor > mTargetScale) {
                mScaleFactor *= mGrad
                postDelayed(this, autoTime.toLong())
            } else {
                mScaleFactor = mTargetScale
            }
            /** 检查边界  */
            checkBorder()
            invalidate()
        }
    }

    inner class SupportLinearLayoutManager(
        context: Context?,
        orientation: Int,
        reverseLayout: Boolean
    ) : LinearLayoutManager(context, orientation, reverseLayout) {
        override fun scrollVerticallyBy(dy: Int, recycler: Recycler, state: State): Int {
            /** 控制缩放后上下滑动的速度  */
            val result = super.scrollVerticallyBy((dy / mScaleFactor + 0.5).toInt(), recycler, state)
            return if (result == (dy / mScaleFactor + 0.5).toInt()) {
                dy
            } else result
        }
    }

    init {
        mContext = context
        layoutManager = SupportLinearLayoutManager(context, VERTICAL, false)
        obtainStyledAttributes(attrs)
        initView()
        initDetector()
    }

    /** 从XML文件获取属性  */
    private fun obtainStyledAttributes(attrs: AttributeSet?) {
        val attr = mContext.obtainStyledAttributes(attrs, R.styleable.GalleryView)
        minScaleFactor = attr.getDimension(R.styleable.GalleryView_minScaleFactor, 1.0f)
        maxScaleFactor = attr.getFloat(R.styleable.GalleryView_maxScaleFactor, minScaleFactor * 4)
        autoTime = attr.getInt(R.styleable.GalleryView_autoScaleTime, 5)
        attr.recycle()
    }

    /** 初始化View  */
    private fun initView() {

        // 中比例缩放银子
        midScaleFactor = (minScaleFactor + maxScaleFactor) / 2

        mScaleFactor = minScaleFactor

        isAutoScale = false
    }

    /** 初始化手势监听  */
    private fun initDetector() {
        mScaleGestureDetector = ScaleGestureDetector(mContext, object : SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                // 获取缩放中心
                centerX = detector.focusX
                centerY = detector.focusY

                // 缩放
                mScaleFactor *= detector.scaleFactor
                mScaleFactor = max(minScaleFactor, min(mScaleFactor, maxScaleFactor))

                /** 缩放导致偏移  */
                //mDeltaX += centerX * (mScaleFactor - lastScaleFactor);
               // mDeltaY += centerY * (mScaleFactor - lastScaleFactor);
//                checkBorder();//检查边界
                invalidate()
                if (onGestureListener != null) {
                    onGestureListener!!.onScale(detector)
                }
                return true
            }
        })
        mGestureDetector = GestureDetector(mContext, object : SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                return if (onGestureListener != null) { onGestureListener!!.onSingleTapConfirmed(e) } else false
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (isAutoScale) {
                    return true
                }
//                centerX = e.getX();
//                centerY = e.getY();
                centerX = 0f
                centerY = 0f
                if (mScaleFactor < midScaleFactor) {
                    postDelayed(
                        AutoScaleRunnable(midScaleFactor, centerX, centerY, autoBigger),
                        autoTime.toLong()
                    )
                } else if (mScaleFactor < maxScaleFactor) {
                    postDelayed(
                        AutoScaleRunnable(maxScaleFactor, centerX, centerY, autoBigger),
                        autoTime.toLong()
                    )
                } else {
                    postDelayed(
                        AutoScaleRunnable(minScaleFactor, centerX, centerY, autoSmall),
                        autoTime.toLong()
                    )
                }
                if (onGestureListener != null) {
                    onGestureListener!!.onDoubleTap(e)
                }
                return true
            }
        })
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.save()
        if (mScaleFactor == 1.0f) {
            mDeltaX = 0.0f
            mDeltaY = 0.0f
        }
        "scaleFactor : $mScaleFactor \t mDeltaX : $mDeltaX \t mDeltaY : $mDeltaY".logMsg(tag = "CustomRv")
        canvas.translate(mDeltaX, mDeltaY)
        canvas.scale(mScaleFactor, mScaleFactor, centerX, centerY);
        // canvas.scale(scaleFactor, scaleFactor)
        super.dispatchDraw(canvas)
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)

        /** 单击、双击事件的处理  */
        if (mGestureDetector!!.onTouchEvent(event)) {
            mMainPointerId = event.getPointerId(0) //防止发生手势事件后,mActivePointerId=-1的情况
            return true
        }

        /** 缩放事件的处理  */
        mScaleGestureDetector!!.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mLastDownX = event.x
                mLastDownY = event.y
                mMainPointerId = event.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                val mainPointIndex = event.findPointerIndex(mMainPointerId)
                val mainPointX = event.getX(mainPointIndex)
                val mainPointY = event.getY(mainPointIndex)
                /** 计算与上次坐标的偏移量并累加  */
                mDeltaX += mainPointX - mLastDownX
                mDeltaY += mainPointY - mLastDownY
                /** 保存坐标  */
                mLastDownX = mainPointX
                mLastDownY = mainPointY
                /** 检查边界  */
                checkBorder()
                invalidate()
            }
            MotionEvent.ACTION_UP -> mMainPointerId = INVALID_POINTER_ID
            MotionEvent.ACTION_CANCEL -> mMainPointerId = INVALID_POINTER_ID
            MotionEvent.ACTION_POINTER_UP -> {
                /** 获取抬起手指  */
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == mMainPointerId) {
                    /** 抬起手指是主手指,则寻找另一根手指 */
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    mLastDownX = event.getX(newPointerIndex)
                    mLastDownY = event.getY(newPointerIndex)
                    mMainPointerId = event.getPointerId(newPointerIndex)
                }
            }
        }

        return true
    }

    // 检查边界
    private fun checkBorder() {
        // 左边界
        if (mDeltaX > 0.0f) mDeltaX = 0.0f

        // 右边界
        if (-mDeltaX > width * (mScaleFactor - 1.0f)) mDeltaX = -width * (mScaleFactor - 1.0f)

        // 上边界
        if (mDeltaY > 0.0f) mDeltaY = 0.0f

        // 下边界
        if (-mDeltaY > height * (mScaleFactor - 1.0f)) mDeltaY = -height * (mScaleFactor - 1.0f)
    }
}