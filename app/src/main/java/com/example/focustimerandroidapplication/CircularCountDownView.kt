package com.example.focustimerandroidapplication

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CircularCountdownView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val arcPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 12f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 72f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private var progress: Float = 1f
    private var timeText: String = "01:00"

    fun setProgress(p: Float) {
        progress = p
        invalidate()
    }

    fun setTimeText(t: String) {
        timeText = t
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = width / 2f - 20f
        val cx = width / 2f
        val cy = height / 2f - 20f
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(rect, -90f, -progress * 360, false, arcPaint)
        canvas.drawText(timeText, cx, cy + textPaint.textSize / 3, textPaint)
    }
}
