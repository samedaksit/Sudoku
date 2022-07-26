package com.samedaksit.sudoku.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.samedaksit.sudoku.R
import com.samedaksit.sudoku.game.GenerateSudoku
import com.samedaksit.sudoku.model.Board
import com.samedaksit.sudoku.model.Cell
import com.samedaksit.sudoku.model.Mode
import kotlinx.coroutines.*

class GameViewModel : ViewModel() {
    private val generateSudoku = GenerateSudoku()

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    var cellsLiveData = MutableLiveData<MutableList<Cell>>()
    var selectedCellLiveData = MutableLiveData<Pair<Int, Int>>()

    var visibleNumberCountsLiveData = MutableLiveData<HashMap<Int, Int>>()
    var remainingChancesLiveData = MutableLiveData<Int>()

    var isTakingNotesLiveData = MutableLiveData<Boolean>()
    var takenNotesLiveData = MutableLiveData<Set<Int>>()

    var elapsedTimeLiveData = MutableLiveData<Int>()

    private var selectedRow = 1
    private var selectedCol = 1

    private var elapsedTime = 0
    private var isTakingNotes = false

    private val fullChances = 3

    private lateinit var board: Board
    private var cellList = MutableList(9 * 9) { i -> Cell(i / 9, i % 9, 0) }

    init {
        setChancesLeftLiveData(fullChances)
        isTakingNotesLiveData.value = isTakingNotes
        selectedCellLiveData.value = Pair(selectedRow, selectedCol)
    }

    // set game
    fun createSudokuAndFillCells(mode: Mode) {
        generateSudoku.generateSudoku(mode)
        cellList = generateSudoku.getCellList()

        assignCells(cellList)
    }

    fun openExistingGame(existingCells: MutableList<Cell>) {
        cellList.clear()
        cellList.addAll(existingCells)
        assignCells(cellList)
    }

    fun restartGame() {
        resetCellsProperties()
        setChancesLeftLiveData(fullChances)
        setCountedTime(0)
    }

    private fun resetCellsProperties() {
        cellList.forEach {
            if (!it.isInitial) {
                it.apply {
                    isVisible = false
                    isAnswerTrue = null
                    tempValue = null
                    notes = mutableSetOf()
                }
            }
        }

        assignCells(cellList)
    }


    //chances
    fun setChancesLeftLiveData(chances: Int) {
        remainingChancesLiveData.value = chances
    }

    //visible Number
    fun setVisibleNumberCounts() {
        val numberCount = hashMapOf<Int, Int>()
        for (i in 1..9) {
            numberCount[i] = 0
        }

        cellList.forEach { cell ->
            if (cell.isVisible || cell.tempValue != null) {
                val cellValue = if (cell.isVisible) cell.value else cell.tempValue
                val value = numberCount[cellValue]
                value?.let { numberCount[cellValue!!] = value + 1 }
            }
        }

        visibleNumberCountsLiveData.value = numberCount
    }

    //cells
    fun updateSelectedCell(row: Int, col: Int) {
        val cell = board.getCell(row, col)

        selectedRow = row
        selectedCol = col
        selectedCellLiveData.value = Pair(row, col)

        if (isTakingNotes) takenNotesLiveData.value = cell.notes
    }

    fun handleInput(number: Int) {
        val cell = board.getCell(selectedRow, selectedCol)

        if (!cell.isVisible) {
            if (number == 10) {
                cell.apply {
                    if (isAnswerTrue == false) {
                        tempValue = null
                        isAnswerTrue = null
                    } else {
                        notes = mutableSetOf()
                        takenNotesLiveData.value = cell.notes
                    }

                }
            } else {
                if (isTakingNotes) {
                    if (cell.notes.contains(number)) cell.notes.remove(number)
                    else cell.notes.add(number)

                    takenNotesLiveData.value = cell.notes
                } else {
                    cell.apply {
                        if (value == number) {
                            textColor = R.color.correct_answer_color
                            isVisible = true
                            isAnswerTrue = true
                            notes = mutableSetOf()
                        } else {
                            textColor = R.color.wrong_answer_color
                            isAnswerTrue = false
                            tempValue = number
                            remainingChancesLiveData.value = remainingChancesLiveData.value!! - 1
                        }
                    }
                }
            }
            assignCells(cellList)
        }
    }

    private fun assignCells(cells: MutableList<Cell>) {
        board = Board(9, cells)
        cellsLiveData.value = board.cells
    }

    //notes
    fun changeNoteTakingState() {
        isTakingNotes = !isTakingNotes
        isTakingNotesLiveData.value = isTakingNotes

        setCurrentNotes()
    }

    private fun setCurrentNotes() {
        val currentNotes =
            if (isTakingNotes) board.getCell(selectedRow, selectedCol).notes else setOf()

        takenNotesLiveData.value = currentNotes
    }

    //timer operations
    private fun startCoroutineTimer(
        delayMillis: Long = 0,
        repeatMillis: Long = 0,
        action: () -> Unit
    ) = scope.launch(Dispatchers.IO) {
        delay(delayMillis)
        if (repeatMillis > 0) {
            while (true) {
                action()
                delay(repeatMillis)
            }
        } else {
            action()
        }
    }

    private val timer: Job = startCoroutineTimer(delayMillis = 0, repeatMillis = 1000) {
        scope.launch(Dispatchers.Main) {
            elapsedTime += 1
            elapsedTimeLiveData.value = elapsedTime
        }
    }

    fun startTimer(time: Int) {
        setCountedTime(time)
        elapsedTimeLiveData.value = elapsedTime
        timer.start()
    }

    fun setCountedTime(time: Int) {
        elapsedTime = time
    }

    fun cancelTimer() {
        timer.cancel()
    }
}