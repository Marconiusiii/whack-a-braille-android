package com.marconius.whackabraille

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
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

        binding.homeScreen.homeHowToPlayButton.setOnClickListener {
            showComingSoonDialog(R.string.how_to_play_message)
        }

        binding.homeScreen.homeGameSettingsButton.setOnClickListener {
            showInputModeDialog()
        }

        binding.homeScreen.homeCashInButton.setOnClickListener {
            showComingSoonDialog(R.string.cash_in_tickets_message)
        }

        binding.homeScreen.homeClearPrizeShelfButton.setOnClickListener {
            showComingSoonDialog(R.string.clear_prize_shelf_message)
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
                speechEngine.stop()
                perkinsInputTracker.reset()
                binding.gameplayScreen.brailleEntryEditText.text?.clear()
                binding.gameplayScreen.brailleEntryEditText.clearFocus()
                hideKeyboard()
            } else {
                focusGameplayInputIfNeeded()
            }
        }

        viewModel.totalTickets.observe(this) { tickets ->
            binding.homeScreen.homeCashInButton.text = getString(
                R.string.cash_in_tickets_button,
                tickets,
            )
            binding.homeScreen.homeCashInButton.contentDescription = getString(
                R.string.cash_in_tickets_accessibility,
                tickets,
            )
        }

        viewModel.selectedInputMode.observe(this) { inputMode ->
            binding.homeScreen.homeGameSettingsButton.contentDescription = getString(
                R.string.current_input_mode_value,
                inputMode.label,
            )
            updateGameplayInputUi(inputMode)
            focusGameplayInputIfNeeded()
        }

        viewModel.prizeShelfCount.observe(this) { count ->
            binding.homeScreen.homePrizeShelfText.text = resources.getQuantityString(
                R.plurals.prize_shelf_count,
                count,
                count,
            )
        }

        viewModel.speechAnnouncement.observe(this) { text ->
            if (viewModel.screenState.value == GameScreenState.GAMEPLAY && !text.isNullOrBlank()) {
                speechEngine.speak(text)
            }
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

    private fun updateGameplayInputUi(inputMode: InputMode) {
        val usesBufferedTextEntry = inputMode.usesBufferedTextEntry

        binding.gameplayScreen.brailleEntryEditText.isVisible = usesBufferedTextEntry
        binding.gameplayScreen.submitBrailleEntryButton.isVisible = usesBufferedTextEntry

        binding.gameplayScreen.brailleEntryEditText.hint = when (inputMode) {
            InputMode.BRAILLE_DISPLAY -> getString(R.string.braille_display_entry_hint)
            else -> getString(R.string.braille_entry_hint)
        }

        binding.gameplayScreen.submitBrailleEntryButton.text = when (inputMode) {
            InputMode.BRAILLE_DISPLAY -> getString(R.string.submit_braille_display_entry)
            else -> getString(R.string.submit_braille_entry)
        }

        if (!usesBufferedTextEntry) {
            binding.gameplayScreen.brailleEntryEditText.text?.clear()
        }
    }

    private fun focusGameplayInputIfNeeded() {
        val inputMode = viewModel.selectedInputMode.value ?: return
        if (viewModel.screenState.value != GameScreenState.GAMEPLAY || !inputMode.usesBufferedTextEntry) {
            return
        }

        val entryField = binding.gameplayScreen.brailleEntryEditText
        entryField.post {
            if (viewModel.screenState.value != GameScreenState.GAMEPLAY) {
                return@post
            }
            entryField.requestFocus()
            entryField.setSelection(entryField.text?.length ?: 0)
            val inputMethodManager = getSystemService(InputMethodManager::class.java)
            inputMethodManager?.showSoftInput(entryField, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(InputMethodManager::class.java)
        inputMethodManager?.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun showInputModeDialog() {
        val inputModes = InputMode.entries.toTypedArray()
        val currentIndex = inputModes.indexOf(viewModel.selectedInputMode.value ?: InputMode.STANDARD)

        AlertDialog.Builder(this)
            .setTitle(R.string.input_mode_dialog_title)
            .setSingleChoiceItems(
                inputModes.map { it.label }.toTypedArray(),
                currentIndex,
            ) { dialog, which ->
                viewModel.setSelectedInputMode(inputModes[which])
                dialog.dismiss()
            }
            .setNegativeButton(R.string.ok, null)
            .show()
    }

    private fun showComingSoonDialog(messageResId: Int) {
        AlertDialog.Builder(this)
            .setTitle(R.string.coming_soon_title)
            .setMessage(messageResId)
            .setPositiveButton(R.string.ok, null)
            .show()
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
