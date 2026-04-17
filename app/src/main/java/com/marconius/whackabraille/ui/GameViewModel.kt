package com.marconius.whackabraille.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.marconius.whackabraille.core.Attempt
import com.marconius.whackabraille.core.Difficulty
import com.marconius.whackabraille.core.GameLoop
import com.marconius.whackabraille.core.InputMode
import com.marconius.whackabraille.core.RoundResult
import com.marconius.whackabraille.data.GamePreferences
import com.marconius.whackabraille.data.GameRepository

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameRepository(GamePreferences(application))
    private val gameLoop = GameLoop()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingRoundStart: Runnable? = null

    private val _screenState = MutableLiveData(GameScreenState.HOME)
    val screenState: LiveData<GameScreenState> = _screenState

    private val _score = MutableLiveData(0)
    val score: LiveData<Int> = _score

    private val _streak = MutableLiveData(0)
    val streak: LiveData<Int> = _streak

    private val _activeLane = MutableLiveData<Int?>(null)
    val activeLane: LiveData<Int?> = _activeLane

    private val _activeTargetLabel = MutableLiveData("Waiting to start")
    val activeTargetLabel: LiveData<String> = _activeTargetLabel

    private val _latestAnnouncement = MutableLiveData("Waiting to start")
    val latestAnnouncement: LiveData<String> = _latestAnnouncement

    private val _speechAnnouncement = MutableLiveData<String?>(null)
    val speechAnnouncement: LiveData<String?> = _speechAnnouncement

    private val _lastRoundResult = MutableLiveData<RoundResult?>(null)
    val lastRoundResult: LiveData<RoundResult?> = _lastRoundResult

    private val _totalTickets = MutableLiveData(repository.getTotalTickets())
    val totalTickets: LiveData<Int> = _totalTickets

    private val _prizeShelfCount = MutableLiveData(repository.getPrizeShelfCount())
    val prizeShelfCount: LiveData<Int> = _prizeShelfCount

    private val _selectedInputMode = MutableLiveData(InputMode.STANDARD)
    val selectedInputMode: LiveData<InputMode> = _selectedInputMode

    private val _currentMoleId = MutableLiveData(0)
    val currentMoleId: LiveData<Int> = _currentMoleId

    init {
        gameLoop.onScoreUpdated = { score, streak ->
            _score.postValue(score)
            _streak.postValue(streak)
        }

        gameLoop.onActiveMoleChanged = { lane, item ->
            _activeLane.postValue(lane)
            _activeTargetLabel.postValue(item?.announceText ?: "Listen for the next mole")
            _currentMoleId.postValue(gameLoop.currentMoleId)
        }

        gameLoop.onAnnouncementRequested = { text ->
            _latestAnnouncement.postValue(text)
            _speechAnnouncement.postValue(text)
        }

        gameLoop.onRoundEnded = { result ->
            if (!result.canceled && !result.isTraining) {
                val updatedTickets = (repository.getTotalTickets() + result.totalTickets).coerceAtLeast(0)
                repository.saveTotalTickets(updatedTickets)
                _totalTickets.postValue(updatedTickets)
            }

            _lastRoundResult.postValue(result)
            _activeLane.postValue(null)
            _activeTargetLabel.postValue(if (result.canceled) "Round stopped" else "Round finished")
            _speechAnnouncement.postValue(null)
            _screenState.postValue(if (result.canceled) GameScreenState.HOME else GameScreenState.ROUND_RESULTS)
        }
    }

    fun startDefaultRound() {
        cancelPendingRoundStart()
        _score.value = 0
        _streak.value = 0
        _latestAnnouncement.value = "Ready?"
        _activeTargetLabel.value = "Ready?"
        _lastRoundResult.value = null
        _currentMoleId.value = 0
        _screenState.value = GameScreenState.GAMEPLAY
        _speechAnnouncement.value = "Ready?"

        val roundOptions = GameLoop.Options(
            modeId = "grade1Letters",
            durationSeconds = 30,
            inputMode = _selectedInputMode.value ?: InputMode.STANDARD,
            difficulty = Difficulty.NORMAL,
            speakBrailleDots = false,
            characterEcho = true,
            timerMusicEnabled = true,
            spatialMoleMappingEnabled = true,
        )
        val startRoundRunnable = Runnable {
            pendingRoundStart = null
            gameLoop.startRound(roundOptions)
        }
        pendingRoundStart = startRoundRunnable
        mainHandler.postDelayed(startRoundRunnable, ROUND_START_DELAY_MS)
    }

    fun finishRound() {
        cancelPendingRoundStart()
        gameLoop.finishRoundEarly()
    }

    fun returnHome() {
        cancelPendingRoundStart()
        _lastRoundResult.value = null
        _latestAnnouncement.value = "Waiting to start"
        _activeTargetLabel.value = "Waiting to start"
        _activeLane.value = null
        _currentMoleId.value = 0
        _speechAnnouncement.value = null
        _screenState.value = GameScreenState.HOME
    }

    fun setSelectedInputMode(inputMode: InputMode) {
        _selectedInputMode.value = inputMode
    }

    fun handleStandardKeyInput(key: String) {
        if (_screenState.value != GameScreenState.GAMEPLAY) return
        val moleId = _currentMoleId.value ?: return
        gameLoop.handleAttempt(
            Attempt(
                moleId = moleId,
                type = InputMode.STANDARD,
                key = key,
            )
        )
    }

    fun handlePerkinsInput(dotMask: Int) {
        if (_screenState.value != GameScreenState.GAMEPLAY) return
        val moleId = _currentMoleId.value ?: return
        gameLoop.handleAttempt(
            Attempt(
                moleId = moleId,
                type = InputMode.PERKINS,
                dotMask = dotMask,
            )
        )
    }

    fun handleBrailleTextInput(text: String, mode: InputMode) {
        if (_screenState.value != GameScreenState.GAMEPLAY) return
        val moleId = _currentMoleId.value ?: return
        gameLoop.handleAttempt(
            Attempt(
                moleId = moleId,
                type = mode,
                text = text,
            )
        )
    }

    override fun onCleared() {
        cancelPendingRoundStart()
        _speechAnnouncement.value = null
        gameLoop.shutdown()
        super.onCleared()
    }

    private fun cancelPendingRoundStart() {
        pendingRoundStart?.let(mainHandler::removeCallbacks)
        pendingRoundStart = null
    }

    companion object {
        private const val ROUND_START_DELAY_MS = 1_100L
    }
}
