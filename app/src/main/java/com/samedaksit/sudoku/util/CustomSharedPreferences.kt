package com.samedaksit.sudoku.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.samedaksit.sudoku.model.Cell
import com.samedaksit.sudoku.model.Mode

class CustomSharedPreferences {
    companion object {
        private const val TIME_PASSED = "time_passed"
        private const val IS_UNFINISHED_GAME_EXIST = "is_unfinished_game_exist"
        private const val UNFINISHED_GAME_CELLS = "unfinished_game_cells"
        private const val UNFINISHED_GAME_CHANCES = "unfinished_game_chances"
        private const val UNFINISHED_GAME_TIME = "unfinished_game_time"
        private const val UNFINISHED_GAME_MODE = "unfinished_game_mode"

        val gson = Gson()

        private var sharedPreferences: SharedPreferences? = null

        @Volatile
        private var instance: CustomSharedPreferences? = null
        private val lock = Any()

        operator fun invoke(context: Context): CustomSharedPreferences =
            instance ?: synchronized(lock) {
                instance ?: makeCustomSharedPreferences(context).also {
                    instance = it
                }
            }

        private fun makeCustomSharedPreferences(context: Context): CustomSharedPreferences {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return CustomSharedPreferences()
        }
    }

    fun saveBestTime(time: Int) {
        sharedPreferences?.edit(commit = true) {
            putInt(TIME_PASSED, time)
        }
    }

    fun getBestTime() = sharedPreferences?.getInt(TIME_PASSED, 999999999)

    fun setIsUnfinishedGameExist(isThere: Boolean) {
        sharedPreferences?.edit(commit = true) {
            putBoolean(IS_UNFINISHED_GAME_EXIST, isThere)
        }
    }

    fun getIsUnfinishedGameExist() = sharedPreferences?.getBoolean(IS_UNFINISHED_GAME_EXIST, false)

    fun saveUnfinishedGameData(cells: MutableList<Cell>) {
        val dataJson = gson.toJson(cells)

        sharedPreferences?.edit(commit = true) {
            putString(UNFINISHED_GAME_CELLS, dataJson)
        }
    }

    fun getUnfinishedGameData(): MutableList<Cell> {
        val dataJson = sharedPreferences?.getString(UNFINISHED_GAME_CELLS, null)
        val type = object : TypeToken<MutableList<Cell>>() {}.type
        return gson.fromJson(dataJson, type)
    }

    fun setUnfinishedGameChances(chances: Int) {
        sharedPreferences?.edit(commit = true) {
            putInt(UNFINISHED_GAME_CHANCES, chances)
        }
    }

    fun getUnfinishedGameChances() = sharedPreferences?.getInt(UNFINISHED_GAME_CHANCES, 3)

    fun setUnfinishedGameTime(time: Int) {
        sharedPreferences?.edit(commit = true) {
            putInt(UNFINISHED_GAME_TIME, time)
        }
    }

    fun getUnfinishedGameTime() = sharedPreferences?.getInt(UNFINISHED_GAME_TIME, 0)

    fun setUnfinishedGameMode(mode: Mode) {
        val dataJson = gson.toJson(mode)

        sharedPreferences?.edit(commit = true) {
            putString(UNFINISHED_GAME_MODE, dataJson)
        }
    }

    fun getUnfinishedGameMode(): Mode {
        val dataJson = sharedPreferences?.getString(UNFINISHED_GAME_MODE, Mode.MEDIUM.toString())
        val type = object : TypeToken<Mode>() {}.type
        return gson.fromJson(dataJson, type)
    }
}