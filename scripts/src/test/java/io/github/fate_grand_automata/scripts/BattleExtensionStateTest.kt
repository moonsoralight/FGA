package io.github.fate_grand_automata.scripts

import io.github.fate_grand_automata.scripts.models.includesSpamWave
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BattleExtensionStateTest {
    @Test
    fun waveThreeSpamConfigurationContinuesIntoLaterWaves() {
        val configured = setOf(1, 3)

        assertTrue(configured.includesSpamWave(1))
        assertFalse(configured.includesSpamWave(2))
        assertTrue(configured.includesSpamWave(3))
        assertTrue(configured.includesSpamWave(4))
        assertTrue(configured.includesSpamWave(20))
    }
}
