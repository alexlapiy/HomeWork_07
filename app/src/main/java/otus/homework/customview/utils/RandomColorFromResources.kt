package otus.homework.customview.utils

import android.content.Context
import androidx.annotation.ArrayRes
import java.util.ArrayDeque


class RandomColorFromResources(
    context: Context,
    @ArrayRes val colorArray: Int
) {
    private val resColors = context.resources.getIntArray(colorArray).toCollection(ArrayDeque())
    private var colors = ArrayDeque(resColors.shuffled())

    val color: Int
        get() {
            if (colors.size == 0) {
                colors = ArrayDeque(resColors.shuffled())
            }
            return colors.pop()
        }
}