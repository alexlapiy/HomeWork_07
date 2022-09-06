package otus.homework.customview.utils

import android.content.Context
import android.graphics.Canvas
import kotlin.math.cos
import kotlin.math.sin
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import otus.homework.customview.R
import otus.homework.customview.models.PayloadModel

object ParsePayloadUtil {
    private val gson = Gson()

    fun getPayloadFromRaw(context: Context): String? {
        return try {
            val inputStream = context.resources.openRawResource(R.raw.payload)
            inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }

    fun parsePayloadJson(json: String): List<PayloadModel> {
        return try {
            gson.fromJson(
                json,
                object : TypeToken<List<PayloadModel>>() {}.type
            )
        } catch (e: Exception) {
            emptyList()
        }
    }
}

fun Canvas.translateBy(angle: Float, distance: Int) {
    val pureDegree = angle % 360
    val degree = Math.toRadians(pureDegree.toDouble())
    val xShift = (cos(degree) * distance).toFloat()
    val yShift = (sin(degree) * distance).toFloat()

    this.translate(xShift, yShift)
}