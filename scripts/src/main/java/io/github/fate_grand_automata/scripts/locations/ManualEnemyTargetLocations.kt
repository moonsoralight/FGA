package io.github.fate_grand_automata.scripts.locations

import io.github.fate_grand_automata.scripts.models.EnemyMode
import io.github.fate_grand_automata.scripts.models.EnemyTarget
import io.github.lib_automata.Location

/** Manual skill-sequence targets only; automatic targeting keeps its original locations. */
object ManualEnemyTargetLocations {
    // Centres measured on the user's 2736x1264 references (2026-09-10).
    // Both screenshots use the existing height-based script transform: 1264 -> 1440.
    // Numbering is right-to-left; the Six upper row is independently measured.
    private val three = listOf(Location(1093, 76), Location(654, 76), Location(214, 76))
    private val six = listOf(
        Location(912, 232), Location(561, 232), Location(211, 232),
        Location(1086, 58), Location(734, 58), Location(383, 58)
    )

    fun locate(enemy: EnemyTarget, mode: EnemyMode, isWide: Boolean): Location {
        val index = enemy.autoSkillCode.digitToInt() - 1
        require(index in 0 until mode.count) {
            "Enemy ${enemy.autoSkillCode} is not available in ${mode.count}-enemy mode"
        }
        val reference = (if (mode == EnemyMode.Six) six else three)[index]
        val scriptPoint = reference * (1440.0 / 1264.0)
        // Preserve the existing enemy-location wide/non-wide horizontal adjustment.
        // Screen scaling and game-area/notch offset remain the original click pipeline.
        return scriptPoint - Location(if (isWide) 0 else 183, 0)
    }
}
