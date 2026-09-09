package io.github.lib_automata

import javax.inject.Inject
import kotlin.time.Duration

interface Clicker {
    operator fun invoke(location: Location, times: Int = 1)
    fun withInterval(location: Location, times: Int, interval: Duration)
}

class RealClicker @Inject constructor(
    private val gestureService: GestureService,
    private val exitManager: ExitManager,
    private val transform: Transformer
): Clicker {
    override fun invoke(location: Location, times: Int) {
        exitManager.checkExitRequested()
        gestureService.click(transform.toScreen(location), times)
    }

    override fun withInterval(location: Location, times: Int, interval: Duration) {
        exitManager.checkExitRequested()
        gestureService.clickWithInterval(transform.toScreen(location), times, interval)
    }
}
