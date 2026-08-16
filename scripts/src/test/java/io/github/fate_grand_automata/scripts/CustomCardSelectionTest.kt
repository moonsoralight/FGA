package io.github.fate_grand_automata.scripts

import io.github.fate_grand_automata.scripts.entrypoints.AutoBattle
import io.github.fate_grand_automata.scripts.enums.CardTypeEnum
import io.github.fate_grand_automata.scripts.models.CommandCard
import io.github.fate_grand_automata.scripts.models.CustomCard
import io.github.fate_grand_automata.scripts.models.CustomCardSelection
import io.github.fate_grand_automata.scripts.models.CustomCardSelectionPerTurn
import io.github.fate_grand_automata.scripts.models.CustomCardSelectionTurn
import io.github.fate_grand_automata.scripts.models.CustomCardSelectionWave
import io.github.fate_grand_automata.scripts.models.FieldSlot
import io.github.fate_grand_automata.scripts.models.ParsedCard
import io.github.fate_grand_automata.scripts.models.TeamSlot
import io.github.fate_grand_automata.scripts.modules.ApplyCustomCardSelection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CustomCardSelectionTest {
    private val cards = listOf(
        ParsedCard(CommandCard.Face.A, TeamSlot.A, FieldSlot.A, CardTypeEnum.Buster),
        ParsedCard(CommandCard.Face.B, TeamSlot.A, FieldSlot.A, CardTypeEnum.Arts),
        ParsedCard(CommandCard.Face.C, TeamSlot.B, FieldSlot.B, CardTypeEnum.Arts),
        ParsedCard(CommandCard.Face.D, TeamSlot.C, FieldSlot.C, CardTypeEnum.Quick),
        ParsedCard(CommandCard.Face.E, TeamSlot.B, FieldSlot.B, CardTypeEnum.Buster)
    )

    @Test
    fun parsesNpGenericAndSpecificCards() {
        val selection = CustomCardSelection.of("N1 + A1 + Q")

        assertEquals(
            listOf(
                CustomCard.NoblePhantasm(FieldSlot.A),
                CustomCard.Face(CardTypeEnum.Arts, FieldSlot.A),
                CustomCard.Face(CardTypeEnum.Quick)
            ),
            selection.toList()
        )
        assertEquals("N1A1Q", selection.toString())
    }

    @Test
    fun parsesAnyUnassignedFaceCard() {
        val selection = CustomCardSelection.of("N1+X+N2")

        assertEquals(
            listOf(
                CustomCard.NoblePhantasm(FieldSlot.A),
                CustomCard.AnyFace(),
                CustomCard.NoblePhantasm(FieldSlot.B)
            ),
            selection.toList()
        )

        val servantSpecific = CustomCardSelection.of("X1X2X3")
        assertEquals(
            listOf(
                CustomCard.AnyFace(FieldSlot.A),
                CustomCard.AnyFace(FieldSlot.B),
                CustomCard.AnyFace(FieldSlot.C)
            ),
            servantSpecific.toList()
        )
    }

    @Test
    fun keepsPr2196SyntaxCompatible() {
        val selection = CustomCardSelection.of("B1A2Q3")

        assertEquals(
            listOf(
                CustomCard.Face(CardTypeEnum.Buster, FieldSlot.A),
                CustomCard.Face(CardTypeEnum.Arts, FieldSlot.B),
                CustomCard.Face(CardTypeEnum.Quick, FieldSlot.C)
            ),
            selection.toList()
        )
    }

    @Test
    fun rejectsInvalidNpAndMoreThanThreeCommands() {
        assertFailsWith<AutoBattle.BattleExitException> { CustomCardSelection.of("N") }
        assertFailsWith<AutoBattle.BattleExitException> { CustomCardSelection.of("N4") }
        assertFailsWith<AutoBattle.BattleExitException> { CustomCardSelection.of("N1ABQ") }
    }

    @Test
    fun matchesAnyColorAndSpecificServantInOrder() {
        val matcher = matcher("N1+A1+Q")

        val match = matcher.pick(cards, turn = 1)!!

        assertEquals(
            listOf(CommandCard.NP.A, CommandCard.Face.B, CommandCard.Face.D),
            match.commands
        )
        assertEquals(listOf(CommandCard.Face.A, CommandCard.Face.C, CommandCard.Face.E), match.remainingCards.map { it.card })
    }

    @Test
    fun consumesDifferentCardsForRepeatedGenericRequirements() {
        val matcher = matcher("AA")

        val match = matcher.pick(cards, turn = 1)!!

        assertEquals(listOf(CommandCard.Face.B, CommandCard.Face.C), match.commands)
    }

    @Test
    fun anyCardNeverReusesCardsReservedByLaterSpecificRequirements() {
        val matcher = matcher("X+A1+B1")

        val match = matcher.pick(cards, turn = 1)!!

        assertEquals(
            listOf(CommandCard.Face.C, CommandCard.Face.B, CommandCard.Face.A),
            match.commands
        )
        assertEquals(listOf(CommandCard.Face.D, CommandCard.Face.E), match.remainingCards.map { it.card })
    }

    @Test
    fun servantSpecificAnyCardCannotReuseLaterSpecificCard() {
        val matcher = matcher("N1+X1+A1")

        val match = matcher.pick(cards, turn = 1)!!

        assertEquals(
            listOf(CommandCard.NP.A, CommandCard.Face.A, CommandCard.Face.B),
            match.commands
        )
        assertEquals(
            listOf(CommandCard.Face.C, CommandCard.Face.D, CommandCard.Face.E),
            match.remainingCards.map { it.card }
        )
    }

    @Test
    fun triesCandidatesTopToBottomAndInheritsLastWaveAndTurn() {
        val config = CustomCardSelectionPerTurn.fromWaves(
            listOf(
                CustomCardSelectionWave.from(
                    listOf(
                        CustomCardSelectionTurn.from(
                            listOf(
                                CustomCardSelection.of("Q1Q1Q1"),
                                CustomCardSelection.of("A1B1X")
                            )
                        )
                    )
                )
            )
        )
        val matcher = ApplyCustomCardSelection(config)

        val match = matcher.pick(cards, wave = 4, turn = 9)!!

        assertEquals(1, match.candidateIndex)
        assertEquals(
            listOf(CommandCard.Face.B, CommandCard.Face.A, CommandCard.Face.C),
            match.commands
        )
    }

    @Test
    fun serializationRoundTripsWaveTurnAndCandidates() {
        val raw = "N1XA|N1A1Q#B2Q2A2,B3Q3A3"
        val parsed = CustomCardSelectionPerTurn.of(raw)

        assertEquals(raw, parsed.toString())
        assertEquals("N1XA", parsed.at(1, 1)[0].toString())
        assertEquals("N1A1Q", parsed.at(1, 7)[1].toString())
        assertEquals("B3Q3A3", parsed.at(9, 2)[0].toString())
    }

    @Test
    fun returnsNullSoExistingPriorityCanHandleIncompleteCombination() {
        val matcher = matcher("N1+Q1+A")

        assertNull(matcher.pick(cards, turn = 1))
    }

    private fun matcher(raw: String) = ApplyCustomCardSelection(
        CustomCardSelectionPerTurn.from(listOf(CustomCardSelection.of(raw)))
    )
}
