package com.samedaksit.sudoku.model

import com.samedaksit.sudoku.R

data class Cell(
    val row: Int,
    val col: Int,
    val value: Int,
    var isInitial: Boolean = true,
    var isVisible: Boolean = true,
    var textColor: Int = R.color.default_number_color,
    //var isChangeable: Boolean = false,
    var isAnswerTrue: Boolean? = null,
    var tempValue: Int? = null,
    var notes: MutableSet<Int> = mutableSetOf()
)