package com.example.puzzle2048

import android.app.Activity
import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.min

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(246, 250, 235)
        window.navigationBarColor = Color.rgb(246, 250, 235)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContentView(GardenWordView(this))
    }
}

private data class WordLevel(val letters: List<String>, val words: List<String>)

private class GardenWordView(context: android.content.Context) : View(context) {
    private val levels = listOf(
        WordLevel(listOf("ب", "ا", "غ", "گ", "ل", "ر", "ا"), listOf("باغ", "گل", "برگ", "بار", "گلاب")),
        WordLevel(listOf("ب", "ه", "ا", "ر", "ن", "و", "گ"), listOf("بهار", "نور", "هوا", "راه", "نگار")),
        WordLevel(listOf("گ", "ل", "س", "ت", "ا", "ن", "ب"), listOf("گل", "سنگ", "گلستان", "لب", "بست")),
        WordLevel(listOf("د", "ر", "خ", "ت", "ا", "ن", "ب"), listOf("درخت", "تند", "بار", "نرد", "تب")),
        WordLevel(listOf("ش", "ک", "و", "ف", "ه", "ا", "ر"), listOf("شکوفه", "هوا", "راه", "کار", "شکر"))
    )
    private val bg = Color.rgb(246, 250, 235)
    private val green = Color.rgb(58, 112, 70)
    private val dark = Color.rgb(55, 76, 53)
    private val white = Color.WHITE
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var levelIndex = 0
    private var score = 0
    private var hints = 3
    private var selected = ""
    private val found = mutableSetOf<String>()
    private var letterCenters = mutableListOf<Pair<Float, Float>>()
    private var submitRect = RectF()
    private var clearRect = RectF()
    private var hintRect = RectF()
    private var nextRect = RectF()
    private var message = "حروف را انتخاب کن و کلمه بساز"
    private var messageUntil = 0L
    private val level: WordLevel get() = levels[levelIndex]

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(bg)
        val w = width.toFloat()
        val h = height.toFloat()
        drawGarden(canvas, w, h)
        text(canvas, "باغ واژه", w / 2f, h * .075f, min(w * .105f, 52f), green, true)
        text(canvas, "باغی پر از کلمه‌های قشنگ", w / 2f, h * .112f, 15f, dark, false)
        val margin = w * .055f
        val cardTop = h * .135f
        val cardH = h * .075f
        val cardGap = 10f
        val cardW = (w - 2 * margin - cardGap * 2) / 3f
        infoCard(canvas, RectF(margin, cardTop, margin + cardW, cardTop + cardH), "مرحله", (levelIndex + 1).toString())
        infoCard(canvas, RectF(margin + cardW + cardGap, cardTop, margin + 2 * cardW + cardGap, cardTop + cardH), "امتیاز", score.toString())
        infoCard(canvas, RectF(margin + 2 * (cardW + cardGap), cardTop, w - margin, cardTop + cardH), "راهنما", hints.toString())
        text(canvas, "کلمه‌های باغ را پیدا کن", w / 2f, h * .255f, 20f, green, true)

        val gridTop = h * .28f
        val slotH = min(43f, h * .057f)
        val slotGap = 9f
        val slotW = (w - 2 * margin - 12f) / 2f
        level.words.forEachIndexed { index, word ->
            val row = index / 2
            val col = index % 2
            val left = if (col == 0) margin else margin + slotW + 12f
            val top = gridTop + row * (slotH + slotGap)
            val rect = RectF(left, top, left + slotW, top + slotH)
            paint.color = if (word in found) Color.rgb(199, 226, 183) else white
            canvas.drawRoundRect(rect, 13f, 13f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            paint.color = if (word in found) green else Color.rgb(198, 218, 187)
            canvas.drawRoundRect(rect, 13f, 13f, paint)
            paint.style = Paint.Style.FILL
            text(canvas, if (word in found) "✓  $word" else "•••", rect.centerX(), rect.centerY() + 7f, 21f, if (word in found) green else Color.rgb(163, 188, 150), true)
        }

        val ringY = h * .70f
        val ringX = w / 2f
        val radius = min(w * .285f, h * .13f)
        paint.color = Color.rgb(224, 238, 205)
        canvas.drawCircle(ringX, ringY, radius + 14f, paint)
        paint.color = Color.rgb(193, 220, 171)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawCircle(ringX, ringY, radius + 4f, paint)
        paint.style = Paint.Style.FILL
        letterCenters = mutableListOf()
        level.letters.forEachIndexed { i, letter ->
            val angle = (-90.0 + i * 360.0 / level.letters.size) * Math.PI / 180.0
            val cx = ringX + cos(angle).toFloat() * radius
            val cy = ringY + sin(angle).toFloat() * radius
            letterCenters.add(cx to cy)
            paint.color = white
            canvas.drawCircle(cx, cy, 23f, paint)
            paint.color = if (selected.contains(letter)) Color.rgb(232, 177, 65) else green
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawCircle(cx, cy, 23f, paint)
            paint.style = Paint.Style.FILL
            text(canvas, letter, cx, cy + 8f, 25f, dark, true)
        }

        val selectedRect = RectF(margin, h * .565f, w - margin, h * .615f)
        paint.color = white
        canvas.drawRoundRect(selectedRect, 15f, 15f, paint)
        text(canvas, if (selected.isEmpty()) "حروف را لمس کن" else selected, w / 2f, selectedRect.centerY() + 8f, 25f, green, true)
        val btnY = h * .865f
        val btnH = min(50f, h * .07f)
        val btnGap = 9f
        val btnW = (w - 2 * margin - 2 * btnGap) / 3f
        submitRect = RectF(margin, btnY, margin + btnW, btnY + btnH)
        clearRect = RectF(margin + btnW + btnGap, btnY, margin + 2 * btnW + btnGap, btnY + btnH)
        hintRect = RectF(margin + 2 * (btnW + btnGap), btnY, w - margin, btnY + btnH)
        button(canvas, submitRect, "ثبت کلمه", green)
        button(canvas, clearRect, "پاک کردن", Color.rgb(127, 158, 109))
        button(canvas, hintRect, "راهنما", Color.rgb(211, 157, 57))
        if (found.containsAll(level.words)) {
            nextRect = RectF(w * .25f, h * .79f, w * .75f, h * .85f)
            button(canvas, nextRect, if (levelIndex == levels.lastIndex) "باغ تمام شد! دوباره" else "باغ بعدی  ←", Color.rgb(213, 139, 79))
        } else nextRect = RectF()
        if (System.currentTimeMillis() < messageUntil) {
            paint.color = Color.rgb(255, 248, 214)
            canvas.drawRoundRect(RectF(margin, h * .225f, w - margin, h * .265f), 12f, 12f, paint)
            text(canvas, message, w / 2f, h * .252f, 14f, dark, true)
            postInvalidateDelayed(300)
        }
    }

