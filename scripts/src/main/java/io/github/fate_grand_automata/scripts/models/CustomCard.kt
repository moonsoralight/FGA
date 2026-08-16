package io.github.fate_grand_automata.scripts.models

import io.github.fate_grand_automata.scripts.enums.CardTypeEnum

sealed class CustomCard {
    /** Any still-unassigned non-NP command card. */
    data class AnyFace(
        val fieldSlot: FieldSlot? = null
    ) : CustomCard() {
        override fun toString() = "X${fieldSlot?.position ?: ""}"
    }

    data class Face(
        val type: CardTypeEnum,
        val fieldSlot: FieldSlot? = null
    ) : CustomCard() {
        override fun toString(): String {
            val typeChar = when (type) {
                CardTypeEnum.Buster -> 'B'
                CardTypeEnum.Arts -> 'A'
                CardTypeEnum.Quick -> 'Q'
                else -> '?'
            }
            return "$typeChar${fieldSlot?.position ?: ""}"
        }
    }

    data class NoblePhantasm(val fieldSlot: FieldSlot) : CustomCard() {
        val commandCard: CommandCard.NP
            get() = CommandCard.NP.list[fieldSlot.position - 1]

        override fun toString() = "N${fieldSlot.position}"
    }
}
