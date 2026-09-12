package io.github.fate_grand_automata.scripts.models

sealed class EnemyTarget(val autoSkillCode: Char) {
    object A : EnemyTarget('1')
    object B : EnemyTarget('2')
    object C : EnemyTarget('3')
    object D : EnemyTarget('4')
    object E : EnemyTarget('5')
    object F : EnemyTarget('6')

    companion object {
        val list by lazy { listOf(A, B, C, D, E, F) }
        // Automatic targeting retains its original left-to-right spatial order.
        val autoChooseTargets by lazy { listOf(A, B, C) }
    }
}
