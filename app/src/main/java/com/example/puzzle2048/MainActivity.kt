package com.example.puzzle2048

import android.app.Activity
import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(250, 248, 239)
        window.navigationBarColor = Color.rgb(250, 248, 239)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContentView(PuzzleView())
    }
}

private class PuzzleView : View {
    private val background = Color.rgb(250, 248, 239)
    private val boardColor = Color.rgb(187, 173, 160)
    private val emptyColor = Color.rgb(205, 193, 180)
    private val ink = Color.rgb(70, 62, 53)
    private val white = Color.WHITE
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val board = Array(4) { IntArray(4) }
    private var score = 0
    private var best = 0
    private var gameOver = false
    private var downX = 0f
    private var downY = 0f
    private var boardRect = RectF()
    private var restartRect = RectF()

    init {
        isFocusable = true
        addRandomTile()
        addRandomTile()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(background)

        val w = width.toFloat()
        val h = height.toFloat()
        drawText(canvas, "۲۰۴۸", w / 2f, h * 0.105f, min(w * 0.15f, 68f), ink, true)
        drawText(canvas, "بازی معمایی اعداد", w / 2f, h * 0.155f, 20f, ink, true)

        val sideMargin = 22f
        val gap = 12f
        val cardTop = h * 0.19f
        val cardHeight = 62f
        val cardWidth = (w - sideMargin * 2f - gap) / 2f
        drawCard(canvas, RectF(sideMargin, cardTop, sideMargin + cardWidth, cardTop + cardHeight), "امتیاز", score.toString())
        drawCard(canvas, RectF(sideMargin + cardWidth + gap, cardTop, w - sideMargin, cardTop + cardHeight), "بهترین", best.toString())

        val buttonWidth = 150f
        restartRect = RectF(w - sideMargin - buttonWidth, h * 0.285f, w - sideMargin, h * 0.285f + 44f)
        paint.color = Color.rgb(143, 122, 102)
        canvas.drawRoundRect(restartRect, 8f, 8f, paint)
        drawText(canvas, "شروع دوباره", restartRect.centerX(), restartRect.centerY() + 6f, 16f, white, true)
        drawText(canvas, "خانه‌های هم‌عدد را با کشیدن انگشت ادغام کن", w / 2f, h * 0.345f, 14f, ink, false)

        val boardSize = min(w - 2f * sideMargin, h * 0.52f)
        val left = (w - boardSize) / 2f
        val top = h * 0.385f
        boardRect = RectF(left, top, left + boardSize, top + boardSize)
        paint.color = boardColor
        canvas.drawRoundRect(boardRect, 10f, 10f, paint)

        val cellGap = boardSize * 0.025f
        val cellSize = (boardSize - cellGap * 5f) / 4f
        for (r in 0..3) {
            for (c in 0..3) {
                val x = left + cellGap + c * (cellSize + cellGap)
                val y = top + cellGap + r * (cellSize + cellGap)
                val value = board[r][c]
                paint.color = tileColor(value)
                canvas.drawRoundRect(RectF(x, y, x + cellSize, y + cellSize), 7f, 7f, paint)
                if (value != 0) {
                    val textColor = if (value <= 4) ink else Color.WHITE
                    val textSize = if (value >= 1024) cellSize * 0.27f else cellSize * 0.34f
                    drawText(canvas, value.toString(), x + cellSize / 2f, y + cellSize / 2f + textSize * 0.34f, textSize, textColor, true)
                }
            }
        }

        if (gameOver) {
            paint.color = Color.argb(185, 250, 248, 239)
            canvas.drawRoundRect(boardRect, 10f, 10f, paint)
            drawText(canvas, "پایان بازی!", boardRect.centerX(), boardRect.centerY() - 10f, 30f, ink, true)
            drawText(canvas, "برای تلاش دوباره دکمه را بزن", boardRect.centerX(), boardRect.centerY() + 28f, 15f, ink, false)
        }
    }

