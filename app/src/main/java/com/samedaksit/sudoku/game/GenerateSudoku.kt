package com.samedaksit.sudoku.game

import com.samedaksit.sudoku.model.Cell
import com.samedaksit.sudoku.model.Mode

class GenerateSudoku {

    private val gridSize = 9
    private val gridBoxSize = 3

    private var grid = Array(9) { IntArray(9) { 0 } }
    private var cellList = mutableListOf<Cell>()
    private var randomIndexes = listOf<Int>()

    fun generateSudoku(mode: Mode) {
        createSudoku()
        fillCellList()
        generateRandomIndexesByMode(mode)
        changeCellsVisibility()
    }

    fun getCellList(): MutableList<Cell> {
        return cellList
    }

    //generating sudoku up to mode
    private fun createSudoku() {
        grid = Array(9) { IntArray(9) { 0 } }
        fillAllTheBoxes()
    }

    private fun fillCellList() {
        cellList = MutableList(9 * 9) { i ->
            Cell(i / 9, i % 9, grid[i / 9][i % 9])
        }
    }

    private fun generateRandomIndexesByMode(mode: Mode) {
        randomIndexes = when (mode) {
            Mode.EASY -> {
                (0..80).shuffled().take(42)
            }
            Mode.MEDIUM -> {
                (0..80).shuffled().take(52)
            }
            Mode.HARD -> {
                (0..80).shuffled().take(56)
            }
            Mode.EXPERT -> {
                (0..80).shuffled().take(60)
            }
        }
    }

    private fun changeCellsVisibility() {
        randomIndexes.forEach { index ->
            cellList[index].apply {
                isVisible = false
                isInitial = false
            }
        }
    }

    //fill all the Boxes
    private fun fillAllTheBoxes() {
        createDiagonalTemplate()
        fillRestOfTheBoxes(0, gridBoxSize)
    }

    //fill numbers in diagonal boxes
    private fun createDiagonalTemplate() {
        for (i in 0 until gridSize step gridBoxSize) {
            createBoxes(i, i)
        }
    }

    private fun createBoxes(row: Int, col: Int) {
        var randomGeneratedValue: Int

        for (j in 0 until gridBoxSize) {
            for (k in 0 until gridBoxSize) {
                do {
                    randomGeneratedValue = (0..9).shuffled().random()
                } while (!isUnused(row, col, randomGeneratedValue))
                grid[row + j][col + k] = randomGeneratedValue
            }
        }
    }

    private fun isUnused(rowStart: Int, columnStart: Int, digit: Int): Boolean {
        for (i in 0 until gridBoxSize) {
            for (j in 0 until gridBoxSize) {
                if (grid[rowStart + i][columnStart + j] == digit) return false
            }
        }
        return true
    }

    //fill numbers in rest of the boxes
    private fun fillRestOfTheBoxes(i: Int, j: Int): Boolean {
        var row = i
        var col = j

        if (col >= gridSize && row < gridSize - 1) {
            row += 1
            col = 0
        }
        if (row >= gridSize && col >= gridSize) {
            return true
        }
        if (row < gridBoxSize) {
            if (col < gridBoxSize) {
                col = gridBoxSize
            }
        } else if (row < gridSize - gridBoxSize) {
            if (col == (row / gridBoxSize) * gridBoxSize) {
                col += gridBoxSize
            }
        } else {
            if (col == gridSize - gridBoxSize) {
                row += 1
                col = 0
                if (row >= gridSize) {
                    return true
                }
            }
        }

        for (digit in 0..9) {
            if (isSafeToFillIn(row, col, digit)) {
                grid[row][col] = digit
                if (fillRestOfTheBoxes(row, col + 1)) {
                    return true
                }
                grid[row][col] = 0
            }
        }
        return false
    }

    private fun isSafeToFillIn(row: Int, col: Int, digit: Int) =
        isUnused(findBoxStart(row), findBoxStart(col), digit)
                && isUnusedInRow(row, digit)
                && isUnusedInColumn(col, digit)

    private fun findBoxStart(index: Int) = index - index % gridBoxSize

    private fun isUnusedInColumn(col: Int, digit: Int): Boolean {
        for (i in 0 until gridSize) {
            if (grid[i][col] == digit) {
                return false
            }
        }
        return true
    }

    private fun isUnusedInRow(row: Int, digit: Int): Boolean {
        for (i in 0 until gridSize) {
            if (grid[row][i] == digit) {
                return false
            }
        }
        return true
    }

}