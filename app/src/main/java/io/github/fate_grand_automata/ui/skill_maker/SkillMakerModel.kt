package io.github.fate_grand_automata.ui.skill_maker

import androidx.compose.runtime.toMutableStateList
import io.github.fate_grand_automata.scripts.models.AutoSkillAction
import io.github.fate_grand_automata.scripts.models.AutoSkillCommand

class SkillMakerModel(skillString: String) {
    private fun reduce(
        acc: List<SkillMakerEntry>,
        add: List<SkillMakerEntry>,
        separator: (AutoSkillAction.Atk) -> SkillMakerEntry.Next
    ): List<SkillMakerEntry> {
        if (acc.isNotEmpty()) {
            val last = acc.last()

            if (last is SkillMakerEntry.Action && last.action is AutoSkillAction.Atk) {
                return acc.subList(0, acc.lastIndex) + separator(last.action) + add
            }
        }

        return acc + separator(AutoSkillAction.Atk.noOp()) + add
    }

    val skillCommand = AutoSkillCommand.parse(skillString)
        .stages
        .map { turns ->
            turns
                .map { turn ->
                    turn.map<AutoSkillAction, SkillMakerEntry> {
                        SkillMakerEntry.Action(it)
                    }
                }
                .reduce { acc, turn ->
                    reduce(acc, turn) { SkillMakerEntry.Next.Turn(it) }
                }
        }
        .reduce { acc, stage ->
            reduce(acc, stage) { SkillMakerEntry.Next.Wave(it) }
        }
        .let { listOf(SkillMakerEntry.Start) + it }
        .toMutableStateList()

    /** One-based Wave at the insertion cursor, including a selected Wave separator. */
    fun waveAt(index: Int) = skillCommand.take(index + 1).count { it is SkillMakerEntry.Next.Wave } + 1

    /** Remove only explicit targets in one Wave, preserving every separator and other action. */
    fun removeEnemyTargets(wave: Int, cursor: Int): Pair<Int, Boolean> {
        var entryWave = 1
        val removed = skillCommand.indices.filter { index ->
            val entry = skillCommand[index]
            if (entry is SkillMakerEntry.Next.Wave) ++entryWave
            entryWave == wave && entry is SkillMakerEntry.Action && entry.action is AutoSkillAction.TargetEnemy
        }
        removed.asReversed().forEach { skillCommand.removeAt(it) }
        return (cursor - removed.count { it <= cursor }) to removed.isNotEmpty()
    }

    override fun toString(): String {
        fun getSkillCmd(): List<SkillMakerEntry> {
            if (skillCommand.isNotEmpty()) {
                val last = skillCommand.last()

                // remove trailing ',' or ',#,'
                if (last is SkillMakerEntry.Next) {
                    return skillCommand.subList(0, skillCommand.lastIndex) + SkillMakerEntry.Action(last.action)
                }
            }

            return skillCommand
        }

        return getSkillCmd().joinToString("")
    }
}