    private fun drawCard(canvas: Canvas, rect: RectF, label: String, value: String) {
        paint.color = Color.rgb(187, 173, 160)
        canvas.drawRoundRect(rect, 8f, 8f, paint)
        drawText(canvas, label, rect.centerX(), rect.top + 21f, 13f, white, true)
        drawText(canvas, value, rect.centerX(), rect.top + 48f, 21f, white, true)
    }

    private fun drawText(canvas: Canvas, text: String, x: Float, baseline: Float, size: Float, color: Int, bold: Boolean) {
        paint.color = color
        paint.textSize = size
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = if (bold) android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
            else android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        canvas.drawText(text, x, baseline, paint)
    }

    private fun tileColor(value: Int): Int = when (value) {
        0 -> emptyColor
        2 -> Color.rgb(238, 228, 218)
        4 -> Color.rgb(237, 224, 200)
        8 -> Color.rgb(242, 177, 121)
        16 -> Color.rgb(245, 149, 99)
        32 -> Color.rgb(246, 124, 95)
        64 -> Color.rgb(246, 94, 59)
        128 -> Color.rgb(237, 207, 114)
        256 -> Color.rgb(237, 204, 97)
        512 -> Color.rgb(237, 200, 80)
        1024 -> Color.rgb(237, 197, 63)
        else -> Color.rgb(237, 194, 46)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (restartRect.contains(event.x, event.y)) {
                    resetGame()
                } else if (abs(dx) > 28f || abs(dy) > 28f) {
                    if (!gameOver) {
                        if (abs(dx) > abs(dy)) move(if (dx < 0) -1 else 1, 0)
                        else move(0, if (dy < 0) -1 else 1)
                    }
                }
                return true
            }
        }
        return true
    }

    private fun resetGame() {
        for (r in 0..3) for (c in 0..3) board[r][c] = 0
        score = 0
        gameOver = false
        addRandomTile()
        addRandomTile()
        invalidate()
    }

    private fun addRandomTile() {
        val empty = mutableListOf<Pair<Int, Int>>()
        for (r in 0..3) for (c in 0..3) if (board[r][c] == 0) empty.add(r to c)
        if (empty.isEmpty()) return
        val (r, c) = empty[Random.nextInt(empty.size)]
        board[r][c] = if (Random.nextInt(10) == 0) 4 else 2
    }

    private fun move(dx: Int, dy: Int) {
        var changed = false
        for (lineIndex in 0..3) {
            val coords = (0..3).map { step ->
                when {
                    dx < 0 -> lineIndex to step
                    dx > 0 -> lineIndex to (3 - step)
                    dy < 0 -> step to lineIndex
                    else -> (3 - step) to lineIndex
                }
            }
            val original = coords.map { (r, c) -> board[r][c] }.toIntArray()
            val compact = original.filter { it != 0 }.toMutableList()
            val merged = mutableListOf<Int>()
            var i = 0
            while (i < compact.size) {
                if (i + 1 < compact.size && compact[i] == compact[i + 1]) {
                    val value = compact[i] * 2
                    merged.add(value)
                    score += value
                    best = maxOf(best, score)
                    i += 2
                } else {
                    merged.add(compact[i])
                    i++
                }
            }
            while (merged.size < 4) merged.add(0)
            for (index in coords.indices) {
                val (r, c) = coords[index]
                if (board[r][c] != merged[index]) changed = true
                board[r][c] = merged[index]
            }
        }
        if (changed) addRandomTile()
        gameOver = !hasMoves()
        invalidate()
    }

    private fun hasMoves(): Boolean {
        for (r in 0..3) for (c in 0..3) {
            if (board[r][c] == 0) return true
            if (r < 3 && board[r][c] == board[r + 1][c]) return true
            if (c < 3 && board[r][c] == board[r][c + 1]) return true
        }
        return false
    }
}
