package otus.homework.customview

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import otus.homework.customview.pie_chart.PyChartView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pieChartView = findViewById<PyChartView>(R.id.pieChartView)
        pieChartView.animateProgress(0, 360)

        pieChartView.setOnPieChartClickListener { pieItems ->
            val categoryName = pieItems.firstOrNull()?.category.orEmpty()
            Toast.makeText(this, categoryName, Toast.LENGTH_SHORT).show()
        }

        val pieChartButtonRestartAnimation: Button = findViewById(R.id.restart_pie_chart_animation)
        pieChartButtonRestartAnimation.setOnClickListener {
            pieChartView.animateProgress(0, 360)
        }
    }
}