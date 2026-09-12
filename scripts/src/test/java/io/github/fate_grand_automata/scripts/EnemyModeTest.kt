package io.github.fate_grand_automata.scripts

import io.github.fate_grand_automata.scripts.locations.ManualEnemyTargetLocations
import io.github.fate_grand_automata.scripts.models.AutoSkillAction
import io.github.fate_grand_automata.scripts.models.AutoSkillCommand
import io.github.fate_grand_automata.scripts.models.EnemyMode
import io.github.fate_grand_automata.scripts.models.EnemyTarget
import io.github.lib_automata.Location
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EnemyModeTest {
    @Test
    fun parsesAllSixTargetsWithoutChangingTheirCodes() {
        val actions = AutoSkillCommand.parse("t1t2t3t4t5t6").stages.single().single()
        assertEquals(('1'..'6').toList(), actions.map { (it as AutoSkillAction.TargetEnemy).enemy.autoSkillCode })
        assertEquals(listOf(EnemyTarget.A, EnemyTarget.B, EnemyTarget.C), EnemyTarget.autoChooseTargets)
    }

    @Test
    fun missingModeDefaultsToThree() {
        assertEquals(EnemyMode.Three, EnemyMode.fromCount(null))
        assertEquals(EnemyMode.Three, EnemyMode.fromCount(3))
        assertEquals(EnemyMode.Six, EnemyMode.fromCount(6))
    }

    @Test
    fun usesNineIndependentlyCalibratedPoints() {
        val three = listOf(Location(1245,87), Location(745,87), Location(244,87))
        val six = listOf(Location(1039,264), Location(639,264), Location(240,264),
            Location(1237,66), Location(836,66), Location(436,66))
        for ((mode, expected) in listOf(EnemyMode.Three to three, EnemyMode.Six to six)) {
            expected.forEachIndexed { index, location ->
                val enemy = EnemyTarget.list[index]
                assertEquals(location, ManualEnemyTargetLocations.locate(enemy, mode, true))
                assertEquals(location - Location(183,0), ManualEnemyTargetLocations.locate(enemy, mode, false))
            }
        }
        assertFailsWith<IllegalArgumentException> {
            ManualEnemyTargetLocations.locate(EnemyTarget.D, EnemyMode.Three, true)
        }
    }
}
