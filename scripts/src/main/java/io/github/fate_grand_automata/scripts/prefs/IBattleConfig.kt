package io.github.fate_grand_automata.scripts.prefs

import io.github.fate_grand_automata.scripts.enums.BraveChainEnum
import io.github.fate_grand_automata.scripts.enums.GameServer
import io.github.fate_grand_automata.scripts.enums.MaterialEnum
import io.github.fate_grand_automata.scripts.enums.ShuffleCardsEnum
import io.github.fate_grand_automata.scripts.models.CardPriorityPerWave
import io.github.fate_grand_automata.scripts.models.ServantPriorityPerWave
import io.github.fate_grand_automata.scripts.models.ServantSpamConfig
import io.github.fate_grand_automata.scripts.models.CustomCardSelectionPerTurn
import io.github.fate_grand_automata.scripts.models.MasterSpamConfig
import io.github.fate_grand_automata.scripts.models.EnemyMode

interface IBattleConfig {
    val id: String
    var name: String
    var skillCommand: String
    // Zero-based Wave order. An unconfigured Wave uses Three, not the last mode.
    var enemyModes: List<EnemyMode>
    var cardPriority: CardPriorityPerWave
    val useServantPriority: Boolean
    val servantPriority: ServantPriorityPerWave
    val rearrangeCards: List<Boolean>
    val braveChains: List<BraveChainEnum>
    val party: Int
    val materials: Set<MaterialEnum>
    val support: ISupportPreferences
    val shuffleCards: ShuffleCardsEnum
    val shuffleCardsWave: Int
    val hakunoShuffleEnabled: Boolean
    val hakunoShuffleAutoDetect: Boolean
    val hakunoShuffleManualSlot: Int
    val hakunoShuffleWaves: Set<Int>

    var spam: List<ServantSpamConfig>
    var masterSpam: MasterSpamConfig
    val autoChooseTarget: Boolean

    val server: GameServer?

    val addRaidTurnDelay: Boolean
    val raidTurnDelaySeconds : Int

    val customCardSelection: CustomCardSelectionPerTurn
    fun export(): Map<String, *>

    fun import(map: Map<String, *>)
}
