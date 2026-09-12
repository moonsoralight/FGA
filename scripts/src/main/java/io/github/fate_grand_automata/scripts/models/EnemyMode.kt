package io.github.fate_grand_automata.scripts.models

enum class EnemyMode(val count: Int) {
    Three(3),
    Six(6);

    companion object {
        fun fromCount(count: Int?) = if (count == 6) Six else Three
    }
}
