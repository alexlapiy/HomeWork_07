package otus.homework.customview.models

data class PieChartItemData(
    val startAngle: Float,
    val sweepAngle: Float,
    val color: Int,
    val text: String,
    val pieChartItems: List<PayloadModel>
)