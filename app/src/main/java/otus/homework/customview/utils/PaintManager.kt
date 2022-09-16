package otus.homework.customview.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.res.ResourcesCompat
import kotlin.math.roundToInt

object PaintManager {
    fun initTextPaint(context: Context, textColor: Int, textSize: Float, font: Int): Paint {
        val textPaint = Paint()
        textPaint.apply {
            color = textColor
            isFakeBoldText
            isAntiAlias = true
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            this.textSize = textSize
            if (font > 0)
                try {
                    typeface = ResourcesCompat.getFont(context, font)
                } catch (ex: Exception) {

                }
        }
        return textPaint
    }

    fun initMainPaint(strokeWidth: Float): Paint {
        val mainPaint = Paint()
        mainPaint.apply {
            isAntiAlias = true
            color = Color.BLACK
            isDither = true
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.BUTT
            strokeJoin = Paint.Join.ROUND
        }
        return mainPaint
    }
}