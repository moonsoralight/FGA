package io.github.fate_grand_automata.ui.custom_card_selection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.fate_grand_automata.R
import io.github.fate_grand_automata.scripts.enums.CardTypeEnum
import io.github.fate_grand_automata.scripts.models.CustomCard
import io.github.fate_grand_automata.scripts.models.CustomCardSelection
import io.github.fate_grand_automata.scripts.models.FieldSlot

@Composable
fun CustomCardSelectionTurnItem(
    selection: CustomCardSelection,
    onSelectionChange: (CustomCardSelection) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        CardPicker(
            onDismiss = { showPicker = false },
            onSelected = {
                val newList = selection.toList() + it
                onSelectionChange(CustomCardSelection(newList))
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            selection.forEachIndexed { index, card ->
                SelectedCardItem(
                    card = card,
                    onRemove = {
                        val newList = selection.toMutableList()
                        newList.removeAt(index)
                        onSelectionChange(CustomCardSelection(newList))
                    }
                )
            }
            if (selection.size < 3) {
                Surface(
                    tonalElevation = 5.dp,
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(50.dp),
                    onClick = { showPicker = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomCardSelectionCandidateList(
    candidates: List<CustomCardSelection>,
    onCandidateChange: (Int, CustomCardSelection) -> Unit,
    onAddCandidate: () -> Unit,
    onRemoveCandidate: (Int) -> Unit,
    onMoveCandidate: (Int, Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        candidates.forEachIndexed { index, selection ->
            var dragDistance by remember(index, candidates.size) { mutableStateOf(0f) }
            var draggedCandidateIndex by remember(index, candidates.size) {
                mutableIntStateOf(index)
            }
            var rowHeight by remember(index, candidates.size) { mutableFloatStateOf(1f) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { rowHeight = it.height.toFloat().coerceAtLeast(1f) }
                    .pointerInput(index, candidates.size) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                dragDistance = 0f
                                draggedCandidateIndex = index
                            },
                            onDragCancel = { dragDistance = 0f },
                            onDragEnd = { dragDistance = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragDistance += dragAmount.y

                                // Keep consuming every crossed row during the same long-press
                                // gesture. The old implementation captured the starting index,
                                // so it could perform only one adjacent move before the user had
                                // to release and long-press again.
                                while (
                                    dragDistance >= rowHeight &&
                                    draggedCandidateIndex < candidates.lastIndex
                                ) {
                                    onMoveCandidate(
                                        draggedCandidateIndex,
                                        draggedCandidateIndex + 1
                                    )
                                    draggedCandidateIndex++
                                    dragDistance -= rowHeight
                                }
                                while (
                                    dragDistance <= -rowHeight &&
                                    draggedCandidateIndex > 0
                                ) {
                                    onMoveCandidate(
                                        draggedCandidateIndex,
                                        draggedCandidateIndex - 1
                                    )
                                    draggedCandidateIndex--
                                    dragDistance += rowHeight
                                }
                            }
                        )
                    }
            ) {
                Text(
                    text = stringResource(R.string.p_custom_card_selection_candidate, index + 1),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 16.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    CustomCardSelectionTurnItem(
                        selection = selection,
                        onSelectionChange = { onCandidateChange(index, it) }
                    )
                }

                if (index > 0) {
                    IconButton(onClick = { onRemoveCandidate(index) }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.p_custom_card_selection_remove_candidate),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        TextButton(
            onClick = onAddCandidate,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(stringResource(R.string.p_custom_card_selection_add_candidate))
        }
    }
}

@Composable
private fun CardPicker(
    onDismiss: () -> Unit,
    onSelected: (CustomCard) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(0.98f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.p_custom_card_selection_card_picker_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val types = listOf(CardTypeEnum.Buster, CardTypeEnum.Arts, CardTypeEnum.Quick)
                val pickerGroups = listOf(
                    stringResource(R.string.p_custom_card_selection_any_servant) to
                            (listOf(CustomCard.AnyFace()) + types.map { CustomCard.Face(type = it) })
                ) + FieldSlot.list.map { slot ->
                    "S${slot.position}" to (
                            listOf(
                                CustomCard.NoblePhantasm(slot),
                                CustomCard.AnyFace(slot)
                            ) +
                                    types.map { CustomCard.Face(type = it, fieldSlot = slot) }
                            )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pickerGroups.forEach { (label, cards) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "$label：",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.width(
                                    if (cards.size == 4) 90.dp else 62.dp
                                )
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                cards.forEach { card ->
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = card.color,
                                        onClick = {
                                            onSelected(card)
                                            onDismiss()
                                        },
                                        modifier = Modifier.size(45.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                card.toString(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedCardItem(
    card: CustomCard,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Surface(
            tonalElevation = 5.dp,
            shape = MaterialTheme.shapes.medium,
            color = card.color,
            modifier = Modifier.size(50.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    card.toString(),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.TopEnd)
                .offset(x = 8.dp, y = (-8).dp)
                .clickable { onRemove() }
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                tint = Color.White,
                modifier = Modifier.padding(2.dp)
            )
        }
    }
}

private val CustomCard.color: Color
    @Composable get() {
        return when (this) {
            is CustomCard.NoblePhantasm -> Color(0xFFBCC7F8)
            is CustomCard.AnyFace -> Color(0xFFFFCB9B)
            is CustomCard.Face -> colorResource(
                when (type) {
                    CardTypeEnum.Buster -> R.color.colorBuster
                    CardTypeEnum.Arts -> R.color.colorArts
                    CardTypeEnum.Quick -> R.color.colorQuick
                    else -> R.color.colorAccent
                }
            )
        }
    }
