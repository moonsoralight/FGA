package io.github.fate_grand_automata.ui.more

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import io.github.fate_grand_automata.R
import io.github.fate_grand_automata.prefs.core.PrefsCore
import io.github.fate_grand_automata.scripts.enums.GameServer
import io.github.fate_grand_automata.ui.icon
import io.github.fate_grand_automata.ui.prefs.ListPreference
import io.github.fate_grand_automata.ui.prefs.remember
import io.github.fate_grand_automata.ui.prefs.SingleSelectChipPreference
import io.github.fate_grand_automata.ui.prefs.StepperPreference
import io.github.fate_grand_automata.ui.prefs.SwitchPreference
import io.github.fate_grand_automata.util.stringRes

fun LazyListScope.battleGroup(
    prefs: PrefsCore,
    questImages: List<String>
) {
    item {
        prefs.gameServerRaw.ListPreference(
            title = stringResource(R.string.p_game_server),
            icon = icon(Icons.Default.Public),
            entries =
            mapOf(PrefsCore.GAME_SERVER_AUTO_DETECT to stringResource(R.string.p_game_server_auto_detect))
                .plus(
                    GameServer.values.associate {
                        it.serialize() to stringResource(it.stringRes)
                    }
                )
        )
    }

    item {
        prefs.storySkip.SwitchPreference(
            title = stringResource(R.string.p_story_skip),
            summary = stringResource(R.string.p_story_skip_summary),
            icon = icon(Icons.Default.FastForward)
        )
    }

    item {
        prefs.withdrawEnabled.SwitchPreference(
            title = stringResource(R.string.p_enable_withdraw),
            icon = icon(R.drawable.ic_exit_run)
        )
    }

    item {
        prefs.stopOnCEGet.SwitchPreference(
            title = stringResource(R.string.p_stop_on_ce_get),
            summary = stringResource(R.string.p_stop_on_ce_get_summary),
            icon = icon(R.drawable.ic_card)
        )
    }

    item {
        prefs.stopOnFirstClearRewards.SwitchPreference(
            title = stringResource(R.string.p_stop_on_first_clear_rewards),
            icon = icon(R.drawable.ic_gift)
        )
    }

    item {
        prefs.screenshotDrops.SwitchPreference(
            title = stringResource(R.string.p_screenshot_drops),
            summary = stringResource(R.string.p_screenshot_drops_summary),
            icon = icon(R.drawable.ic_screenshot)
        )
    }

    item {
        prefs.screenshotDropsUnmodified.SwitchPreference(
            title = stringResource(R.string.p_screenshot_drops_unmodified),
            summary = stringResource(R.string.p_screenshot_drops_unmodified_summary),
            icon = icon(R.drawable.ic_screenshot)
        )
    }
    item {
        prefs.screenshotBond.SwitchPreference(
            title = stringResource(R.string.p_screenshot_bond),
            summary = stringResource(R.string.p_screenshot_bond_summary),
            icon = icon(R.drawable.ic_screenshot)
        )
    }

    item {
        prefs.autoDreamFireEnabled.SwitchPreference(
            title = stringResource(R.string.p_auto_dream_fire),
            summary = stringResource(R.string.p_auto_dream_fire_summary),
            icon = icon(Icons.Default.LocalFireDepartment)
        )
    }

    item {
        val debugMode by prefs.debugMode.remember()
        if (debugMode) {
            prefs.dreamFireTestMode.SwitchPreference(
                title = stringResource(R.string.p_dream_fire_test_mode),
                summary = stringResource(R.string.p_dream_fire_test_mode_summary),
                icon = icon(Icons.Default.LocalFireDepartment)
            )
        }
    }

    item {
        val secondsUnit = stringResource(R.string.p_seconds_unit)
        prefs.dreamFireBondWaitSeconds.StepperPreference(
            title = stringResource(R.string.p_dream_fire_bond_wait_seconds),
            valueRange = 1..10,
            valueRepresentation = { "$it $secondsUnit" }
        )
    }

    item {
        prefs.dreamFireLevel1Image.ListPreference(
            title = stringResource(R.string.p_dream_fire_level1_image),
            entries = listOf("").plus(questImages).associateWith {
                it.ifBlank { stringResource(R.string.p_not_set) }
            }
        )
    }

    item {
        prefs.dreamFireLevel2Image.ListPreference(
            title = stringResource(R.string.p_dream_fire_level2_image),
            entries = listOf("").plus(questImages).associateWith {
                it.ifBlank { stringResource(R.string.p_not_set) }
            }
        )
    }

    item {
        prefs.dreamFireMapImage.ListPreference(
            title = stringResource(R.string.p_dream_fire_map_image),
            entries = listOf("").plus(questImages).associateWith {
                it.ifBlank { stringResource(R.string.p_not_set) }
            }
        )
    }

    item {
        prefs.hideSQInAPResources.SwitchPreference(
            title = stringResource(R.string.p_hide_sq_in_resource),
            summary = stringResource(R.string.p_hide_sq_in_resource_summary),
            icon = icon(R.drawable.ic_apple)
        )
    }

    item {
        prefs.boostItemSelectionMode.SingleSelectChipPreference(
            title = stringResource(R.string.p_boost_item),
            icon = icon(Icons.Default.OfflineBolt),
            entries = (-1..3).associateWith { it.boostItemString() }
        )
    }

    item {
        prefs.skipServantFaceCardCheck.SwitchPreference(
            title = stringResource(R.string.p_skip_servant_face_checks),
            summary = stringResource(R.string.p_skip_servant_face_checks_summary),
            icon = icon(Icons.Default.NoAccounts)
        )
    }

    item {
        prefs.treatSupportLikeOwnServant.SwitchPreference(
            title = stringResource(R.string.p_treat_support_like_own_servant),
            summary = stringResource(R.string.p_treat_support_like_own_servant_summary),
            icon = icon(R.drawable.ic_biceps)
        )
    }
}

@Composable
private fun Int.boostItemString() = when (this) {
    -1 -> stringResource(R.string.p_boost_item_disabled)
    0 -> stringResource(R.string.p_boost_item_skip)
    else -> toString()
}
