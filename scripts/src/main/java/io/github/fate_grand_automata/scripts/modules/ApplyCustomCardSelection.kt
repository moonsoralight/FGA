package io.github.fate_grand_automata.scripts.modules

import io.github.fate_grand_automata.scripts.models.CustomCardSelectionPerTurn
import io.github.fate_grand_automata.scripts.models.CustomCard
import io.github.fate_grand_automata.scripts.models.CustomCardSelection
import io.github.fate_grand_automata.scripts.models.CommandCard
import io.github.fate_grand_automata.scripts.models.ParsedCard
import io.github.lib_automata.dagger.ScriptScope
import javax.inject.Inject

@ScriptScope
class ApplyCustomCardSelection @Inject constructor(
    private val selection: CustomCardSelectionPerTurn
) {
    data class Match(
        val commands: List<CommandCard>,
        val remainingCards: List<ParsedCard>,
        val candidateIndex: Int
    )

    /**
     * Resolves the configured commands in order without reusing a face card.
     * Returns null when no custom selection exists or a required face card is unavailable,
     * allowing the caller to fall back to the existing priority picker.
     */
    fun pick(
        cards: List<ParsedCard>,
        wave: Int,
        turn: Int,
        availableNps: Set<CommandCard.NP> = CommandCard.NP.list.toSet()
    ): Match? {
        return selection.at(wave, turn)
            .withIndex()
            .firstNotNullOfOrNull { (candidateIndex, requirements) ->
                if (requirements.isEmpty()) return@firstNotNullOfOrNull null

                val resolved = resolve(
                    requirements = requirements,
                    requirementIndex = 0,
                    availableCards = cards,
                    availableNps = availableNps,
                    picked = emptyList()
                ) ?: return@firstNotNullOfOrNull null

                Match(
                    commands = resolved.first,
                    remainingCards = resolved.second,
                    candidateIndex = candidateIndex
                )
            }
    }

    /** Legacy single-wave overload. */
    fun pick(cards: List<ParsedCard>, turn: Int): Match? = pick(cards, wave = 1, turn = turn)

    private fun resolve(
        requirements: CustomCardSelection,
        requirementIndex: Int,
        availableCards: List<ParsedCard>,
        availableNps: Set<CommandCard.NP>,
        picked: List<CommandCard>
    ): Pair<List<CommandCard>, List<ParsedCard>>? {
        if (requirementIndex >= requirements.size) return picked to availableCards

        return when (val requirement = requirements[requirementIndex]) {
            is CustomCard.NoblePhantasm -> {
                val np = requirement.commandCard
                if (np !in availableNps) null
                else resolve(
                    requirements,
                    requirementIndex + 1,
                    availableCards,
                    availableNps - np,
                    picked + np
                )
            }

            is CustomCard.Face -> availableCards
                .withIndex()
                .filter { (_, card) ->
                    card.type == requirement.type &&
                            (requirement.fieldSlot == null || card.fieldSlot == requirement.fieldSlot)
                }
                .firstNotNullOfOrNull { (cardIndex, card) ->
                    resolve(
                        requirements,
                        requirementIndex + 1,
                        availableCards.filterIndexed { index, _ -> index != cardIndex },
                        availableNps,
                        picked + card.card
                    )
                }

            is CustomCard.AnyFace -> availableCards
                .withIndex()
                .filter { (_, card) ->
                    requirement.fieldSlot == null || card.fieldSlot == requirement.fieldSlot
                }
                .firstNotNullOfOrNull { (cardIndex, card) ->
                    resolve(
                        requirements,
                        requirementIndex + 1,
                        availableCards.filterIndexed { index, _ -> index != cardIndex },
                        availableNps,
                        picked + card.card
                    )
                }
        }
    }
}
