package io.github.fate_grand_automata.scripts.modules

import io.github.fate_grand_automata.scripts.IFgoAutomataApi
import io.github.fate_grand_automata.scripts.Images
import io.github.fate_grand_automata.scripts.entrypoints.AutoBattle
import io.github.fate_grand_automata.scripts.models.NPUsage
import io.github.fate_grand_automata.scripts.models.ParsedCard
import io.github.fate_grand_automata.scripts.models.Skill
import io.github.fate_grand_automata.scripts.models.battle.BattleState
import io.github.fate_grand_automata.scripts.prefs.IBattleConfig
import io.github.lib_automata.dagger.ScriptScope
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@ScriptScope
class Battle @Inject constructor(
    api: IFgoAutomataApi,
    private val servantTracker: ServantTracker,
    private val state: BattleState,
    private val battleConfig: IBattleConfig,
    private val autoSkill: AutoSkill,
    private val caster: Caster,
    private val card: Card,
    private val skillSpam: SkillSpam,
    private val shuffleChecker: ShuffleChecker,
    private val stageTracker: StageTracker,
    private val autoChooseTarget: AutoChooseTarget,
    private val hakunoShuffle: HakunoShuffle
) : IFgoAutomataApi by api {
    private var battleTailReached = false
    private var battleEndDialogueHandled = false
    private var preBattleDialogueEnabled = true

    init {
        prefs.stopAfterThisRun = false
        state.markStartTime()

        resetState()
    }

    fun resetState() {
        battleTailReached = false
        battleEndDialogueHandled = false
        // A new quest may have a pre-battle dialogue again. This is the only
        // place that re-enables the bronze-chest branch for the next battle.
        preBattleDialogueEnabled = true

        // Don't increment no. of runs if we're just clicking on quest again and again
        // This can happen due to lags introduced during some events
        if (state.stage != -1) {
            state.nextRun()

            servantTracker.nextRun()
        }

        if (prefs.stopAfterThisRun) {
            prefs.stopAfterThisRun = false
            throw AutoBattle.BattleExitException(AutoBattle.ExitReason.StopAfterThisRun)
        }

        if (prefs.selectedServerConfigPref.shouldLimitRuns && state.runs >= prefs.selectedServerConfigPref.limitRuns) {
            throw AutoBattle.BattleExitException(AutoBattle.ExitReason.LimitRuns(state.runs))
        }
    }

    fun isIdle() = images[Images.BattleScreen] in locations.battle.screenCheckRegion

    fun isPreBattleDialogue() =
        preBattleDialogueEnabled &&
                !isIdle() &&
                locations.battle.battleStartChestRegion.exists(images[Images.BattleStartChest])

    fun advancePreBattleDialogue() {
        // Perform exactly one safe-area click and return to AutoBattle's original
        // screen dispatcher. The next dispatcher cycle checks battle.png first;
        // once it appears, the chest branch stops immediately and performBattle()
        // starts through the unchanged original path.
        locations.battle.battleDialogueAdvanceRegion.center.click()
    }

    fun isPostBattleDialogue() =
        battleTailReached &&
                !battleEndDialogueHandled &&
                locations.battle.battleEndZeroEnemyRegion.exists(images[Images.BattleEndZeroEnemy])

    fun advancePostBattleDialogue() {
        battleEndDialogueHandled = true
        locations.battle.battleDialogueAdvanceRegion.center.clickWithInterval(
            times = prefs.battleEndClickCount,
            interval = 330.milliseconds
        )
    }

    fun clickAttack(): List<ParsedCard> {
        locations.battle.attackClick.click()

        // Wait for Attack button to disappear
        locations.battle.screenCheckRegion.waitVanish(images[Images.BattleScreen], 5.seconds)

        prefs.waitBeforeCards.wait()

        return card.readCommandCards()
    }

    fun performBattle() {
        // battle.png is the sole formal battle-entry marker. Once the original
        // battle path starts, the pre-battle dialogue branch stays disabled for
        // the rest of this quest and is re-enabled only by resetState().
        preBattleDialogueEnabled = false
        prefs.waitBeforeTurn.wait()

        onTurnStarted()

        if (battleConfig.addRaidTurnDelay){
            battleConfig.raidTurnDelaySeconds.seconds.wait()
        }

        servantTracker.beginTurn()

        val npUsage = autoSkill.execute(state.stage, state.turn)
        skillSpam.spamSkills()

        val cards = (hakunoShuffle.shuffleUntilMatched(
            readCards = ::clickAttack,
            closeAttack = { locations.attack.backClick.click() }
        ) ?: clickAttack())
            .takeUnless { shouldShuffle(it, npUsage) }
            ?: shuffleCards()

        card.clickCommandCards(cards, npUsage)

        if (autoSkill.isLastConfiguredTurn(state.stage, state.turn)) {
            battleTailReached = true
        }

        0.5.seconds.wait()
    }

    private fun shouldShuffle(cards: List<ParsedCard>, npUsage: NPUsage): Boolean {
        // Not this wave
        if (state.stage != (battleConfig.shuffleCardsWave - 1)) {
            return false
        }

        // Already shuffled
        if (state.shuffled) {
            return false
        }

        return shuffleChecker.shouldShuffle(
            mode = battleConfig.shuffleCards,
            cards = cards,
            npUsage = npUsage
        )
    }

    private fun shuffleCards(): List<ParsedCard> {
        locations.attack.backClick.click()

        caster.castMasterSkill(Skill.Master.C)
        state.shuffled = true

        return clickAttack()
    }

    private fun onTurnStarted() = useSameSnapIn {
        stageTracker.checkCurrentStage()

        state.nextTurn()

        if (battleConfig.autoChooseTarget) {
            autoChooseTarget.choose()
        }
    }
}
