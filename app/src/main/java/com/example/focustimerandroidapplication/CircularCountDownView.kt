package com.example.focustimerandroidapplication

import android.content.Context
import android.graphics.*
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class CircularCountdownView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val arcPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 16f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private val minutesInput = EditText(context).apply {
        setText("01")
        setTextColor(Color.WHITE)
        textSize = 60f
        inputType = InputType.TYPE_CLASS_NUMBER
        setBackgroundColor(Color.TRANSPARENT)
        gravity = Gravity.END
        setSelectAllOnFocus(true)
    }

    private val colon = TextView(context).apply {
        text = ":"
        setTextColor(Color.WHITE)
        textSize = 60f
        gravity = Gravity.CENTER
    }

    private val secondsInput = EditText(context).apply {
        setText("00")
        setTextColor(Color.WHITE)
        textSize = 60f
        inputType = InputType.TYPE_CLASS_NUMBER
        setBackgroundColor(Color.TRANSPARENT)
        gravity = Gravity.START
        setSelectAllOnFocus(true)
    }

    var onTimeEdited: ((Int, Int) -> Unit)? = null
    private var progress = 1f
    private var isEditable = true

    init {
        setWillNotDraw(false)

        val timeLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(minutesInput)
            addView(colon)
            addView(secondsInput)
        }

        addView(timeLayout, LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))

        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isEditable) notifyTimeChanged()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        minutesInput.addTextChangedListener(watcher)
        secondsInput.addTextChangedListener(watcher)
    }

    private fun notifyTimeChanged() {
        val min = minutesInput.text.toString().toIntOrNull() ?: 0
        val sec = secondsInput.text.toString().toIntOrNull() ?: 0
        onTimeEdited?.invoke(min, sec)
    }

    fun setProgress(p: Float) {
        progress = p.coerceIn(0f, 1f)
        invalidate()
    }

    fun setTime(min: Int, sec: Int) {
        isEditable = false
        minutesInput.setText(String.format("%02d", min))
        secondsInput.setText(String.format("%02d", sec))
        isEditable = true
    }

    fun setTimeText(text: String) {
        val parts = text.split(":")
        if (parts.size == 2) {
            setTime(parts[0].toIntOrNull() ?: 0, parts[1].toIntOrNull() ?: 0)
        }
    }

    fun setEditable(editable: Boolean) {
        isEditable = editable
        minutesInput.isEnabled = editable
        secondsInput.isEnabled = editable
        if (!editable) {
            minutesInput.clearFocus()
            secondsInput.clearFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = width / 2f - 30f
        val cx = width / 2f
        val cy = height / 2f
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val sweep = -progress * 360f
        canvas.drawArc(rect, -90f, sweep, false, arcPaint)
    }
}
