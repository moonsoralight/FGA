package io.github.fate_grand_automata.scripts.modules

import io.github.fate_grand_automata.scripts.IFgoAutomataApi
import io.github.fate_grand_automata.scripts.Images
import io.github.fate_grand_automata.scripts.ScriptNotify
import io.github.lib_automata.Match
import io.github.lib_automata.Pattern
import io.github.lib_automata.Region
import io.github.lib_automata.Scale
import io.github.lib_automata.Swiper
import io.github.lib_automata.dagger.ScriptScope
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** OCR-free Chaldean Flame workflow driven by grayscale templates. */
@ScriptScope
class AutoDreamFire @Inject constructor(
    api: IFgoAutomataApi,
    private val swipe: Swiper,
    private val scale: Scale
) : IFgoAutomataApi by api, AutoCloseable {
    class Failure(message: String) : Exception(message)

    private companion object {
        const val MAX_CLICK_ATTEMPTS = 10
        const val MAX_ENTRY_SWIPES = 60
        val SETTLED_MATCH_TIMEOUT = 3.seconds
        val SWIPE_MATCH_TIMEOUT = 2.seconds
        val CLICK_SETTLE_DELAY = 1.seconds
    }

    private var pending = false
    val hasPending: Boolean get() = pending

    fun recordCappedOwnedServants(): Boolean {
        if (!prefs.autoDreamFireEnabled || pending) return true

        // result.png remains the normal completion trigger. Only when Auto Dream Fire is
        // enabled, leave this first result page untouched while polling the grayscale
        // "最大值" template. A match starts the Dream Fire flow immediately; reaching the
        // configured 1-10 second limit means that normal result clicking can resume.
        pending = locations.dreamFireBondCapLane.exists(
            images[Images.DreamFireBondMax],
            prefs.dreamFireBondWaitSeconds.coerceIn(1, 10).seconds,
            similarity = 0.72
        )
        return true
    }

    fun executeFromRepeatScreen() {
        if (!pending) return
        if (prefs.dreamFireLevel1Image.isBlank()) {
            throw Failure("已停止脚本：原因：未设置一级入口特征图")
        }

        leaveContinuousSortie()
        openBondCapEnhancement()
        unlockAllEligibleServants()
        pending = false
    }

    /**
     * Test-only entry cut directly from the completed production flow. It starts at the servant
     * picker after all eligible servants have already been handled, so it can verify the two
     * close actions and the entire return-to-quest tail without consuming a Chaldean Flame.
     */
    fun executeTestFromServantPicker() {
        if (prefs.dreamFireLevel1Image.isBlank()) {
            throw Failure("已停止脚本：原因：未设置一级入口特征图")
        }

        messages.notify(ScriptNotify.Alert("返回FGO继续测试"))

        val closePattern = images[Images.DreamFireBackClose]
        val plusPattern = images[Images.DreamFirePlus]
        waitForServantPicker(closePattern, plusPattern)

        // Keep the production picker's existing five-second eligibility check. Test mode must
        // never select an eligible servant because that could spend a Chaldean Flame.
        val eligible = waitFor(
            locations.dreamFireEligibleLane,
            images[Images.DreamFireCanOpen],
            5.seconds,
            0.68
        )
        if (eligible != null) {
            throw Failure("梦火测试页面仍有可以开放的从者，已停止以避免消耗梦火")
        }

        finishAfterNoEligibleServantAndResumeQuest(closePattern, plusPattern)
    }

    private fun waitForServantPicker(closePattern: Pattern, plusPattern: Pattern) {
        while (true) {
            val close = locations.dreamFireBackRegion.find(closePattern, 0.66)
            val plus = locations.dreamFirePlusRegion.find(plusPattern, 0.66)
            if (close != null && plus == null) return
            1.seconds.wait()
        }
    }

    private fun leaveContinuousSortie() {
        clickUntilNext(
            currentRegion = locations.scriptArea,
            currentPattern = images[Images.Close],
            nextRegion = locations.dreamFireMenuRegion,
            nextPattern = images[Images.DreamFireMenu],
            failureMessage = "未找到退出连续出击后的菜单",
            currentSimilarity = 0.70,
            // This click performs the exit itself. The one-second settled-page rule starts
            // only after the quest has been left and the main menu is visible.
            settleBeforeClick = false
        )
    }

    private fun openBondCapEnhancement() {
        clickUntilNext(
            currentRegion = locations.dreamFireMenuRegion,
            currentPattern = images[Images.DreamFireMenu],
            nextRegion = locations.dreamFireStrengthenRegion,
            nextPattern = images[Images.DreamFireStrengthen],
            failureMessage = "未找到强化入口",
            currentSimilarity = 0.70
        )

        clickUntilNext(
            currentRegion = locations.dreamFireStrengthenRegion,
            currentPattern = images[Images.DreamFireStrengthen],
            nextRegion = locations.dreamFireStrengthenTitleRegion,
            nextPattern = images[Images.DreamFireStrengthenTitle],
            failureMessage = "未进入强化页面"
        )

        val unlockPattern = images[Images.DreamFireBondUnlock]
        findWithUpwardSwipes(
            region = locations.dreamFireEnhancementListRegion,
            pattern = unlockPattern,
            maxSwipes = MAX_ENTRY_SWIPES,
            similarity = 0.68
        ) ?: throw Failure("未找到牵绊等级上限开放")

        clickUntilNext(
            currentRegion = locations.dreamFireEnhancementListRegion,
            currentPattern = unlockPattern,
            nextRegion = locations.dreamFirePlusRegion,
            nextPattern = images[Images.DreamFirePlus],
            failureMessage = "未进入牵绊等级上限开放页面"
        )
    }

    private fun unlockAllEligibleServants() {
        val plusPattern = images[Images.DreamFirePlus]
        // Both the servant picker and the empty '+' page use the same top-left button.
        // Use its dedicated tightly-cropped grayscale template instead of FGA's generic close.
        val closePattern = images[Images.DreamFireBackClose]

        while (true) {
            if (locations.dreamFirePlusRegion.find(plusPattern, 0.68) == null) {
                throw Failure("未找到从者选择按钮")
            }

            // The picker has no mandatory next-page template: when all five servants have
            // already been handled, "可以开放" is correctly absent. Use disappearance of the
            // '+' page as the transition proof, then inspect the settled picker.
            clickUntilGone(
                currentRegion = locations.dreamFirePlusRegion,
                currentPattern = plusPattern,
                failureMessage = "点击从者选择按钮后页面未切换"
            )

            val eligible = waitFor(
                locations.dreamFireEligibleLane,
                images[Images.DreamFireCanOpen],
                5.seconds,
                0.68
            )
            if (eligible == null) {
                // From this point production mode must use the exact tail verified by test mode:
                // close the picker, close the empty '+' page, return through Menu -> Terminal,
                // verify the main page, and navigate back to the configured quest.
                finishAfterNoEligibleServantAndResumeQuest(closePattern, plusPattern)
                return
            }

            clickUntilNext(
                currentRegion = locations.dreamFireEligibleLane,
                currentPattern = images[Images.DreamFireCanOpen],
                nextRegion = locations.dreamFireOpenRegion,
                nextPattern = images[Images.DreamFireOpen],
                failureMessage = "选择从者后未找到开放按钮"
            )

            // The game can render the "开放" feature before the page accepts input. Use the
            // same ten verified attempts as every other transition. Only ten complete
            // locate-wait-click-check cycles without a "决定" dialog mean insufficient
            // materials; one early ignored tap must never trigger that diagnosis.
            val confirm = clickOpenUntilConfirm()
            if (confirm == null) {
                val message = "已停止脚本：原因：材料不足"
                messages.notify(ScriptNotify.Alert(message))
                throw Failure(message)
            }

            clickUntilGone(
                currentRegion = locations.dreamFireConfirmButtonRegion,
                currentPattern = images[Images.DreamFireConfirm],
                failureMessage = "点击决定后梦火开放流程未启动",
                similarity = 0.70
            )

            // Speed up the effect, but stop tapping immediately once the empty '+' page returns.
            var returned = false
            for (attempt in 0 until 40) {
                if (locations.dreamFirePlusRegion.find(plusPattern, 0.66) != null) {
                    returned = true
                    break
                }
                locations.dreamFireOpenRegion.click()
                250.milliseconds.wait()
            }
            if (!returned && !locations.dreamFirePlusRegion.exists(
                    plusPattern,
                    8.seconds,
                    0.66
                )
            ) throw Failure("梦火动画结束后未返回从者选择页面")
        }

    }

    /**
     * Shared tail after the servant picker contains no "can open" entry.
     *
     * The same tightly-cropped top-left "关闭" template is deliberately used twice:
     * servant picker -> empty '+' page -> strengthening list. Each click is accepted only
     * after the next page's positive template is visible.
     */
    private fun finishAfterNoEligibleServant(
        closePattern: Pattern = images[Images.DreamFireBackClose],
        plusPattern: Pattern = images[Images.DreamFirePlus]
    ) {
        clickUntilNext(
            currentRegion = locations.dreamFireBackRegion,
            currentPattern = closePattern,
            nextRegion = locations.dreamFirePlusRegion,
            nextPattern = plusPattern,
            failureMessage = "关闭从者列表后未返回梦火页面",
            currentSimilarity = 0.66,
            nextSimilarity = 0.66
        )

        // Empty '+' page -> strengthening list. Verify the menu before returning.
        clickUntilNext(
            currentRegion = locations.dreamFireBackRegion,
            currentPattern = closePattern,
            nextRegion = locations.dreamFireMenuRegion,
            nextPattern = images[Images.DreamFireMenu],
            failureMessage = "返回强化列表后未找到菜单",
            currentSimilarity = 0.66
        )
    }

    /**
     * One shared, production-grade tail used by both normal Auto Dream Fire and its test mode.
     * Keeping the complete tested sequence behind one call prevents the two paths from drifting.
     */
    private fun finishAfterNoEligibleServantAndResumeQuest(
        closePattern: Pattern = images[Images.DreamFireBackClose],
        plusPattern: Pattern = images[Images.DreamFirePlus]
    ) {
        finishAfterNoEligibleServant(closePattern, plusPattern)
        returnToMainMenu()
        returnToQuest()
    }

    private fun returnToMainMenu() {
        clickUntilNext(
            currentRegion = locations.dreamFireMenuRegion,
            currentPattern = images[Images.DreamFireMenu],
            nextRegion = locations.dreamFireTerminalRegion,
            nextPattern = images[Images.DreamFireTerminal],
            failureMessage = "未找到终端入口"
        )

        clickUntilNext(
            currentRegion = locations.dreamFireTerminalRegion,
            currentPattern = images[Images.DreamFireTerminal],
            nextRegion = locations.dreamFireNotificationRegion,
            nextPattern = images[Images.DreamFireNotification],
            failureMessage = "未能返回显示“通知”的游戏主页面",
            currentSimilarity = 0.66
        )
    }

    private fun returnToQuest() {
        findStoredEntryAndClick(
            name = prefs.dreamFireLevel1Image,
            label = "一级入口",
            searchRegion = locations.dreamFireLevel1SearchRegion
        )

        if (prefs.dreamFireLevel2Image.isNotBlank()) {
            findStoredEntryAndClick(
                name = prefs.dreamFireLevel2Image,
                label = "二级入口",
                searchRegion = locations.dreamFireRightHalfRegion
            )
        }

        // These are the two intentional fixed delays. Both target screens may have a heavy
        // map/list load before FGA's ordinary repeated comparison becomes meaningful.
        5.seconds.wait()
        val lastExecutedPattern = images[Images.DreamFireLastExecuted]
        if (prefs.dreamFireMapImage.isNotBlank()) {
            val mapPattern = loadStoredQuestPattern(
                name = prefs.dreamFireMapImage,
                label = "地图关卡"
            )
            openLastExecutedQuestFromMap(mapPattern, lastExecutedPattern)
        } else {
            5.seconds.wait()
            waitFor(
                locations.dreamFireQuestConfirmRegion,
                lastExecutedPattern,
                SETTLED_MATCH_TIMEOUT,
                0.68
            ) ?: throw Failure("未找到“上次执行”标签")
        }

        clickQuestUntilSupportOrStamina(lastExecutedPattern)
    }

    /**
     * Map mode is intentionally strict: the map marker is only observed on the first match.
     * After one fixed second it must still match before the offset quest location is clicked.
     * The following three seconds are completely silent; only then may "last executed" be
     * checked. A missing confirmation restarts the whole marker check/click cycle.
     */
    private fun openLastExecutedQuestFromMap(
        mapPattern: Pattern,
        lastExecutedPattern: Pattern
    ) {
        var mapWasClicked = false

        repeat(MAX_CLICK_ATTEMPTS) {
            // After the first map click, entering the confirmation phase is proven only by
            // the next phase's "last executed" feature. It may coexist with the old map
            // marker, so it must be checked first and continuously rather than by one frame.
            if (mapWasClicked && isQuestConfirmationVisible(lastExecutedPattern)) return

            waitFor(
                locations.dreamFireMapRegion,
                mapPattern,
                SETTLED_MATCH_TIMEOUT,
                0.66
            ) ?: return@repeat

            // First match is validation only and must never be clicked.
            CLICK_SETTLE_DELAY.wait()

            // The confirmation panel may finish loading during the one-second settling
            // period while the old map marker is still visible underneath it.
            if (mapWasClicked && isQuestConfirmationVisible(lastExecutedPattern)) return

            val secondMatch = locations.dreamFireMapRegion.find(mapPattern, 0.66)
                ?: return@repeat

            (secondMatch.region.center + locations.dreamFireMapClickOffset).click()
            mapWasClicked = true

            // Do not inspect any image while the quest detail panel is loading.
            3.seconds.wait()

            // The next-step feature wins even when the map marker remains visible behind
            // the opened panel. Only a full timeout may restart the map click cycle.
            if (isQuestConfirmationVisible(lastExecutedPattern)) return
        }

        throw Failure("有地图模式下未能从“上一次”关卡进入确认关卡页面")
    }

    private fun isQuestConfirmationVisible(lastExecutedPattern: Pattern): Boolean =
        locations.dreamFireQuestConfirmRegion.exists(
            lastExecutedPattern,
            SETTLED_MATCH_TIMEOUT,
            0.68
        )

    /**
     * Keep the established quest click offset, but require a positive destination screen.
     * Each click is followed by a fixed three-second no-comparison window. Only FGA's original
     * support-screen or stamina-dialog detector can complete this step.
     */
    private fun clickQuestUntilSupportOrStamina(lastExecutedPattern: Pattern) {
        repeat(MAX_CLICK_ATTEMPTS) {
            if (isSupportOrStaminaScreen()) return

            locations.dreamFireQuestConfirmRegion.find(
                lastExecutedPattern,
                0.68
            ) ?: return@repeat

            CLICK_SETTLE_DELAY.wait()
            val secondMatch = locations.dreamFireQuestConfirmRegion.find(
                lastExecutedPattern,
                0.68
            ) ?: return@repeat

            (secondMatch.region.center + locations.dreamFireQuestClickOffset).click()

            // The quest transition must be allowed to finish before checking its destination.
            3.seconds.wait()
            if (isSupportOrStaminaScreen()) return
        }

        throw Failure("点击关卡10次后仍未进入助战或体力恢复页面")
    }

    private fun isSupportOrStaminaScreen(): Boolean = useSameSnapIn {
        locations.support.screenCheckRegion.find(
            images[Images.SupportScreen],
            0.85
        ) != null || locations.staminaScreenRegion.find(
            images[Images.Stamina]
        ) != null
    }

    private fun findStoredEntryAndClick(
        name: String,
        label: String,
        searchRegion: Region
    ) {
        val pattern = loadStoredQuestPattern(name, label)

        findWithListResetAndUpwardSwipes(
            region = searchRegion,
            pattern = pattern,
            maxSwipes = MAX_ENTRY_SWIPES,
            similarity = 0.70
        ) ?: throw Failure("未找到$label")

        clickUntilGone(
            currentRegion = searchRegion,
            currentPattern = pattern,
            failureMessage = "点击${label}后页面未切换",
            similarity = 0.70
        )
    }

    /**
     * All user-supplied access/ images share one loading path. The image loader uses the live
     * script screenshot scale, so entrance and map templates are normalized identically for the
     * current device before any grayscale comparison is attempted.
     */
    private fun loadStoredQuestPattern(name: String, label: String): Pattern = runCatching {
        images.loadQuestPattern(name, scale.screenToImage ?: Scale.NoScaling)
    }.getOrElse { throw Failure("无法读取${label}图片：$name") }

    /**
     * User entrance lists are first normalized to their top with three downward swipes. Every
     * completed swipe is followed by a settled, repeated comparison lasting up to two seconds.
     * No comparison runs during a gesture. Entrance lists deliberately do not use the scrollbar
     * tip as an early-stop signal: that small grayscale feature can also match before the list has
     * actually reached the bottom. The bounded swipe count is the only traversal safety limit.
     */
    private fun findWithListResetAndUpwardSwipes(
        region: Region,
        pattern: Pattern,
        maxSwipes: Int,
        similarity: Double
    ): Match? {
        waitFor(region, pattern, SETTLED_MATCH_TIMEOUT, similarity)?.let { return it }

        repeat(3) {
            swipe(locations.dreamFireSafeSwipeDownStart, locations.dreamFireSafeSwipeDownEnd)
            waitFor(region, pattern, SWIPE_MATCH_TIMEOUT, similarity)?.let { return it }
        }

        repeat(maxSwipes) {
            swipe(locations.dreamFireSafeSwipeStart, locations.dreamFireSafeSwipeEnd)
            waitFor(region, pattern, SWIPE_MATCH_TIMEOUT, similarity)?.let { return it }
        }
        return null
    }

    /** Same settled-swipe rule for the strengthening list. */
    private fun findWithUpwardSwipes(
        region: Region,
        pattern: Pattern,
        maxSwipes: Int,
        similarity: Double
    ): Match? {
        waitFor(region, pattern, SETTLED_MATCH_TIMEOUT, similarity)?.let { return it }

        repeat(maxSwipes) {
            swipe(locations.dreamFireSafeSwipeStart, locations.dreamFireSafeSwipeEnd)
            waitFor(region, pattern, SWIPE_MATCH_TIMEOUT, similarity)?.let { return it }
        }
        return null
    }

    private fun clickOpenUntilConfirm(): Match? {
        val openPattern = images[Images.DreamFireOpen]
        val confirmPattern = images[Images.DreamFireConfirm]

        repeat(MAX_CLICK_ATTEMPTS) {
            locations.dreamFireConfirmButtonRegion.find(confirmPattern, 0.70)?.let { return it }

            val open = locations.dreamFireOpenRegion.find(openPattern, 0.68)
            if (open != null) {
                CLICK_SETTLE_DELAY.wait()
                // Re-find after the fixed delay so a stale pre-load coordinate is never used.
                locations.dreamFireOpenRegion.find(openPattern, 0.68)?.region?.click()
            }

            waitFor(
                locations.dreamFireConfirmButtonRegion,
                confirmPattern,
                SETTLED_MATCH_TIMEOUT,
                0.70
            )?.let { return it }
        }
        return null
    }

    /**
     * Clicks only a freshly re-found current feature. After every click, the next page is polled
     * for three seconds. A missed next feature causes the previous feature to be re-found before
     * another click; stale coordinates are never reused. Ten unsuccessful attempts stop safely.
     */
    private fun clickUntilNext(
        currentRegion: Region,
        currentPattern: Pattern,
        nextRegion: Region,
        nextPattern: Pattern,
        failureMessage: String,
        currentSimilarity: Double = 0.68,
        nextSimilarity: Double = 0.68,
        waitAfterClick: Duration = Duration.ZERO,
        settleBeforeClick: Boolean = true,
        click: (Match) -> Unit = { it.region.click() }
    ): Match {
        repeat(MAX_CLICK_ATTEMPTS) {
            nextRegion.find(nextPattern, nextSimilarity)?.let { return it }

            currentRegion.find(currentPattern, currentSimilarity)?.let {
                if (settleBeforeClick) CLICK_SETTLE_DELAY.wait()
                // The first match may have been rendered before the page became interactive.
                // Re-locate after the fixed delay and click only the current, settled feature.
                currentRegion.find(currentPattern, currentSimilarity)?.let { settled ->
                    click(settled)
                    if (waitAfterClick > Duration.ZERO) waitAfterClick.wait()
                }
            }

            waitFor(
                nextRegion,
                nextPattern,
                SETTLED_MATCH_TIMEOUT,
                nextSimilarity
            )?.let { return it }
        }
        throw Failure(failureMessage)
    }

    /** Transition proof for pages whose valid next screen has no mandatory positive template. */
    private fun clickUntilGone(
        currentRegion: Region,
        currentPattern: Pattern,
        failureMessage: String,
        similarity: Double = 0.68,
        click: (Match) -> Unit = { it.region.click() }
    ) {
        repeat(MAX_CLICK_ATTEMPTS) {
            currentRegion.find(currentPattern, similarity) ?: return
            CLICK_SETTLE_DELAY.wait()
            val current = currentRegion.find(currentPattern, similarity) ?: return@repeat
            click(current)
            if (currentRegion.waitVanish(
                    currentPattern,
                    SETTLED_MATCH_TIMEOUT,
                    similarity
                )
            ) return
        }
        throw Failure(failureMessage)
    }

    private fun waitFor(
        region: Region,
        pattern: Pattern,
        timeout: Duration,
        similarity: Double
    ): Match? {
        if (!region.exists(pattern, timeout, similarity)) return null
        return region.find(pattern, similarity)
    }

    override fun close() {
        pending = false
    }
}
