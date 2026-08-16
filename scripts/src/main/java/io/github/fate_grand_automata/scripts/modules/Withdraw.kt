package io.github.fate_grand_automata.scripts.modules

import io.github.fate_grand_automata.scripts.IFgoAutomataApi
import io.github.fate_grand_automata.scripts.Images
import io.github.fate_grand_automata.scripts.entrypoints.AutoBattle
import io.github.lib_automata.dagger.ScriptScope
import io.github.fate_grand_automata.scripts.prefs.IBattleConfig
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@ScriptScope
class Withdraw @Inject constructor(
    api: IFgoAutomataApi,
    private val battleConfig: IBattleConfig
) : IFgoAutomataApi by api {
    var count = 0
        private set

    /**
     * Checks if the window for withdrawing from the battle exists.
     */
    fun needsToWithdraw() =
        images[Images.Withdraw] in locations.withdrawRegion

    /**
     * Handles withdrawing from battle. Depending on whether withdraw is enabled, the script either
     * withdraws automatically or stops completely.
     */
    fun withdraw() {
        if (battleConfig.masterSpam.reviveWithCommandSpells) {
            if (tryCommandSpellRevive()) return

            // Command-spell revival is explicitly selected as the defeat policy. If any step
            // cannot be verified, stop safely; never fall through into the withdrawal workflow.
            throw AutoBattle.BattleExitException(AutoBattle.ExitReason.CommandSpellReviveFailed)
        }

        if (!prefs.withdrawEnabled) {
            throw AutoBattle.BattleExitException(AutoBattle.ExitReason.WithdrawDisabled)
        }

        // Withdraw Region can vary depending on if you have Command Spells/Quartz
        val withdrawRegion = locations.withdrawRegion.find(images[Images.Withdraw])
            ?: return

        withdrawRegion.region.click()

        0.5.seconds.wait()

        // Click the "Accept" button after choosing to withdraw
        locations.withdrawAcceptClick.click()

        1.seconds.wait()

        // Click the "Close" button after accepting the withdrawal
        locations.withdrawCloseClick.click()

        ++count
    }

    /** Uses a fixed grayscale template for the enabled "Use Command Spells" button. */
    private fun tryCommandSpellRevive(): Boolean {
        if (images[Images.CommandSpellReviveEnabled] !in locations.commandSpellReviveRegion) {
            return false
        }

        locations.commandSpellReviveClick.click()

        // The confirmation animation must finish before the second tap.  Prefer the grayscale
        // match so different aspect ratios click the button itself; if that match fails, still
        // perform the required second tap at the calibrated right-hand "决定" position instead
        // of silently returning without ever confirming the revive.
        1.seconds.wait()
        val confirmPattern = images[Images.DreamFireConfirm]
        val confirmVisible = locations.commandSpellReviveConfirmRegion.exists(
            confirmPattern, 3.seconds, similarity = 0.62
        )
        val confirm = if (confirmVisible) {
            locations.commandSpellReviveConfirmRegion.find(
                confirmPattern, similarity = 0.62
            )
        } else null
        if (confirm != null) {
            confirm.region.click()
        } else {
            locations.commandSpellReviveConfirmClick.click()
        }

        5.seconds.wait()
        return !needsToWithdraw()
    }
}
