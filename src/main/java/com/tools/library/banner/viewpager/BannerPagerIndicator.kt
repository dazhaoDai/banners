package com.tools.library.banner.viewpager

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout
import com.tools.library.banner.R
import com.tools.library.banner.dp

class BannerPagerIndicator @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), IBannerPagerIndicator {


    private val rectFList = arrayListOf<RectF>()

    private var currentItem = 0
    private var indicatorWidth: Float = 0f
    private var indicatorHeight: Float = 0f
    private var indicatorRadius: Float = 0f
    private var indicatorSpacing: Float = 0f
    private var indicatorColor: Int  = 0
    private var indicatorSelectColor: Int  = 0
    private var count: Int = 0

    private val normalPaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
    }

    private val selectPaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
    }

    init {
        setWillNotDraw(false)
        initAttrs(attrs)
        normalPaint.color = indicatorColor
        selectPaint.color = indicatorSelectColor
    }

    private fun initAttrs(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BannerPagerIndicator)
        indicatorWidth = typedArray.getDimension(R.styleable.BannerPagerIndicator_indicatorWidth, 8.dp.toFloat())
        indicatorHeight = typedArray.getDimension(R.styleable.BannerPagerIndicator_indicatorHeight,8.dp.toFloat())
        indicatorRadius = typedArray.getDimension(R.styleable.BannerPagerIndicator_indicatorRadius, 2.dp.toFloat())
        indicatorColor = typedArray.getColor(R.styleable.BannerPagerIndicator_indicatorNormalColor, Color.WHITE)
        indicatorSelectColor = typedArray.getColor(R.styleable.BannerPagerIndicator_indicatorSelectColor, Color.RED)
        indicatorSpacing = typedArray.getDimension(R.styleable.BannerPagerIndicator_indicatorSpacing, 4.dp.toFloat())
        typedArray.recycle()
    }
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        prepareIndicator()
    }

    private fun prepareIndicator() {
        rectFList.clear()
        if (count <= 1) return
        var startX = (width - (indicatorWidth * count + indicatorSpacing * (count - 1))) / 2
        val startY = (height - indicatorHeight) / 2
        for (i in 0 until count) {
            val right = startX + indicatorWidth
            val rectF = RectF(startX, startY, right, startY + indicatorHeight)
            rectFList.add(rectF)
            startX = right + indicatorSpacing
        }
    }

    override fun setCount(count: Int) {
        this.count = count
        prepareIndicator()
        invalidate()
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onPageSelected(position: Int) {
        currentItem = position
        invalidate()
    }

    override fun onPageScrollStateChanged(state: Int) {

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (rectFList.isEmpty()) return
        for (i in 0 until rectFList.size) {
            val rectF = rectFList[i]
            val drawPaint = if (currentItem == i) {
                selectPaint
            } else {
                normalPaint
            }
            canvas?.drawRoundRect(rectF, indicatorRadius, indicatorRadius, drawPaint)
        }
    }

}