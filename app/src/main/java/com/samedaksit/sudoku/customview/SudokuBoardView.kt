package com.samedaksit.sudoku.customview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.samedaksit.sudoku.R
import com.samedaksit.sudoku.model.Cell
import kotlin.math.min

class SudokuBoardView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    private var listener: OnTouchListener? = null

    private val size = 9
    private val squareSize = 3

    private val thickLineStrokeWidth = 6F
    private val thinLineStrokeWidth = 2F

    private var cellSizePixels = 0F
    private var noteSizePixels = 0F

    private var selectedCellRow = 0
    private var selectedCellColumn = 0

    private var cells = mutableListOf<Cell>()

    //paint
    private val thickLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = thickLineStrokeWidth
    }

    private val thinLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = thinLineStrokeWidth
    }

    private val selectedCellPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = ContextCompat.getColor(context, R.color.selected_cell_color)
    }

    private val highlightedCellPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = ContextCompat.getColor(context, R.color.conflicted_number_cell_color)
    }

    private val conflictedCellPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = ContextCompat.getColor(context, R.color.conflicted_cell_color)
    }

    private val textPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
    }

    private val noteTextPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.BLACK
    }

    //override func..
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val sizeInPixels = min(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(sizeInPixels, sizeInPixels)
    }

    override fun onDraw(canvas: Canvas) {
        setMeasurements(width)
        paintCells(canvas)
        drawLines(canvas)
        drawText(canvas)
    }

    //set measurements
    private fun setMeasurements(width: Int) {
        cellSizePixels = (width / size).toFloat()
        textPaint.textSize = cellSizePixels / 1.5F
        noteSizePixels = cellSizePixels / squareSize.toFloat()
        noteTextPaint.textSize = cellSizePixels / squareSize.toFloat()
    }

    //draw things
    private fun drawLines(canvas: Canvas) {
        canvas.drawRect(
            0F, 0F, width.toFloat(), height.toFloat(), thickLinePaint
        )
        for (i in 1 until size) {
            val paintToUse = when (i % squareSize) {
                0 -> thickLinePaint
                else -> thinLinePaint
            }
            canvas.drawLine(
                i * cellSizePixels,
                0F,
                i * cellSizePixels,
                height.toFloat(),
                paintToUse
            )
            canvas.drawLine(
                0F,
                i * cellSizePixels,
                width.toFloat(),
                i * cellSizePixels,
                paintToUse
            )
        }
    }

    private fun paintCells(canvas: Canvas) {
        var selectedCell: Cell? = null

        cells.forEach { cell ->
            val r = cell.row
            val c = cell.col

            if (r == selectedCellRow && c == selectedCellColumn) {
                selectedCell = cell
                paintCell(canvas, r, c, selectedCellPaint)
            } else if (r == selectedCellRow || c == selectedCellColumn) {
                paintCell(canvas, r, c, conflictedCellPaint)
            } else if (r / squareSize == selectedCellRow / squareSize && c / squareSize == selectedCellColumn / squareSize) {
                paintCell(canvas, r, c, conflictedCellPaint)
            }
        }

        if (selectedCell?.isVisible != true && selectedCell?.isAnswerTrue != false) {
            return
        } else if (selectedCell?.isVisible == true) {
            cells.filter {
                it.isVisible && it.value == selectedCell?.value
            }.forEach {
                paintCell(canvas, it.row, it.col, highlightedCellPaint)
            }
        } else {
            cells.filter {
                it.isVisible && it.value == selectedCell?.tempValue || it.isAnswerTrue == false && it.tempValue == selectedCell?.tempValue
            }.forEach {
                paintCell(canvas, it.row, it.col, highlightedCellPaint)
            }
        }

    }

    private fun paintCell(canvas: Canvas, r: Int, c: Int, cellPaint: Paint) {
        canvas.drawRect(
            c * cellSizePixels,
            r * cellSizePixels,
            (c + 1) * cellSizePixels,
            (r + 1) * cellSizePixels,
            cellPaint
        )
    }

    private fun drawText(canvas: Canvas) {
        cells.forEach { cell ->
            when {
                cell.isVisible -> {
                    val valueString = cell.value.toString()
                    drawNumbers(canvas, cell, valueString)
                }
                cell.isAnswerTrue == false -> {
                    val valueString = cell.tempValue.toString()
                    drawNumbers(canvas, cell, valueString)
                }
                else -> {
                    val textBounds = Rect()
                    cell.notes.forEach { note ->
                        val valueString = note.toString()
                        val rowInCell = (note - 1) / squareSize
                        val colInCell = (note - 1) % squareSize
                        noteTextPaint.getTextBounds(valueString, 0, valueString.length, textBounds)
                        val textWidth = noteTextPaint.measureText(valueString)
                        val textHeight = textBounds.height()

                        canvas.drawText(
                            valueString,
                            (cell.col * cellSizePixels) + (colInCell * noteSizePixels) + noteSizePixels / 2 - textWidth / 2F,
                            (cell.row * cellSizePixels) + (rowInCell * noteSizePixels) + noteSizePixels / 2 + textHeight / 2F,
                            noteTextPaint

                        )
                    }
                }
            }
        }
    }

    private fun drawNumbers(canvas: Canvas, cell: Cell, valueString: String) {
        val row = cell.row
        val col = cell.col

        val textBounds = Rect()

        textPaint.getTextBounds(valueString, 0, valueString.length, textBounds)

        val textWidth = textPaint.measureText(valueString)
        val textHeight = textBounds.height()
        textPaint.color = ContextCompat.getColor(context, cell.textColor)
        canvas.drawText(
            valueString,
            (col * cellSizePixels) + cellSizePixels / 2 - textWidth / 2,
            (row * cellSizePixels) + cellSizePixels / 2 + textHeight / 2,
            textPaint
        )
    }

    //do on touch
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                handleTouchEvent(event.x, event.y)
                true
            }
            else -> {
                false
            }
        }
    }

    private fun handleTouchEvent(x: Float, y: Float) {
        val possibleSelectedRow = (y / cellSizePixels).toInt()
        val possibleSelectedCol = (x / cellSizePixels).toInt()
        listener?.onCellTouch(possibleSelectedRow, possibleSelectedCol)
    }

    fun updateSelectedCellUI(row: Int, col: Int) {
        selectedCellRow = row
        selectedCellColumn = col
        invalidate()
    }

    fun updateCells(cellList: MutableList<Cell>) {
        cells.clear()
        cells.addAll(cellList)
        invalidate()
    }

    fun registerListener(listener: OnTouchListener) {
        this.listener = listener
    }

    interface OnTouchListener {
        fun onCellTouch(row: Int, col: Int)
    }
}