package io.github.fate_grand_automata.scripts.models

/** Ordered alternative combinations for one turn. */
class CustomCardSelectionTurn(
    private val candidates: List<CustomCardSelection>
) : List<CustomCardSelection> by candidates {
    override fun toString() = candidates.joinToString(CANDIDATE_SEPARATOR)

    companion object {
        internal const val CANDIDATE_SEPARATOR = "|"
        val empty = CustomCardSelectionTurn(listOf(CustomCardSelection.empty))

        fun from(candidates: List<CustomCardSelection>) =
            CustomCardSelectionTurn(candidates.ifEmpty { listOf(CustomCardSelection.empty) })
    }
}

/** Turn rules for one wave. */
class CustomCardSelectionWave(
    private val turns: List<CustomCardSelectionTurn>
) : List<CustomCardSelectionTurn> by turns {
    fun atTurn(turn: Int): CustomCardSelectionTurn =
        turns[(turn - 1).coerceAtLeast(0).coerceAtMost(turns.lastIndex)]

    override fun toString() = turns.joinToString(TURN_SEPARATOR)

    companion object {
        internal const val TURN_SEPARATOR = ","
        val empty = CustomCardSelectionWave(listOf(CustomCardSelectionTurn.empty))

        fun from(turns: List<CustomCardSelectionTurn>) =
            CustomCardSelectionWave(turns.ifEmpty { listOf(CustomCardSelectionTurn.empty) })
    }
}

/**
 * Wave -> turn -> ordered candidate combinations.
 *
 * Waves and turns beyond the configured range inherit the last configured entry. The legacy
 * comma-separated turn format remains readable as a single-wave configuration.
 */
class CustomCardSelectionPerTurn private constructor(
    private val waves: List<CustomCardSelectionWave>
) : List<CustomCardSelectionWave> by waves {
    fun at(wave: Int, turn: Int): CustomCardSelectionTurn {
        val selectedWave = waves[(wave - 1).coerceAtLeast(0).coerceAtMost(waves.lastIndex)]
        return selectedWave.atTurn(turn)
    }

    /** Legacy single-wave accessor retained for integrations and imported configs. */
    fun atTurn(turn: Int): CustomCardSelection =
        at(1, turn).firstOrNull() ?: CustomCardSelection.empty

    override fun toString() = waves.joinToString(WAVE_SEPARATOR)

    companion object {
        private const val WAVE_SEPARATOR = "#"

        val empty = CustomCardSelectionPerTurn(listOf(CustomCardSelectionWave.empty))

        /** Legacy builder: one wave, one candidate per turn. */
        fun from(selectionsPerTurn: List<CustomCardSelection>) = fromWaves(
            listOf(
                CustomCardSelectionWave.from(
                    selectionsPerTurn.map { CustomCardSelectionTurn.from(listOf(it)) }
                )
            )
        )

        fun fromWaves(waves: List<CustomCardSelectionWave>) =
            CustomCardSelectionPerTurn(waves.ifEmpty { listOf(CustomCardSelectionWave.empty) })

        fun of(selection: String): CustomCardSelectionPerTurn {
            if (selection.isBlank()) return empty

            val parsedWaves = selection.split(WAVE_SEPARATOR).map { rawWave ->
                CustomCardSelectionWave.from(
                    rawWave.split(CustomCardSelectionWave.TURN_SEPARATOR).map { rawTurn ->
                        CustomCardSelectionTurn.from(
                            rawTurn.split(CustomCardSelectionTurn.CANDIDATE_SEPARATOR)
                                .map { CustomCardSelection.of(it) }
                        )
                    }
                )
            }
            return fromWaves(parsedWaves)
        }
    }
}
