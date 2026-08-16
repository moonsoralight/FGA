package io.github.fate_grand_automata.scripts.modules

import io.github.fate_grand_automata.scripts.IFgoAutomataApi
import io.github.fate_grand_automata.scripts.ScriptLog
import io.github.fate_grand_automata.scripts.enums.BraveChainEnum
import io.github.fate_grand_automata.scripts.models.CommandCard
import io.github.fate_grand_automata.scripts.models.FieldSlot
import io.github.fate_grand_automata.scripts.models.NPUsage
import io.github.fate_grand_automata.scripts.models.ParsedCard
import io.github.fate_grand_automata.scripts.models.SpamConfigPerTeamSlot
import io.github.fate_grand_automata.scripts.models.includesSpamWave
import io.github.fate_grand_automata.scripts.models.battle.BattleState
import io.github.fate_grand_automata.scripts.prefs.IBattleConfig
import io.github.lib_automata.dagger.ScriptScope
import javax.inject.Inject

@ScriptScope
class Card @Inject constructor(
    api: IFgoAutomataApi,
    private val servantTracker: ServantTracker,
    private val state: BattleState,
    private val spamConfig: SpamConfigPerTeamSlot,
    private val caster: Caster,
    private val parser: CardParser,
    private val priority: FaceCardPriority,
    private val braveChains: ApplyBraveChains,
    private val battleConfig: IBattleConfig,
    private val selection: ApplyCustomCardSelection
) : IFgoAutomataApi by api {

    fun readCommandCards(): List<ParsedCard> = useSameSnapIn {
        parser.parse()
    }

    private val spamNps: Set<CommandCard.NP>
        get() =
            (FieldSlot.list.zip(CommandCard.NP.list))
                .mapNotNull { (servantSlot, np) ->
                    val teamSlot = servantTracker.deployed[servantSlot] ?: return@mapNotNull null
                    val npSpamConfig = spamConfig[teamSlot].np

                    if (npSpamConfig.waves.includesSpamWave(state.stage + 1) &&
                        caster.canSpam(npSpamConfig.spam)
                    )
                        np
                    else null
                }
                .toSet()

    private fun pickCardsByPriority(
        cards: List<ParsedCard>,
        npUsage: NPUsage
    ): List<ParsedCard> {
        val cardsOrderedByPriority = priority.sort(cards, state.stage)

        fun <T> List<T>.inCurrentWave(default: T) =
            if (isNotEmpty())
                this[state.stage.coerceIn(indices)]
            else default

        val braveChainsPerWave = battleConfig.braveChains
        val rearrangeCardsPerWave = battleConfig.rearrangeCards

        return braveChains.pick(
            cards = cardsOrderedByPriority,
            npUsage = npUsage,
            braveChains = braveChainsPerWave.inCurrentWave(BraveChainEnum.None),
            rearrange = rearrangeCardsPerWave.inCurrentWave(false)
        )
    }

    private fun pickFaceCards(
        cards: List<ParsedCard>,
        npUsage: NPUsage,
        customMatch: ApplyCustomCardSelection.Match?
    ): List<CommandCard.Face> {
        if (customMatch == null) {
            return pickCardsByPriority(cards, npUsage).map { it.card }
        }

        val customFaces = customMatch.commands.filterIsInstance<CommandCard.Face>()
        val remaining = pickCardsByPriority(customMatch.remainingCards, npUsage)
            .map { it.card }
        return customFaces + remaining
    }

    private fun clickCustomCommands(match: ApplyCustomCardSelection.Match) {
        // Every NP in the match was verified on the current attack screen before resolving the
        // candidate. Every one of the remaining five dealt face cards is still attempted exactly
        // once; once FGO has accepted three attacks it ignores the remaining taps naturally.
        val remainingFaces = pickCardsByPriority(match.remainingCards, NPUsage.none)
            .map { it.card }

        (match.commands + remainingFaces).forEach { command ->
            when (command) {
                is CommandCard.Face -> {
                    messages.log(ScriptLog.ClickingCards(listOf(command)))
                    caster.use(command)
                }

                is CommandCard.NP -> {
                    messages.log(ScriptLog.ClickingNPs(listOf(command)))
                    caster.use(command)
                }
            }
        }
    }

    fun clickCommandCards(
        cards: List<ParsedCard>,
        npUsage: NPUsage
    ) {
        val availableNps = useSameSnapIn { servantTracker.availableNps() }
        val customMatch = selection.pick(
            // The matcher removes every resolved physical card from its remaining pool, so X/Xn
            // can never reuse a card claimed by another requirement.  Sorting first makes Xn
            // choose the configured colour priority among cards of that same servant.
            cards = priority.sort(cards, state.stage),
            wave = state.stage + 1,
            turn = state.turn + 1,
            availableNps = availableNps
        )
        if (customMatch != null) {
            clickCustomCommands(customMatch)
            return
        }

        val pickedCards = pickFaceCards(cards, npUsage, customMatch)
            .take(3)

        if (npUsage.cardsBeforeNP > 0) {
            pickedCards
                .take(npUsage.cardsBeforeNP)
                .also { messages.log(ScriptLog.ClickingCards(it)) }
                .forEach { caster.use(it) }
        }

        val nps = npUsage.nps + spamNps

        if (nps.isNotEmpty()) {
            nps
                .also { messages.log(ScriptLog.ClickingNPs(it)) }
                .forEach { caster.use(it) }
        }

        pickedCards
            .drop(npUsage.cardsBeforeNP)
            .also { messages.log(ScriptLog.ClickingCards(it)) }
            .forEach { caster.use(it) }
    }

    fun matchesCustomStrategy(cards: List<ParsedCard>, wave: Int, turn: Int): Boolean =
        selection.pick(
            cards = priority.sort(cards, state.stage),
            wave = wave,
            turn = turn,
            availableNps = useSameSnapIn { servantTracker.availableNps() }
        ) != null

}
