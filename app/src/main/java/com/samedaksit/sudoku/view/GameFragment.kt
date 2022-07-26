package com.samedaksit.sudoku.view

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.samedaksit.sudoku.R
import com.samedaksit.sudoku.customview.SudokuBoardView
import com.samedaksit.sudoku.databinding.FragmentGameBinding
import com.samedaksit.sudoku.model.Cell
import com.samedaksit.sudoku.model.Mode
import com.samedaksit.sudoku.model.Newness
import com.samedaksit.sudoku.util.CustomSharedPreferences
import com.samedaksit.sudoku.viewmodel.GameViewModel

class GameFragment : Fragment(), SudokuBoardView.OnTouchListener {

    private lateinit var binding: FragmentGameBinding
    private lateinit var viewModel: GameViewModel
    private lateinit var customSharedPreferences: CustomSharedPreferences

    private lateinit var numberViews: List<View>
    private lateinit var gameMode: Mode

    private var chanceLeft = 3
    private var timeCount = 0
    private var isGameExisting = true
    private var cells = MutableList(9 * 9) { i -> Cell(i / 9, i % 9, 0) }

    private var mRewardedAd: RewardedAd? = null
    private var isRewarded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        customSharedPreferences = CustomSharedPreferences.invoke(requireContext())
        setSudokuBoardListener()
        setNumberViewsList()
        setClickListeners()
        initializeViewModel()
        observeLiveData()

        arguments?.let {
            when (GameFragmentArgs.fromBundle(it).isNewGame) {
                Newness.NEW -> {
                    gameMode = GameFragmentArgs.fromBundle(it).gameMode
                    initializeNewSudoku(gameMode)
                }
                Newness.OLD -> {
                    doIfOldGame()
                }
                Newness.OLD_AND_RESTART -> {
                    doIfOldGameAndRestartIt()
                }
            }
            setGameModeText(gameMode)
            updateNumberCounts()
        }

