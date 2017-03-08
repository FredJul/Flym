/*
 * Copyright (c) 2012-2017 Frederic Julian
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package net.frju.flym.ui.views

import android.content.Context
import android.text.Layout.Alignment
import android.text.StaticLayout
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.TextView
import net.fred.feedex.R

class AutoResizer(private val textView: TextView) {

    var maxTextSize: Float
        get() = mMaxTextSize
        set(maxTextSize) {
            mMaxTextSize = maxTextSize
            resizeText()
        }

    var minTextSize: Float
        get() = mMinTextSize
        set(minTextSize) {
            mMinTextSize = minTextSize
            resizeText()
        }

    private var mMinTextSize: Float = 0.toFloat()

    private var mMaxTextSize: Float = 0.toFloat()

    init {

        val metrics = textView.context.resources.displayMetrics
        mMinTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MIN_TEXT_SIZE_IN_DP.toFloat(), metrics)
        mMaxTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_TEXT_SIZE_IN_DP.toFloat(), metrics)
    }

    fun initAttrs(context: Context, attrs: AttributeSet) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.AutoResizeTextView)
        mMinTextSize = a.getDimension(R.styleable.AutoResizeTextView_minTextSize, mMinTextSize)
        mMaxTextSize = a.getDimension(R.styleable.AutoResizeTextView_maxTextSize, mMaxTextSize)
        a.recycle()
    }

    /**
     * Resize the text size with default width and height
     */
    fun resizeText() {
        val heightLimit = textView.height - textView.paddingBottom - textView.paddingTop
        val widthLimit = textView.width - textView.paddingLeft - textView.paddingRight

        val newText = textView.text

        // Do not resize if the view does not have dimensions or there is no text
        if (newText == null || newText.isEmpty() || heightLimit <= 0 || widthLimit <= 0) {
            return
        }

        // Get the text view's paint object
        val textPaint = textView.paint
        val originalPaintTextSize = textPaint.textSize

        // Bisection method: fast & precise
        var lower = mMinTextSize
        var upper = mMaxTextSize
        var loopCounter = 1
        var targetTextSize: Float
        var textHeight: Int

        while (loopCounter < BISECTION_LOOP_WATCH_DOG && upper - lower > 1) {
            targetTextSize = (lower + upper) / 2

            // Update the text paint object
            textPaint.textSize = targetTextSize
            // Measure using a static layout
            val layout = StaticLayout(newText, textPaint, widthLimit, Alignment.ALIGN_NORMAL, textView.lineSpacingMultiplier, textView.lineSpacingExtra, true)
            textHeight = layout.height

            if (textHeight > heightLimit) {
                upper = targetTextSize
            } else {
                lower = targetTextSize
            }
            loopCounter++
        }

        textPaint.textSize = originalPaintTextSize // need to restore the initial one to avoid graphical issues

        targetTextSize = lower

        // Some devices try to auto adjust line spacing, so force default line spacing
        // and invalidate the layout as a side effect
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, targetTextSize)
    }

    companion object {

        private val MIN_TEXT_SIZE_IN_DP = 9
        private val MAX_TEXT_SIZE_IN_DP = 50
        private val BISECTION_LOOP_WATCH_DOG = 30
    }
}