    private fun drawGarden(canvas: Canvas, w: Float, h: Float) {
        val points = listOf(18f to h * .18f, w - 18f to h * .23f, 22f to h * .83f, w - 20f to h * .81f)
        points.forEachIndexed { idx, p ->
            val colors = listOf(Color.rgb(239, 143, 172), Color.rgb(232, 177, 65), Color.rgb(171, 154, 219), Color.rgb(245, 190, 105))
            for (i in 0..5) {
                val a = i * Math.PI / 3.0
                paint.color = colors[idx]
                canvas.drawCircle(p.first + cos(a).toFloat() * 5f, p.second + sin(a).toFloat() * 5f, 4f, paint)
            }
            paint.color = Color.rgb(232, 177, 65)
            canvas.drawCircle(p.first, p.second, 3f, paint)
        }
        paint.color = Color.rgb(224, 239, 209)
        canvas.drawOval(RectF(-w * .15f, h * .92f, w * .55f, h * 1.04f), paint)
        canvas.drawOval(RectF(w * .5f, h * .94f, w * 1.15f, h * 1.05f), paint)
    }

    private fun infoCard(canvas: Canvas, rect: RectF, label: String, value: String) {
        paint.color = white
        canvas.drawRoundRect(rect, 13f, 13f, paint)
        text(canvas, label, rect.centerX(), rect.top + rect.height() * .36f, 13f, Color.rgb(107, 137, 93), false)
        text(canvas, value, rect.centerX(), rect.top + rect.height() * .78f, 20f, green, true)
    }

    private fun button(canvas: Canvas, rect: RectF, label: String, color: Int) {
        paint.color = color
        canvas.drawRoundRect(rect, 15f, 15f, paint)
        text(canvas, label, rect.centerX(), rect.centerY() + 6f, 15f, white, true)
    }

    private fun text(canvas: Canvas, value: String, x: Float, baseline: Float, size: Float, color: Int, bold: Boolean) {
        paint.color = color
        paint.textSize = size
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.create("sans-serif", if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        canvas.drawText(value, x, baseline, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) return true
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x
            val y = event.y
            when {
                submitRect.contains(x, y) -> submitWord()
                clearRect.contains(x, y) -> { selected = ""; message = "حروف پاک شد"; showMessage() }
                hintRect.contains(x, y) -> useHint()
                nextRect.contains(x, y) && nextRect.width() > 0f && found.containsAll(level.words) -> {
                    if (levelIndex == levels.lastIndex) { levelIndex = 0; score = 0; hints = 3 } else levelIndex++
                    found.clear()
                    selected = ""
                    message = "به باغ تازه خوش آمدی!"
                    showMessage()
                }
                else -> {
                    val hit = letterCenters.indexOfFirst { (cx, cy) -> (x - cx) * (x - cx) + (y - cy) * (y - cy) < 55f * 55f }
                    if (hit >= 0) { selected += level.letters[hit]; messageUntil = 0L }
                    else if (y < height * .56f) selected = ""
                }
            }
            invalidate()
            return true
        }
        return true
    }

    private fun submitWord() {
        val candidate = selected
        when {
            candidate.isEmpty() -> message = "اول چند حرف انتخاب کن"
            candidate in level.words && found.add(candidate) -> { score += candidate.length * 10; message = "آفرین! کلمه پیدا شد"; selected = "" }
            candidate in found -> { message = "این کلمه را قبلاً پیدا کردی"; selected = "" }
            else -> { message = "این کلمه در باغ نیست؛ دوباره تلاش کن"; selected = "" }
        }
        showMessage()
    }

    private fun useHint() {
        val missing = level.words.firstOrNull { it !in found }
        when {
            missing == null -> message = "همه کلمه‌ها پیدا شده‌اند!"
            hints <= 0 -> message = "راهنمای رایگان تمام شده"
            else -> { hints--; found.add(missing); score += missing.length * 5; message = "راهنما: «$missing» پیدا شد" }
        }
        showMessage()
    }

    private fun showMessage() { messageUntil = System.currentTimeMillis() + 2200 }
}
