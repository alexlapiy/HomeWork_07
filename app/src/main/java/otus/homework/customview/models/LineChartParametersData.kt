package otus.homework.customview.models

import java.util.*

data class LineChartParametersData(
    val minTime: Calendar,
    val maxTime: Calendar,
    val minAmount: Double,
    val maxAmount: Double
)