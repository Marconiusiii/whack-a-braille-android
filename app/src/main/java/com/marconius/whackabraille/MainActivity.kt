package com.marconius.whackabraille

import android.os.Bundle
import android.text.InputType
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.marconius.whackabraille.databinding.ActivityMainBinding
import com.marconius.whackabraille.input.PerkinsInputTracker
import com.marconius.whackabraille.core.InputMode
import com.marconius.whackabraille.speech.AndroidSpeechEngine
import com.marconius.whackabraille.ui.GameScreenState
import com.marconius.whackabraille.ui.GameViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: GameViewModel
    private lateinit var speechEngine: AndroidSpeechEngine
    private val perkinsInputTracker = PerkinsInputTracker()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[GameViewModel::class.java]
        speechEngine = AndroidSpeechEngine(this)

        binding.homeScreen.inputModeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val inputMode = when (checkedId) {
                R.id.inputModePerkinsRadio -> InputMode.PERKINS
                R.id.inputModeBrailleTextRadio -> InputMode.BRAILLE_TEXT
                else -> InputMode.STANDARD
            }
            viewModel.setSelectedInputMode(inputMode)
        }

        binding.homeScreen.startGameButton.setOnClickListener {
            viewModel.startDefaultRound()
        }

        binding.gameplayScreen.exitRoundButton.setOnClickListener {
            viewModel.finishRound()
        }

        binding.resultsScreen.resultsHomeButton.setOnClickListener {
            viewModel.returnHome()
        }

        binding.gameplayScreen.brailleEntryEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)
        binding.gameplayScreen.submitBrailleEntryButton.setOnClickListener {
            submitBrailleEntry()
        }
        binding.gameplayScreen.brailleEntryEditText.setOnEditorActionListener { _, _, _ ->
            submitBrailleEntry()
            true
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.screenState.observe(this) { state ->
            binding.homeScreen.root.visibility = if (state == GameScreenState.HOME) View.VISIBLE else View.GONE
            binding.gameplayScreen.root.visibility = if (state == GameScreenState.GAMEPLAY) View.VISIBLE else View.GONE
            binding.resultsScreen.root.visibility = if (state == GameScreenState.ROUND_RESULTS) View.VISIBLE else View.GONE
            if (state != GameScreenState.GAMEPLAY) {
                perkinsInputTracker.reset()
                binding.gameplayScreen.brailleEntryEditText.text?.clear()
            }
        }

        viewModel.totalTickets.observe(this) { tickets ->
            binding.homeScreen.homeTicketsValueText.text = resources.getQuantityString(
                R.plurals.saved_tickets,
                tickets,
                tickets,
            )
        }

        viewModel.prizeShelfCount.observe(this) { count ->
            binding.homeScreen.homePrizeShelfText.text = resources.getQuantityString(
                R.plurals.prize_shelf_count,
                count,
                count,
            )
        }

        viewModel.score.observe(this) { score ->
            binding.gameplayScreen.gameplayScoreText.text = getString(R.string.score_value, score)
        }

        viewModel.streak.observe(this) { streak ->
            binding.gameplayScreen.gameplayStreakText.text = getString(R.string.streak_value, streak)
        }

        viewModel.latestAnnouncement.observe(this) { text ->
            binding.gameplayScreen.gameplayAnnouncementText.text = text
            speechEngine.speak(text)
        }

        viewModel.activeLane.observe(this) { lane ->
            updateBoard(lane, viewModel.activeTargetLabel.value.orEmpty())
        }

        viewModel.activeTargetLabel.observe(this) { label ->
            updateBoard(viewModel.activeLane.value, label)
        }

        viewModel.lastRoundResult.observe(this) { result ->
            if (result == null) return@observe
            binding.resultsScreen.resultsTitleText.text =
                if (result.isTraining) getString(R.string.training_complete_title) else getString(R.string.results_title)
            binding.resultsScreen.resultsScoreText.text = getString(R.string.score_value, result.score)
            binding.resultsScreen.resultsTicketsRoundText.text = getString(R.string.tickets_round_value, result.totalTickets)
            binding.resultsScreen.resultsTicketsTotalText.text = getString(
                R.string.total_tickets_value,
                viewModel.totalTickets.value ?: 0,
            )
            binding.resultsScreen.resultsStatsText.text = getString(
                R.string.results_stats_value,
                result.hits,
                result.misses,
                result.escapes,
            )
        }
    }

    private fun updateBoard(activeLane: Int?, label: String) {
        val laneViews = listOf(
            binding.gameplayScreen.lane0Text,
            binding.gameplayScreen.lane1Text,
            binding.gameplayScreen.lane2Text,
            binding.gameplayScreen.lane3Text,
            binding.gameplayScreen.lane4Text,
        )

        laneViews.forEachIndexed { index, textView ->
            if (index == activeLane) {
                textView.text = label
                textView.setBackgroundColor(getColor(android.R.color.holo_orange_light))
            } else {
                textView.text = getString(R.string.lane_waiting)
                textView.setBackgroundColor(getColor(android.R.color.darker_gray))
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (viewModel.screenState.value != GameScreenState.GAMEPLAY) {
            return super.dispatchKeyEvent(event)
        }

        if (event.deviceId == KeyCharacterMap.VIRTUAL_KEYBOARD) {
            return super.dispatchKeyEvent(event)
        }

        val selectedInputMode = viewModel.selectedInputMode.value ?: InputMode.STANDARD

        if (selectedInputMode == InputMode.PERKINS) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (perkinsInputTracker.onKeyDown(event.keyCode)) {
                        return true
                    }
                }
                KeyEvent.ACTION_UP -> {
                    val dotMask = perkinsInputTracker.onKeyUp(event.keyCode)
                    if (dotMask != null) {
                        viewModel.handlePerkinsInput(dotMask)
                        return true
                    }
                }
            }
        }

        if (selectedInputMode == InputMode.STANDARD && event.action == KeyEvent.ACTION_DOWN) {
            val unicodeChar = event.unicodeChar
            if (unicodeChar != 0) {
                val key = unicodeChar.toChar().toString()
                viewModel.handleStandardKeyInput(key)
                return true
            }
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        speechEngine.shutdown()
        super.onDestroy()
    }

    private fun submitBrailleEntry() {
        val selectedInputMode = viewModel.selectedInputMode.value ?: InputMode.STANDARD
        if (selectedInputMode != InputMode.BRAILLE_TEXT && selectedInputMode != InputMode.BRAILLE_DISPLAY) {
            return
        }

        val text = binding.gameplayScreen.brailleEntryEditText.text?.toString().orEmpty()
        if (text.isBlank()) return

        viewModel.handleBrailleTextInput(text, selectedInputMode)
        binding.gameplayScreen.brailleEntryEditText.text?.clear()
    }
}
