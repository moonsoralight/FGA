package io.github.fate_grand_automata.scripts.models

import io.github.fate_grand_automata.scripts.entrypoints.AutoBattle
import io.github.fate_grand_automata.scripts.enums.CardTypeEnum

class CustomCardSelection(
    private val cards: List<CustomCard>
) : List<CustomCard> by cards {
    override fun toString() = joinToString(separator = "")


    companion object {
        val empty = CustomCardSelection(emptyList())

        private const val customCardSelectionError = "Custom Card Selection Error at '"

        private fun raiseParseError(msg: String): Nothing {
            throw AutoBattle.BattleExitException(
                AutoBattle.ExitReason.CustomCardSelectionParseError(msg)
            )
        }

        fun of(selection: String): CustomCardSelection {
            val normalized = selection
                .filterNot { it.isWhitespace() || it == '+' }
                .uppercase()
            val cards = mutableListOf<CustomCard>()
            var index = 0

            fun fieldSlot(char: Char, token: String) = when (char) {
                '1' -> FieldSlot.A
                '2' -> FieldSlot.B
                '3' -> FieldSlot.C
                else -> raiseParseError("$customCardSelectionError$token': Invalid servant field slot.")
            }

            while (index < normalized.length) {
                val typeChar = normalized[index]
                val nextChar = normalized.getOrNull(index + 1)
                val slot = nextChar?.takeIf { it.isDigit() }?.let {
                    fieldSlot(it, "$typeChar$it")
                }

                val card = when (typeChar) {
                    'X' -> CustomCard.AnyFace(slot)

                    'B', 'A', 'Q' -> {
                        val type = when (typeChar) {
                            'B' -> CardTypeEnum.Buster
                            'A' -> CardTypeEnum.Arts
                            else -> CardTypeEnum.Quick
                        }
                        CustomCard.Face(type = type, fieldSlot = slot)
                    }

                    'N' -> {
                        val requiredSlot = slot
                            ?: raiseParseError("$customCardSelectionError$typeChar': NP must specify servant 1, 2, or 3.")
                        CustomCard.NoblePhantasm(requiredSlot)
                    }

                    else -> raiseParseError("$customCardSelectionError$typeChar': Invalid card type.")
                }

                cards += card
                index += if (slot == null) 1 else 2
            }

            if (cards.size > 3) {
                raiseParseError("$customCardSelectionError': Expected at most 3 cards selected per turn, but ${cards.size} found.")
            }
            return CustomCardSelection(cards)
        }
    }
}
