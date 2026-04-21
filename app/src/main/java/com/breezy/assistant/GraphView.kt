package com.breezy.assistant

import android.content.Context
import android.graphics.*
import android.view.View

class GraphView(context: Context) : View(context) {

    private val dataPoints = mutableListOf<Float>()
    private val maxPoints = 20
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var lineColor = Color.BLUE
    private var fillColor = Color.BLUE and 0x33FFFFFF

    fun setColors(line: Int, fill: Int) {
        lineColor = line
        fillColor = fill
        invalidate()
    }

    fun addDataPoint(point: Float) {
        dataPoints.add(point)
        if (dataPoints.size > maxPoints) {
            dataPoints.removeAt(0)
        }
        invalidate()
    }

    private val path = Path()
    private val fillPath = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.size < 2) return

        val width = width.toFloat()
        val height = height.toFloat()
        val stepX = width / (maxPoints - 1)
        val maxValue = (dataPoints.maxOrNull() ?: 100f).coerceAtLeast(10f) * 1.2f

        path.reset()
        fillPath.reset()

        dataPoints.forEachIndexed { i, point ->
            val x = i * stepX
            val y = height - (point / maxValue * height)

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            
            if (i == dataPoints.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        fillPaint.color = fillColor
        canvas.drawPath(fillPath, fillPaint)

        paint.color = lineColor
        canvas.drawPath(path, paint)
    }
}
