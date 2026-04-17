package com.marconius.whackabraille.ui

import android.app.Application
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
            _screenState.postValue(if (result.canceled) GameScreenState.HOME else GameScreenState.ROUND_RESULTS)
        }
    }

    fun startDefaultRound() {
        _score.value = 0
        _streak.value = 0
        _latestAnnouncement.value = "Get ready"
        _activeTargetLabel.value = "Get ready"
        _lastRoundResult.value = null
        _currentMoleId.value = 0
        _screenState.value = GameScreenState.GAMEPLAY

        gameLoop.startRound(
            GameLoop.Options(
                modeId = "grade1Letters",
                durationSeconds = 30,
                inputMode = _selectedInputMode.value ?: InputMode.STANDARD,
                difficulty = Difficulty.NORMAL,
                speakBrailleDots = false,
                characterEcho = true,
                timerMusicEnabled = true,
                spatialMoleMappingEnabled = true,
            )
        )
    }

    fun finishRound() {
        gameLoop.finishRoundEarly()
    }

    fun returnHome() {
        _lastRoundResult.value = null
        _latestAnnouncement.value = "Waiting to start"
        _activeTargetLabel.value = "Waiting to start"
        _activeLane.value = null
        _currentMoleId.value = 0
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
        gameLoop.shutdown()
        super.onCleared()
    }
}
