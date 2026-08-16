package io.github.fate_grand_automata.ui.custom_card_selection

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.fate_grand_automata.prefs.core.BattleConfigCore
import io.github.fate_grand_automata.scripts.models.CustomCardSelection
import io.github.fate_grand_automata.scripts.models.CustomCardSelectionPerTurn
import io.github.fate_grand_automata.scripts.models.CustomCardSelectionTurn
import io.github.fate_grand_automata.scripts.models.CustomCardSelectionWave
import javax.inject.Inject

@HiltViewModel
class CustomCardSelectionViewModel @Inject constructor(
    val battleConfig: BattleConfigCore
) : ViewModel() {
    data class TurnState(
        val candidates: SnapshotStateList<CustomCardSelection>
    )

    data class WaveState(
        val turns: SnapshotStateList<TurnState>
    )

    val waves: SnapshotStateList<WaveState> by lazy {
        battleConfig.customCardSelection.get().map { wave ->
            WaveState(
                wave.map { turn -> TurnState(turn.toMutableStateList()) }
                    .toMutableStateList()
            )
        }.toMutableStateList()
    }

    fun addWave() {
        waves.add(
            WaveState(
                mutableListOf(
                    TurnState(mutableListOf(CustomCardSelection.empty).toMutableStateList())
                ).toMutableStateList()
            )
        )
        save()
    }

    fun copyWave(index: Int) {
        val source = waves.getOrNull(index) ?: return
        waves.add(
            WaveState(
                source.turns.map { turn ->
                    TurnState(turn.candidates.toList().toMutableStateList())
                }.toMutableStateList()
            )
        )
        save()
    }

    fun removeWave(index: Int) {
        if (waves.size > 1 && index in waves.indices) {
            waves.removeAt(index)
            save()
        }
    }

    fun addTurn(waveIndex: Int) {
        waves.getOrNull(waveIndex)?.turns?.add(
            TurnState(mutableListOf(CustomCardSelection.empty).toMutableStateList())
        )
        save()
    }

    fun copyTurn(waveIndex: Int, turnIndex: Int) {
        val turns = waves.getOrNull(waveIndex)?.turns ?: return
        val source = turns.getOrNull(turnIndex) ?: return
        turns.add(TurnState(source.candidates.toList().toMutableStateList()))
        save()
    }

    fun removeTurn(waveIndex: Int, turnIndex: Int) {
        val turns = waves.getOrNull(waveIndex)?.turns ?: return
        if (turns.size > 1 && turnIndex in turns.indices) {
            turns.removeAt(turnIndex)
            save()
        }
    }

    fun addCandidate(waveIndex: Int, turnIndex: Int) {
        waves.getOrNull(waveIndex)?.turns?.getOrNull(turnIndex)
            ?.candidates?.add(CustomCardSelection.empty)
        save()
    }

    fun removeCandidate(waveIndex: Int, turnIndex: Int, candidateIndex: Int) {
        val candidates = waves.getOrNull(waveIndex)?.turns?.getOrNull(turnIndex)?.candidates ?: return
        if (candidateIndex > 0 && candidateIndex in candidates.indices) {
            candidates.removeAt(candidateIndex)
            save()
        }
    }

    fun moveCandidate(waveIndex: Int, turnIndex: Int, fromIndex: Int, toIndex: Int) {
        val candidates = waves.getOrNull(waveIndex)?.turns?.getOrNull(turnIndex)?.candidates ?: return
        if (fromIndex !in candidates.indices || toIndex !in candidates.indices || fromIndex == toIndex) return
        val candidate = candidates.removeAt(fromIndex)
        candidates.add(toIndex, candidate)
        save()
    }

    fun updateCandidate(
        waveIndex: Int,
        turnIndex: Int,
        candidateIndex: Int,
        selection: CustomCardSelection
    ) {
        val candidates = waves.getOrNull(waveIndex)?.turns?.getOrNull(turnIndex)?.candidates ?: return
        if (candidateIndex in candidates.indices) {
            candidates[candidateIndex] = selection
            save()
        }
    }

    fun save() {
        val perTurn = CustomCardSelectionPerTurn.fromWaves(
            waves.map { wave ->
                CustomCardSelectionWave.from(
                    wave.turns.map { turn ->
                        CustomCardSelectionTurn.from(turn.candidates.toList())
                    }
                )
            }
        )
        battleConfig.customCardSelection.set(perTurn)
    }
}
