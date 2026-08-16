package io.github.fate_grand_automata.ui.custom_card_selection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.fate_grand_automata.R
import io.github.fate_grand_automata.ui.DimmedIcon
import io.github.fate_grand_automata.ui.Heading
import io.github.fate_grand_automata.ui.dialog.FgaDialog
import io.github.fate_grand_automata.ui.icon

@Composable
fun CustomCardSelectionView(
    vm: CustomCardSelectionViewModel
) {
    var selectedWave by remember { mutableIntStateOf(0) }
    var selectedTurn by remember { mutableIntStateOf(0) }
    val infoDialog = FgaDialog()

    LaunchedEffect(vm.waves.size) {
        selectedWave = selectedWave.coerceAtMost(vm.waves.lastIndex.coerceAtLeast(0))
    }
    val wave = vm.waves.getOrNull(selectedWave)
    LaunchedEffect(selectedWave, wave?.turns?.size) {
        selectedTurn = selectedTurn.coerceAtMost((wave?.turns?.lastIndex ?: 0).coerceAtLeast(0))
    }

    infoDialog.build {
        title(stringResource(R.string.p_custom_card_selection))
        message(stringResource(R.string.p_custom_card_selection_dialog_message))
        buttons(onSubmit = { }, showCancel = false)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Heading(text = stringResource(R.string.p_custom_card_selection))
                IconButton(onClick = { infoDialog.show() }) {
                    DimmedIcon(icon(Icons.Default.Info), contentDescription = "Info")
                }
            }

            CustomCardSelectionTurnSelector(
                itemCount = vm.waves.size,
                selectedIndex = selectedWave,
                label = "WAVE",
                onSelectedIndexChange = {
                    selectedWave = it
                    selectedTurn = 0
                },
                onAdd = { vm.addWave() },
                onCopy = {
                    vm.copyWave(it)
                    selectedWave = vm.waves.lastIndex
                    selectedTurn = 0
                },
                onRemove = {
                    val oldSelectedWave = selectedWave
                    vm.removeWave(it)
                    selectedWave = when {
                        it < oldSelectedWave -> oldSelectedWave - 1
                        it == oldSelectedWave -> it.coerceAtMost(vm.waves.lastIndex)
                        else -> oldSelectedWave.coerceAtMost(vm.waves.lastIndex)
                    }.coerceAtLeast(0)
                    if (it == oldSelectedWave) {
                        selectedTurn = 0
                    }
                }
            )

            HorizontalDivider()

            wave?.let { selectedWaveState ->
                CustomCardSelectionTurnSelector(
                    itemCount = selectedWaveState.turns.size,
                    selectedIndex = selectedTurn,
                    label = "TURN",
                    onSelectedIndexChange = { selectedTurn = it },
                    onAdd = { vm.addTurn(selectedWave) },
                    onCopy = {
                        vm.copyTurn(selectedWave, it)
                        selectedTurn = selectedWaveState.turns.lastIndex
                    },
                    onRemove = {
                        val oldSelectedTurn = selectedTurn
                        vm.removeTurn(selectedWave, it)
                        selectedTurn = when {
                            it < oldSelectedTurn -> oldSelectedTurn - 1
                            it == oldSelectedTurn -> it.coerceAtMost(selectedWaveState.turns.lastIndex)
                            else -> oldSelectedTurn.coerceAtMost(selectedWaveState.turns.lastIndex)
                        }.coerceAtLeast(0)
                    }
                )

                HorizontalDivider()

                selectedWaveState.turns.getOrNull(selectedTurn)?.let { turn ->
                    CustomCardSelectionCandidateList(
                        candidates = turn.candidates,
                        onCandidateChange = { candidateIndex, selection ->
                            vm.updateCandidate(selectedWave, selectedTurn, candidateIndex, selection)
                        },
                        onAddCandidate = { vm.addCandidate(selectedWave, selectedTurn) },
                        onRemoveCandidate = { candidateIndex ->
                            vm.removeCandidate(selectedWave, selectedTurn, candidateIndex)
                        },
                        onMoveCandidate = { fromIndex, toIndex ->
                            vm.moveCandidate(selectedWave, selectedTurn, fromIndex, toIndex)
                        }
                    )
                }
            }
        }
    }
}
