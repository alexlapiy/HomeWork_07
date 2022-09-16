package otus.homework.customview.line_chart

import android.content.Context
import android.graphics.*
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import otus.homework.customview.R
import otus.homework.customview.models.LineChartData
import otus.homework.customview.models.LineChartParametersData
import otus.homework.customview.models.PayloadModel
import otus.homework.customview.utils.ParsePayloadUtil
import otus.homework.customview.utils.RandomColorFromResources
import java.util.*

class LineChartView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    private var sectionsList: MutableList<LineChartData?> = mutableListOf()
    private var maxGraphicAmount: Double = 10000.0
    private var maxGraphicDay: Int = 31
    private val colorsFromResources = RandomColorFromResources(context, R.array.view_colors)

    private val graphLinesPaint = Paint().apply {
        color = Color.GRAY
    }
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = resources.getDimension(R.dimen.line_chart_text_min_size)
    }
    private val pathPaint = Paint().apply {
        strokeWidth = 6f
        style = Paint.Style.STROKE
        pathEffect = CornerPathEffect(40f)
    }

    private var amountYStep = 0.0f
    private var amountStep = 0.0
    private var dayStep = 0.0f

    init {
        val sectionData = getSectionData()
        val lineChartParameters = getLineChartParameters(sectionData)
        val mappedData = getLineChartData(sectionData)

        setData(mappedData, lineChartParameters)
    }

    private fun getLineChartData(sectionsData: List<PayloadModel>): List<LineChartData> {
        val data = sectionsData.groupBy { it.category }

        return data.values.map {
            LineChartData(
                color = colorsFromResources.color,
                section = it
            )
        }
    }

    private fun getLineChartParameters(sectionsData: List<PayloadModel>): LineChartParametersData {
        val minTime = sectionsData.minOf { it.time }
        val maxTime = sectionsData.maxOf { it.time }

        val minDate = Calendar.getInstance().apply {
            timeInMillis = minTime
        }
        val maxDate = Calendar.getInstance().apply {
            timeInMillis = maxTime
        }

        val minAmount = sectionsData.minOf { it.amount }
        val maxAmount = sectionsData.maxOf { it.amount }

        return LineChartParametersData(
            minTime = minDate,
            maxTime = maxDate,
            minAmount = minAmount.toDouble(),
            maxAmount = maxAmount.toDouble()
        )
    }

    private fun getSectionData(): List<PayloadModel> {
        return ParsePayloadUtil.getPayloadFromRaw(context)?.let {
            ParsePayloadUtil.parsePayloadJson(it)
        }.orEmpty()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val size = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.UNSPECIFIED,
            MeasureSpec.AT_MOST -> resources.getDimensionPixelSize(R.dimen.line_graph_min_size)
            else -> resources.getDimensionPixelSize(R.dimen.line_graph_min_size)
        }
        setMeasuredDimension(size, size / 2)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        amountYStep = (height - (2 * PADDING)) / HORIZONTAL_LINE_COUNT
        dayStep = (width - (2 * PADDING)) / maxGraphicDay
        amountStep = maxGraphicAmount / HORIZONTAL_LINE_COUNT

        drawGraphLines(canvas)
        drawGraphAxes(canvas)
        drawSectionsLines(canvas)
    }

    override fun onSaveInstanceState(): Parcelable {
        val state = super.onSaveInstanceState()
        return GraphSavedState(sectionsList, state)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val graphSavedState = state as GraphSavedState
        super.onRestoreInstanceState(graphSavedState.superState)
        sectionsList.clear()
        graphSavedState.sectionsData.let { sectionsList.addAll(it) }
    }

    private fun drawGraphLines(canvas: Canvas) {
        canvas.drawLine(PADDING, PADDING, width - PADDING, PADDING, graphLinesPaint)
        for (i in 1..HORIZONTAL_LINE_COUNT) {
            val y = PADDING + (i * amountYStep)
            canvas.drawLine(PADDING, y, width - PADDING, y, graphLinesPaint)
        }

        canvas.drawLine(PADDING, PADDING, PADDING, height - PADDING, graphLinesPaint)
        for (i in 1..maxGraphicDay) {
            if (i % 2 != 0) {
                val x = PADDING + ((i - 1) * dayStep)
                canvas.drawLine(x, PADDING, x, height - PADDING, graphLinesPaint)
            }
        }

    }

    private fun drawGraphAxes(canvas: Canvas) {
        var y = PADDING + (amountYStep * HORIZONTAL_LINE_COUNT) + (PADDING / 2 + 10)
        for (i in 1..maxGraphicDay) {
            if (i % 2 != 0) {
                val x = PADDING + ((i - 1) * dayStep) - 10
                canvas.drawText("$i", x, y, textPaint)
            }
        }

        val x = (maxGraphicDay - 1) * dayStep - PADDING
        for (i in 1..HORIZONTAL_LINE_COUNT) {
            val amount = (maxGraphicAmount - (amountStep * (i - 1))).toInt()
            y = PADDING + (i * amountYStep) - (amountYStep - PADDING / 1.5f)
            canvas.drawText("$amount Р", x, y, textPaint)
        }
    }

    private fun drawSectionsLines(canvas: Canvas) {
        sectionsList.forEach { data ->
            val path = Path()
            path.moveTo(PADDING, height - PADDING)
            for (item in data?.section.orEmpty()) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = item.time
                val pointX = calendar.let { time -> PADDING + (time.get(Calendar.DAY_OF_MONTH) - 1) * dayStep }
                val pointY = (height - PADDING) - (item.amount * amountYStep / amountStep)
                pointX.let {
                    path.lineTo(pointX, pointY.toFloat())
                }
            }
            val endPointX = PADDING + (maxGraphicDay - 1) * dayStep
            path.lineTo(endPointX,height - PADDING)

            pathPaint.color = data?.color ?: 0
            canvas.drawPath(path, pathPaint)
        }
    }

    private fun setData(data: List<LineChartData>, lineChartParameters: LineChartParametersData) {
        this.sectionsList = data.toMutableList()

        maxGraphicAmount = lineChartParameters.maxAmount
        maxGraphicDay = lineChartParameters.minTime.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private class GraphSavedState : BaseSavedState {

        var sectionsData: List<LineChartData?> = emptyList()

        private constructor(source: Parcel) : super(source) {
            sectionsData.run { source.readList(this, LineChartData::class.java.classLoader) }
        }

        constructor(sectionsData: List<LineChartData?>, superState: Parcelable?) : super(superState) {
            this.sectionsData = sectionsData
        }

        override fun writeToParcel(out: Parcel?, flags: Int) {
            super.writeToParcel(out, flags)
            out?.writeList(sectionsData)
        }

        companion object CREATOR : Parcelable.Creator<GraphSavedState> {
            override fun createFromParcel(source: Parcel): GraphSavedState {
                return GraphSavedState(source)
            }

            override fun newArray(size: Int): Array<GraphSavedState> {
                return newArray(size)
            }
        }
    }

    companion object {
        private const val HORIZONTAL_LINE_COUNT = 4
        private const val PADDING = 50f
    }
}