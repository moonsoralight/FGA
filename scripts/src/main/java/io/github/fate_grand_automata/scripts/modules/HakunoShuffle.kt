package io.github.fate_grand_automata.scripts.modules

import io.github.fate_grand_automata.scripts.IFgoAutomataApi
import io.github.fate_grand_automata.scripts.Images
import io.github.fate_grand_automata.scripts.models.FieldSlot
import io.github.fate_grand_automata.scripts.models.ParsedCard
import io.github.fate_grand_automata.scripts.models.skills
import io.github.fate_grand_automata.scripts.models.battle.BattleState
import io.github.fate_grand_automata.scripts.prefs.IBattleConfig
import io.github.lib_automata.dagger.ScriptScope
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Hakuno's shuffle is deliberately run only after configured skills and immediately before
 * command-card selection. All recognition uses small, fixed grayscale template regions.
 */
@ScriptScope
class HakunoShuffle @Inject constructor(
    api: IFgoAutomataApi,
    private val battleConfig: IBattleConfig,
    private val state: BattleState,
    private val servantTracker: ServantTracker,
    private val caster: Caster,
    private val customSelection: ApplyCustomCardSelection
) : IFgoAutomataApi by api {
    private fun isEnabled(): Boolean =
        battleConfig.hakunoShuffleEnabled &&
            (state.stage + 1) in battleConfig.hakunoShuffleWaves &&
            battleConfig.customCardSelection.at(state.stage + 1, state.turn + 1)
                .any { it.isNotEmpty() }

    fun shuffleUntilMatched(
        readCards: () -> List<ParsedCard>,
        closeAttack: () -> Unit
    ): List<ParsedCard>? {
        if (!isEnabled()) return null

        // Read the hand first. A valid hand must never depend on successful name recognition.
        var cards = readCards()
        if (matches(cards)) return cards

        val hakunoSlot = findHakunoSlot()
        if (hakunoSlot == null) {
            closeAttack()
            return null
        }

        while (true) {
            closeAttack()
            if (!canUseSkillOne(hakunoSlot)) return null

            caster.castServantSkill(hakunoSlot.skills().first(), null)
            if (closeEnergyInsufficientPopup()) return null

            250.milliseconds.wait()
            cards = readCards()
            if (matches(cards)) return cards
        }
    }

    private fun matches(cards: List<ParsedCard>) = customSelection.pick(
        cards = cards,
        wave = state.stage + 1,
        turn = state.turn + 1,
        availableNps = useSameSnapIn { servantTracker.availableNps() }
    ) != null

    private fun findHakunoSlot(): FieldSlot? {
        if (!battleConfig.hakunoShuffleAutoDetect) {
            return FieldSlot.list.getOrNull(
                battleConfig.hakunoShuffleManualSlot.coerceIn(1, 3) - 1
            )
        }

        val template = images[Images.HakunoName]
        return FieldSlot.list.firstOrNull { slot ->
            slot in servantTracker.deployed &&
                locations.battle.servantNameRegion(slot).find(template, similarity = 0.70) != null
        }
    }

    private fun canUseSkillOne(slot: FieldSlot): Boolean {
        val teamSlot = servantTracker.deployed[slot] ?: return false
        val skill = slot.skills().first()
        val skillImage = servantTracker.checkImages[teamSlot]?.skills?.firstOrNull() ?: return false
        return skillImage in locations.battle.imageRegion(skill)
    }

    private fun closeEnergyInsufficientPopup(): Boolean {
        val detected = locations.battle.energyInsufficientTextRegion.find(
            images[Images.HakunoEnergyInsufficient], similarity = 0.70
        ) ?: return false

        // The phrase proves which popup this is; the button is independently localized.
        locations.battle.energyInsufficientCloseRegion.find(
            images[Images.HakunoEnergyClose], similarity = 0.68
        )?.region?.click() ?: locations.battle.energyInsufficientCloseRegion.click()
        return true
    }
}
