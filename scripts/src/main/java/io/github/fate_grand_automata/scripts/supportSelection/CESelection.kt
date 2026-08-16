package io.github.fate_grand_automata.scripts.supportSelection

import io.github.fate_grand_automata.SupportImageKind
import io.github.fate_grand_automata.scripts.IFgoAutomataApi
import io.github.fate_grand_automata.scripts.Images
import io.github.fate_grand_automata.scripts.enums.BondCEEffectEnum
import io.github.fate_grand_automata.scripts.enums.GameServer
import io.github.fate_grand_automata.scripts.prefs.ISupportPreferences
import io.github.lib_automata.Region
import io.github.lib_automata.dagger.ScriptScope
import javax.inject.Inject

@ScriptScope
class CESelection @Inject constructor(
    api: IFgoAutomataApi,
    private val supportPrefs: ISupportPreferences,
    private val starChecker: SupportSelectionStarChecker,
    private val grandChecker: SupportSelectionGrandChecker
) : IFgoAutomataApi by api {
    fun check(ces: List<String>, bounds: SupportBounds): Boolean {
        // TODO: Only check the lower part (excluding Servant)
        val searchRegion = bounds.region.clip(locations.support.listRegion)
        val grandSearchRegion = bounds.region.clip(locations.support.grandCeListRegion)
        val cnGrandLabelSearchRegion = bounds.region.clip(locations.support.cnGrandCeLabelListRegion)

        if (ces.isEmpty()) {
            // servant must not have blank ce
            return !searchRegion.exists(images[Images.SupportBlankCE])
        }

        if (isGrandServant(grandSearchRegion, cnGrandLabelSearchRegion)) {
            val matched = ces
                .flatMap { entry -> images.loadSupportPattern(SupportImageKind.CE, entry) }
                .mapNotNull {
                    grandSearchRegion.find(it)
                }
                .filter {
                    !supportPrefs.mlb || isGrandLimitBroken(it.region)
                }

            val grandCeRegion1 = locations.support.grandCeRegion1.copy(y = searchRegion.y + locations.support.grandCeRegion1.y)
            val grandCeRegion3 = locations.support.grandCeRegion3.copy(y = searchRegion.y + locations.support.grandCeRegion3.y)
            val bondRegion = locations.support.bondCeRegion.copy(y = searchRegion.y + locations.support.bondCeRegion.y)

            val normalMatch = matched.any { grandCeRegion1.contains(it.region) }
            val rewardMatch = matched.any { grandCeRegion3.contains(it.region) }

            val ceSlotMatch = if (supportPrefs.requireBothNormalAndRewardMatch) {
                normalMatch && rewardMatch
            } else {
                normalMatch || rewardMatch
            }

            val bondEffectMatch = if (prefs.gameServer == GameServer.Cn) {
                checkCnBondCeEffect(searchRegion.y, bondRegion)
            } else {
                checkOriginalBondCeEffect(bondRegion)
            }

            return ceSlotMatch && bondEffectMatch
        } else {
            val matched = ces
                .flatMap { entry -> images.loadSupportPattern(SupportImageKind.CE, entry) }
                .mapNotNull {
                    searchRegion.find(it)
                }
                .filter {
                    !supportPrefs.mlb || isLimitBroken(it.region)
                }
            return matched.isNotEmpty()
                && !supportPrefs.requireBothNormalAndRewardMatch
                && supportPrefs.bondCEEffect == BondCEEffectEnum.Ignore
        }
    }

    private fun isLimitBroken(craftEssence: Region): Boolean {
        val limitBreakRegion = locations.support.limitBreakRegion
            .copy(y = craftEssence.y)

        return starChecker.isStarPresent(limitBreakRegion)
    }

    private fun isGrandServant(
        ces: Region,
        cnLabelSearchRegion: Region
    ): Boolean {
        if (prefs.gameServer == GameServer.Cn) {
            return grandChecker.isGrandPresent(
                cnLabelSearchRegion,
                Images.GrandCeLabelCn
            )
        }

        val multiCeLabelRegion = locations.support.grandCeLabelRegion
            .copy(y = ces.y + 40)

        return grandChecker.isGrandPresent(multiCeLabelRegion)
    }

    private fun isGrandLimitBroken(craftEssence: Region): Boolean {
        val limitBreakRegion = locations.support.grandLimitBreakRegion
            .copy(y = craftEssence.y)

        return starChecker.isStarPresent(limitBreakRegion)
    }

    /**
     * CN Grand-support bond-CE classification:
     *
     * 1. Match the dedicated empty-slot template inside the complete second CE
     *    slot. A match means that no bond CE is equipped, so neither effect can
     *    satisfy the preference.
     * 2. Only when the slot is not empty, inspect the existing small effect-icon
     *    region. The stable original-effect icon means Default; otherwise the
     *    equipped bond CE is the only other CN variant, NP 50%.
     *
     * This deliberately does not use the MLB marker: the original tiny MLB
     * template is a secondary check after a concrete CE has already matched and
     * produces false positives when used alone on an empty slot.
     */
    private fun checkCnBondCeEffect(
        supportRowY: Int,
        bondEffectRegion: Region
    ): Boolean {
        if (supportPrefs.bondCEEffect == BondCEEffectEnum.Ignore) {
            return true
        }

        val secondSlotRegion = locations.support.grandCeRegion2
            .copy(y = supportRowY + locations.support.grandCeRegion2.y)
        val isEmpty = secondSlotRegion.find(images[Images.BondCeEmptyCn]) != null

        if (isEmpty) {
            return false
        }

        val hasDefaultEffect = bondEffectRegion.find(
            images[Images.BondCeEffectDefaultCn]
        ) != null

        return when (supportPrefs.bondCEEffect) {
            BondCEEffectEnum.Default -> hasDefaultEffect
            BondCEEffectEnum.NP -> !hasDefaultEffect
            BondCEEffectEnum.Ignore -> true
        }
    }

    private fun checkOriginalBondCeEffect(bondEffectRegion: Region): Boolean =
        when (supportPrefs.bondCEEffect) {
            BondCEEffectEnum.Default ->
                bondEffectRegion.find(images[Images.BondCeEffectDefault]) != null

            BondCEEffectEnum.NP ->
                bondEffectRegion.find(images[Images.BondCeEffectNP]) != null

            BondCEEffectEnum.Ignore -> true
        }

}