        initializeAds()
        buildAd()


    }

    private fun initializeAds() {
        MobileAds.initialize(requireContext()) {}
    }

    private fun buildAd() {
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            requireContext(),
            "ca-app-pub-3940256099942544/5224354917",
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mRewardedAd = null
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                }
            })
    }

    private fun doIfOldGame() {
        val chances = customSharedPreferences.getUnfinishedGameChances()
        val time = customSharedPreferences.getUnfinishedGameTime()

        gameMode = customSharedPreferences.getUnfinishedGameMode()
        viewModel.openExistingGame(customSharedPreferences.getUnfinishedGameData())

        viewModel.setChancesLeftLiveData(chances!!)
        viewModel.setCountedTime(time!!)
    }

    private fun doIfOldGameAndRestartIt() {
        gameMode = customSharedPreferences.getUnfinishedGameMode()
        viewModel.openExistingGame(customSharedPreferences.getUnfinishedGameData())
        restartGame()
    }

    private fun initializeNewSudoku(gameMode: Mode) {
        viewModel.createSudokuAndFillCells(gameMode)
        viewModel.startTimer(0)
    }

    private fun setGameModeText(gameMode: Mode) {
        val modeInTurkish = makeModeTurkish(gameMode)
        binding.gameModeText.text = getString(R.string.toolbar_game_mode_title, modeInTurkish)
    }

    private fun makeModeTurkish(gameMode: Mode): String {
        val turkishVersion = when (gameMode) {
            Mode.EASY -> getString(R.string.mode_easy_text)
            Mode.MEDIUM -> getString(R.string.mode_medium_text)
            Mode.HARD -> getString(R.string.mode_hard_text)
            Mode.EXPERT -> getString(R.string.mode_expert_text)
        }
        return turkishVersion
    }

    private fun setSudokuBoardListener() {
        binding.sudokuBoard.registerListener(this)
    }

    private fun setNumberViewsList() {
        numberViews = listOf(
            binding.numberOne,
            binding.numberTwo,
            binding.numberThree,
            binding.numberFour,
            binding.numberFive,
            binding.numberSix,
            binding.numberSeven,
            binding.numberEight,
            binding.numberNine,
            binding.deleteNumber
        )
    }

    private fun setClickListeners() {
        setGoBackButtonListener()
        setNumberClicks()
        setEditNotesClickListener()
        setRestartGameButtonListener()
    }

    private fun setGoBackButtonListener() {
        binding.goBackButton.setOnClickListener {
            goOpeningPage()
        }
    }

    private fun setNumberClicks() {
        numberViews.forEachIndexed { index, textView ->
            textView.setOnClickListener {
                viewModel.handleInput(index + 1)
                updateNumberCounts()
            }
        }
    }

    private fun setEditNotesClickListener() {
        binding.editNotes.setOnClickListener { viewModel.changeNoteTakingState() }
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(this)[GameViewModel::class.java]
    }

    private fun setRestartGameButtonListener() {
        binding.restartGame.setOnClickListener {
            restartGame()
        }
    }

    private fun restartGame() {
        viewModel.restartGame()
        updateNumberCounts()
    }

    private fun observeLiveData() {
        observeCountedTime()
        observeInputNumbersCountLiveData()
        observeCellsLiveData()
        observeSelectedCellLiveData()
        observeChanceLeftLiveData()
        observeIsTakingNotesLiveData()
        observeTakenNotesLiveData()
    }

    private fun observeCountedTime() {
        viewModel.elapsedTimeLiveData.observe(viewLifecycleOwner) {
            binding.timeCounted = it
            timeCount = it
        }
    }

    private fun observeInputNumbersCountLiveData() {
        viewModel.visibleNumberCountsLiveData.observe(viewLifecycleOwner) {
            setNumberViewsVisibility(it)
        }
    }

    private fun setNumberViewsVisibility(map: HashMap<Int, Int>) {
        map.forEach { (n, i) ->
            if (i == 9) {
                numberViews[n - 1].visibility = View.INVISIBLE
            } else {
                numberViews[n - 1].visibility = View.VISIBLE
            }
        }
    }

    private fun observeCellsLiveData() {
        viewModel.cellsLiveData.observe(viewLifecycleOwner) { cells ->
            updateCells(cells)
            doIfGameFinished(cells)
        }
    }

    private fun updateCells(cells: MutableList<Cell>?) = cells?.let {
        this.cells.clear()
        this.cells.addAll(cells)
        binding.sudokuBoard.updateCells(cells)
    }

    private fun doIfGameFinished(cells: MutableList<Cell>?) = cells?.let { cellList ->
        if (checkIfGameFinished(cellList)) {
            isGameExisting = false
            setGameFinishedSuccessDialog()
            viewModel.elapsedTimeLiveData.removeObservers(viewLifecycleOwner)
        }
    }

    private fun checkIfGameFinished(cells: MutableList<Cell>?): Boolean {
        cells?.let {
            var invisibleCellCount = 0
            cells.forEach { cell ->
                if (!cell.isVisible) invisibleCellCount += 1
            }
            return invisibleCellCount == 0
        }
        return false
    }

    private fun setGameFinishedSuccessDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.game_end_success_layout)

        val bestTimeTextView = dialog.findViewById<TextView>(R.id.bestTimeText)
        val chanceImg2 = dialog.findViewById<ImageView>(R.id.secondChanceImg)
        val chanceImg3 = dialog.findViewById<ImageView>(R.id.thirdChanceImg)
        val nextGameButton = dialog.findViewById<Button>(R.id.nextGameButton)
        val goHomeButton = dialog.findViewById<TextView>(R.id.goBackToHomeButton)

        bestTimeTextView.text = checkIfBestTimeAndReturnIt().toString()
        doWhenChanceNumberChanges(chanceImg2, chanceImg3)

        setNextGameButtonClickListener(nextGameButton, dialog)
        setGoHomeButtonClickListener(goHomeButton, dialog)

        dialogSettings(dialog)
        dialog.show()
    }

    private fun checkIfBestTimeAndReturnIt(): Int {
        var bestTime = customSharedPreferences.getBestTime()
        bestTime?.let {
            if (timeCount < it) {
                customSharedPreferences.saveBestTime(timeCount)
                bestTime = timeCount
            }
        }
        return bestTime!!
    }

    private fun doWhenChanceNumberChanges(chanceImg2: ImageView, chanceImg3: ImageView) {
        when (chanceLeft) {
            2 -> chanceImg3.setImageResource(R.drawable.favorite_border_red)
            1 -> {
                chanceImg2.setImageResource(R.drawable.favorite_border_red)
                chanceImg3.setImageResource(R.drawable.favorite_border_red)
            }
        }
    }

    private fun setNextGameButtonClickListener(nextGameButton: Button, dialog: Dialog) {
        nextGameButton.setOnClickListener {
            setNextGame(gameMode)
            viewModel.startTimer(0)
            observeCountedTime()
            viewModel.setChancesLeftLiveData(3)
            dialog.dismiss()
        }
    }

    private fun setGoHomeButtonClickListener(goHomeButton: TextView, dialog: Dialog) {
        goHomeButton.setOnClickListener {
            goOpeningPage()
            dialog.dismiss()
        }
    }

    private fun setNextGame(gameMode: Mode) {
        viewModel.createSudokuAndFillCells(gameMode)
        updateNumberCounts()
    }

    private fun updateNumberCounts() {
        viewModel.setVisibleNumberCounts()
    }

    private fun observeSelectedCellLiveData() {
        viewModel.selectedCellLiveData.observe(viewLifecycleOwner) { cell ->
            binding.sudokuBoard.updateSelectedCellUI(cell.first, cell.second)
        }
    }

    private fun observeChanceLeftLiveData() {
        viewModel.remainingChancesLiveData.observe(viewLifecycleOwner) { count ->
            binding.chanceLeft = count
            chanceLeft = count
            if (count <= 0) {
                isGameExisting = false
                viewModel.elapsedTimeLiveData.removeObservers(viewLifecycleOwner)
                setGameEndFailedDialog()
            }
        }
    }

    private fun setGameEndFailedDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.game_end_failed_layout)

        val watchAdButton = dialog.findViewById<Button>(R.id.watchVideoButton)
        val playAgainButton = dialog.findViewById<TextView>(R.id.playAgainButton)

        dialogSettings(dialog)
        dialog.show()
        setPlayAgainButtonClickListener(dialog, playAgainButton)
        setWatchAddButtonClickListener(dialog, watchAdButton)

    }

    private fun setPlayAgainButtonClickListener(dialog: Dialog, playAgainButton: TextView) {
        playAgainButton.setOnClickListener {
            restartGame()
            viewModel.startTimer(0)
            observeCountedTime()
            dialog.dismiss()
        }
    }

    private fun setWatchAddButtonClickListener(dialog: Dialog, watchAdButton: Button) {
        watchAdButton.setOnClickListener {
            viewModel.remainingChancesLiveData.removeObservers(viewLifecycleOwner)
            showAd(dialog)
            isGameExisting = true
            dialog.dismiss()
        }
    }

    private fun showAd(dialog: Dialog) {
        setAdSettings(dialog)
        if (mRewardedAd != null) {
            mRewardedAd?.show(requireActivity()) {
                fun onUserEarnedReward() {
                    isRewarded = true
                }
                onUserEarnedReward()
            }

        } else {
            isRewarded = false
        }

    }

    private fun setAdSettings(dialog: Dialog) {
        mRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
            }

            override fun onAdDismissedFullScreenContent() {
                mRewardedAd = null

                if (isRewarded) {
                    viewModel.startTimer(timeCount)
                    observeCountedTime()
                    viewModel.setChancesLeftLiveData(1)
                    observeChanceLeftLiveData()
                    isRewarded = false
                    dialog.dismiss()
                }

                buildAd()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                mRewardedAd = null
            }

            override fun onAdImpression() {
            }

            override fun onAdShowedFullScreenContent() {
            }
        }
    }

    private fun dialogSettings(dialog: Dialog) {
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        setDialogLayout(dialog)
    }

    private fun setDialogLayout(dialog: Dialog) {
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun observeIsTakingNotesLiveData() {
        viewModel.isTakingNotesLiveData.observe(viewLifecycleOwner) { updateNoteTakingUI(it) }
    }

    private fun updateNoteTakingUI(isNoteTaking: Boolean?) = isNoteTaking?.let {
        val color = if (it) {
            ContextCompat.getColor(
                requireContext(),
                R.color.selected_cell_color
            )
        } else {
            ContextCompat.getColor(
                requireContext(),
                R.color.white
            )
        }
        binding.editColor = color
    }

    private fun observeTakenNotesLiveData() {
        viewModel.takenNotesLiveData.observe(viewLifecycleOwner) { updateTakenNotes(it) }
    }

    private fun updateTakenNotes(numbers: Set<Int>?) = numbers?.let { set ->
        numberViews.forEachIndexed { index, button ->
            val color = if (set.contains(index + 1))
                ContextCompat.getColor(
                    requireContext(),
                    R.color.selected_cell_color
                ) else ContextCompat.getColor(
                requireContext(),
                R.color.white
            )
            button.setBackgroundColor(color)
        }
    }

    private fun goOpeningPage() {
        val action = GameFragmentDirections.actionGameFragmentToOpeningFragment()
        Navigation.findNavController(binding.root).navigate(action)
    }

    override fun onCellTouch(row: Int, col: Int) {
        viewModel.updateSelectedCell(row, col)
    }

    override fun onPause() {
        super.onPause()
        if (isGameExisting) {
            setCustomPrefsOnPause()
        } else {
            customSharedPreferences.setIsUnfinishedGameExist(isGameExisting)
        }

    }

    private fun setCustomPrefsOnPause() {
        customSharedPreferences.setIsUnfinishedGameExist(isGameExisting)
        customSharedPreferences.saveUnfinishedGameData(cells)
        customSharedPreferences.setUnfinishedGameChances(chanceLeft)
        customSharedPreferences.setUnfinishedGameMode(gameMode)
        customSharedPreferences.setUnfinishedGameTime(timeCount)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.cancelTimer()
    }
}