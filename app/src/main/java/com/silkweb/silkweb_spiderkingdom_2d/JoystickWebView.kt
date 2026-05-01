package com.silkweb.silkweb_spiderkingdom_2d

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class JoystickWebView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00FFCC")
        strokeWidth = 2f
        style = Paint.Style.STROKE
        alpha = 100 // Slightly transparent like the background
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = (width.coerceAtMost(height) / 2f) * 0.9f
        
        val sides = 8
        val rings = 8

        // Draw radial lines
        for (i in 0 until sides) {
            val angle = (i * 2 * PI) / sides
            canvas.drawLine(cx, cy, (cx + maxRadius * cos(angle)).toFloat(), (cy + maxRadius * sin(angle)).toFloat(), paint)
        }

        // Draw rings
        for (r in 1..rings) {
            val currentR = maxRadius * (r.toFloat() / rings)
            val path = Path()
            for (i in 0..sides) {
                val angle = (i * 2 * PI) / sides
                val vx = cx + currentR * cos(angle).toFloat()
                val vy = cy + currentR * sin(angle).toFloat()
                if (i == 0) path.moveTo(vx, vy) else path.lineTo(vx, vy)
            }
            path.close()
            canvas.drawPath(path, paint)
        }
    }
}
