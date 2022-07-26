package com.samedaksit.sudoku.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.Navigation
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.samedaksit.sudoku.R
import com.samedaksit.sudoku.databinding.FragmentOpeningBinding
import com.samedaksit.sudoku.model.Mode
import com.samedaksit.sudoku.model.Newness
import com.samedaksit.sudoku.util.CustomSharedPreferences

class OpeningFragment : Fragment() {

    private lateinit var binding: FragmentOpeningBinding

    private lateinit var customSharedPreferences: CustomSharedPreferences

    private var isGameExisting: Boolean? = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOpeningBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        customSharedPreferences = CustomSharedPreferences.invoke(requireContext())

        isGameExisting = customSharedPreferences.getIsUnfinishedGameExist()

        binding.isGameExisting = isGameExisting == true

        binding.continueGameButton.setOnClickListener {
            val gameMode = customSharedPreferences.getUnfinishedGameMode()
            navigateToGameFragment(gameMode, Newness.OLD)
        }

        binding.newGameButton.setOnClickListener {
            showModeSelectDialog()
        }

    }

    private fun showModeSelectDialog() {

        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(R.layout.select_game_mode_dialog)

        val easyLayout = dialog.findViewById<LinearLayout>(R.id.easyModeLayout)
        val mediumLayout = dialog.findViewById<LinearLayout>(R.id.mediumModeLayout)
        val hardLayout = dialog.findViewById<LinearLayout>(R.id.hardModeLayout)
        val expertLayout = dialog.findViewById<LinearLayout>(R.id.expertModeLayout)
        val restartGameText = dialog.findViewById<TextView>(R.id.restartGameText)

        if (isGameExisting == false) restartGameText?.visibility = View.GONE

        dialog.show()

        easyLayout?.setOnClickListener { modeLayoutClickListener(Mode.EASY, dialog) }
        mediumLayout?.setOnClickListener { modeLayoutClickListener(Mode.MEDIUM, dialog) }
        hardLayout?.setOnClickListener { modeLayoutClickListener(Mode.HARD, dialog) }
        expertLayout?.setOnClickListener { modeLayoutClickListener(Mode.EXPERT, dialog) }

        restartGameText?.setOnClickListener {
            navigateToGameFragment(Mode.MEDIUM, Newness.OLD_AND_RESTART)
            dialog.dismiss()
        }
    }

    private fun modeLayoutClickListener(mode: Mode, dialog: BottomSheetDialog) {
        navigateToGameFragment(mode, Newness.NEW)
        dialog.dismiss()
    }

    private fun navigateToGameFragment(mode: Mode, isNew: Newness) {
        val action =
            OpeningFragmentDirections.actionOpeningFragmentToGameFragment().setGameMode(mode)
                .setIsNewGame(isNew)
        Navigation.findNavController(binding.root).navigate(action)
    }
}