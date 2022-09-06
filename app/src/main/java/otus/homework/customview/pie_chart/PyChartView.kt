package otus.homework.customview.pie_chart

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import otus.homework.customview.R
import otus.homework.customview.models.PayloadModel
import otus.homework.customview.models.PieChartItemData
import otus.homework.customview.utils.PaintManager
import otus.homework.customview.utils.ParsePayloadUtil
import otus.homework.customview.utils.RandomColorFromResources
import otus.homework.customview.utils.translateBy
import kotlin.math.*

class PyChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var textColor: Int = 0
    var itemFont: Int = 0
    var itemTextSize: Float = 0f
    var strokeWidth = 0f
    var animationTime = 1000L
    var pieChartSize = 0
    private val viewMinSize = resources.getDimensionPixelSize(R.dimen.pie_chart_view_min_size)

    private var pieChartItems = listOf<PieChartItemData>()
    private var onPieChartItemClick: ((List<PayloadModel>) -> Unit)? = null
    private val colorsFromResources = RandomColorFromResources(context, R.array.view_colors)

    private lateinit var mainPaint: Paint
    private lateinit var textPaint: Paint

    private var radius: Float = 0f
    private var widthFloat: Float = 0f
    private var heightFloat: Float = 0f

    private var currentScale = 360
    private var currentAngle = 0f
    private var textInterDistance = 0

    private val oval = RectF()

    init {
        val sections = ParsePayloadUtil.getPayloadFromRaw(context)?.let {
            ParsePayloadUtil.parsePayloadJson(it)
        }.orEmpty()

        val data = sections.groupBy { it.category }
        val sumAmount = sections.sumOf { it.amount }.toFloat()
        calculatePieChartSections(data, sumAmount)
        getAttributes(attrs)
    }

    private fun getAttributes(attributes: AttributeSet?) {
        attributes?.let { initAttributes(it) }
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        initPaints()
    }

    private fun initAttributes(attrs: AttributeSet) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.PieChart,
            0, 0
        ).apply {
            try {
                textColor = getColor(R.styleable.PieChart_itemTextColor, Color.WHITE)
                itemTextSize = getDimension(R.styleable.PieChart_itemTextSize, 14f)
                itemFont = getResourceId(R.styleable.PieChart_textFontFamily, 0)
                strokeWidth = getDimension(R.styleable.PieChart_pieStrokeWidth, 100f)
            } finally {
                recycle()
            }
        }
    }

    private fun initPaints() {
        with(PaintManager) {
            mainPaint = initMainPaint(strokeWidth)
            textPaint = initTextPaint(context, textColor, itemTextSize, itemFont)
        }
    }

    private fun calculatePieChartSections(
        data: Map<String, List<PayloadModel>>,
        sumAmount: Float
    ) {
        val sections = mutableListOf<PieChartItemData>()
        var currentAngle = INITIAL_ANGLE
        data.values.forEach { categoryItems ->
            val sweepAngle =
                categoryItems.sumOf { it.amount } / sumAmount * ARC_FULL_ROTATION_DEGREE
            val pieChartItem = PieChartItemData(
                startAngle = currentAngle,
                sweepAngle = sweepAngle,
                color = colorsFromResources.color,
                text = categoryItems.firstOrNull()?.category.orEmpty(),
                pieChartItems = categoryItems
            )
            sections.add(pieChartItem)
            currentAngle += sweepAngle
        }
        this.pieChartItems = sections
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // полагаем, что width = height
        pieChartSize = onViewMeassure(widthMeasureSpec)
        calculateValues()

        setMeasuredDimension(pieChartSize, pieChartSize)
    }

    private fun onViewMeassure(measureSpecSize: Int): Int {
        val viewMode = MeasureSpec.getMode(measureSpecSize)
        val viewSize = MeasureSpec.getSize(measureSpecSize)
        return when (viewMode) {
            MeasureSpec.UNSPECIFIED -> viewMinSize
            MeasureSpec.EXACTLY -> viewSize
            MeasureSpec.AT_MOST -> viewMinSize
            else -> throw IllegalStateException("onViewMeassure() wrong view mode")
        }
    }

    private fun calculateValues() {
        widthFloat = measuredWidth.toFloat()
        heightFloat = measuredHeight.toFloat()
        radius = calculateRadius(measuredWidth.toFloat(), measuredHeight.toFloat())
        strokeWidth = strokeWidth.coerceAtMost(radius)
        textInterDistance = (radius - strokeWidth / 2).toInt()
        val ovalRadius = radius - strokeWidth / 2
        oval.set(
            -ovalRadius,
            -ovalRadius,
            ovalRadius,
            ovalRadius
        )
    }

    private fun calculateRadius(width: Float, height: Float): Float {
        return if (width > height) {
            height / 2
        } else {
            width / 2
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // translate into center of view
        canvas.translate(widthFloat / 2, heightFloat / 2)
        drawPieItems(canvas)
        drawTextItems(canvas)
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()

        val pieChartState = PieChartSavedState(superState)
        pieChartState.pieChartItems = this.pieChartItems

        return pieChartState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null || state !is PieChartSavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        this.pieChartItems = state.pieChartItems
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return generalGestureDetector.onTouchEvent(event)
    }

    private val generalGestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent?): Boolean {
                return true
            }

            override fun onSingleTapUp(event: MotionEvent): Boolean {
                val touchX = event.x
                val touchY = event.y

                getTouchPieChartAngle(touchX, touchY)?.let { angle ->
                    pieChartItems.forEach { section ->
                        if (angle > section.startAngle
                            && angle <= section.sweepAngle + section.startAngle
                        ) {
                            onPieChartItemClick?.invoke(section.pieChartItems)
                            return true
                        }
                    }
                }
                return false
            }
        })

    private fun getTouchPieChartAngle(touchX: Float, touchY: Float): Float? {
        val chartCenter = pieChartSize / 2
        val innerRadius = chartCenter - mainPaint.strokeWidth

        val ac: Double = abs(chartCenter.toDouble() - touchX.toDouble())
        val cb: Double = abs(chartCenter.toDouble() - touchY.toDouble())

        val distanceBetweenCenterAndTouchPoint = hypot(ac, cb)

        if (distanceBetweenCenterAndTouchPoint < innerRadius || distanceBetweenCenterAndTouchPoint > chartCenter) {
            return null
        }

        val zeroVector = PointF(chartCenter.toFloat(), 0F)
        val touchVector = PointF(touchX - chartCenter, touchY - chartCenter)

        val vectorMultiply = zeroVector.x * touchVector.x + zeroVector.y * touchVector.y
        val zeroVectorModule = sqrt(zeroVector.x.pow(2) + zeroVector.y.pow(2))
        val touchVectorModule = sqrt(touchVector.x.pow(2) + touchVector.y.pow(2))

        val angleRadians = acos(vectorMultiply / (zeroVectorModule * touchVectorModule))
        val angleBetweenVectors = Math.toDegrees(angleRadians.toDouble()).toFloat()

        return if (touchY < chartCenter) {
            ARC_FULL_ROTATION_DEGREE - angleBetweenVectors
        } else {
            angleBetweenVectors
        }
    }

    fun setOnPieChartClickListener(callback: ((List<PayloadModel>) -> Unit)?) {
        onPieChartItemClick = callback
    }


    private fun drawPieItems(canvas: Canvas) {
        for (item in pieChartItems) {
            canvas.drawArc(
                oval,
                currentAngle,
                getItemValue(item),
                false,
                mainPaint.apply {
                    color = item.color
                })
            currentAngle += item.sweepAngle
        }
    }

    private fun drawTextItems(canvas: Canvas) {
        for (item in pieChartItems) {
            val angle = getAngleBetween(item)
            canvas.translateBy(angle, textInterDistance)
            val list = item.text.split(" ")
            list.forEachIndexed { index, string ->
                canvas.drawText(string, 0f, itemTextSize * (index + 0.5f), textPaint)
            }
            currentAngle += item.sweepAngle
            canvas.translateBy(angle, -textInterDistance)
        }
    }

    fun animateProgress(from: Int, to: Int) {
        val valuesHolder = PropertyValuesHolder.ofInt(PERCENTAGE_VALUE_HOLDER, from, to)
        val animator = ValueAnimator().apply {
            interpolator = LinearInterpolator()
            setValues(valuesHolder)
            duration = animationTime
            addUpdateListener {
                val percentage = it.getAnimatedValue(PERCENTAGE_VALUE_HOLDER) as Int
                currentScale = percentage
                invalidate()
            }
        }
        animator.start()
    }

    private class PieChartSavedState : BaseSavedState {
        var pieChartItems: List<PieChartItemData> = listOf()

        constructor(superState: Parcelable?) : super(superState)

        private constructor(source: Parcel) : super(source) {
            source.readList(pieChartItems, List::class.java.classLoader)
        }

        override fun writeToParcel(out: Parcel?, flags: Int) {
            super.writeToParcel(out, flags)
            out?.writeList(pieChartItems)
        }

        companion object CREATOR : Parcelable.Creator<PieChartSavedState> {
            override fun createFromParcel(parcel: Parcel): PieChartSavedState {
                return PieChartSavedState(parcel)
            }

            override fun newArray(size: Int): Array<PieChartSavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    private fun getCurrentPercent() = currentScale.toFloat() / ARC_FULL_ROTATION_DEGREE

    private fun getAngleBetween(item: PieChartItemData) =
        currentAngle + item.sweepAngle / 2 * getCurrentPercent()

    private fun getItemValue(item: PieChartItemData) = item.sweepAngle * getCurrentPercent()

    companion object {
        const val ARC_FULL_ROTATION_DEGREE = 360F
        const val INITIAL_ANGLE = 0F
        const val PERCENTAGE_VALUE_HOLDER = "percentage"
    }
}