package com.goldalert.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import org.json.JSONArray

class GoldChartView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var candles = JSONArray()

    fun setCandles(data: JSONArray) {
        candles = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.rgb(18, 22, 30))

        if (candles.length() == 0) {
            paint.color = Color.WHITE
            paint.textSize = 36f
            canvas.drawText("XAUUSD - Waiting for candles", 20f, 65f, paint)
            return
        }

        val count = candles.length()
        var minPrice = Double.MAX_VALUE
        var maxPrice = -Double.MAX_VALUE

        for (i in 0 until count) {
            val c = candles.getJSONObject(i)
            minPrice = minOf(minPrice, c.getDouble("low"))
            maxPrice = maxOf(maxPrice, c.getDouble("high"))
        }

        val range = (maxPrice - minPrice).coerceAtLeast(0.000001)
        val top = 50f
        val bottom = height - 40f
        val step = width.toFloat() / count

        fun y(price: Double): Float =
            bottom - ((price - minPrice) / range * (bottom - top)).toFloat()

        for (i in 0 until count) {
            val c = candles.getJSONObject(i)
            val open = c.getDouble("open")
            val close = c.getDouble("close")
            val high = c.getDouble("high")
            val low = c.getDouble("low")

            paint.color = if (close >= open) Color.GREEN else Color.RED
            paint.strokeWidth = 2f

            val x = (i + 0.5f) * step
            canvas.drawLine(x, y(high), x, y(low), paint)

            paint.style = Paint.Style.FILL
            canvas.drawRect(
                x - step * 0.3f,
                minOf(y(open), y(close)),
                x + step * 0.3f,
                maxOf(y(open), y(close)) + 1f,
                paint
            )
        }
    }
}
