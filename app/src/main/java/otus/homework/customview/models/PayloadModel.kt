package otus.homework.customview.models

data class PayloadModel(
    val id: Int,
    val name: String,
    val amount: Int,
    val category: String,
    val time: Long
)
