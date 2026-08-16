package io.github.fate_grand_automata.scripts.locations

import io.github.fate_grand_automata.scripts.enums.RefillResourceEnum
import io.github.fate_grand_automata.scripts.models.BoostItem
import io.github.lib_automata.Location
import io.github.lib_automata.Region
import io.github.lib_automata.dagger.ScriptScope
import javax.inject.Inject

@ScriptScope
class Locations @Inject constructor(
    scriptAreaTransforms: IScriptAreaTransforms,
    val fp: FPLocations,
    val lottery: LotteryLocations,
    val support: SupportScreenLocations,
    val attack: AttackScreenLocations,
    val battle: BattleScreenLocations,
    val servant: ServantLevelLocations,
) : IScriptAreaTransforms by scriptAreaTransforms {

    val continueRegion = Region(120, 1000, 800, 300).xFromCenter()

    val continueBoostClick = Location(-20, 1120).xFromCenter()

    val inventoryFullRegion = Region(-280, 860, 560, 190).xFromCenter()

    val ordealCallConfirmPodUseRegion = Region(190, 1096, 420, 62).xFromCenter()

    val ordealCallOutOfPodsRegion = Region(-112, 1088, 219, 72).xFromCenter()

    val ordealCallOutOfPodsClick = Location(-2, 1124).xFromCenter()

    val interludeCloseClick = Location(-399, 1125).xFromCenter()
    val interludeEndScreenClose = Region(-515, 1080, 230, 90).xFromCenter()

    val menuScreenRegion =
        (if (isWide)
            Region(-600, 1200, 600, 240)
        else Region(-460, 1200, 460, 240))
            .xFromRight()

    val menuSelectQuestClick =
        (if (isWide)
            Location(-460, 440)
        else Location(-270, 440))
            .xFromRight()

    val menuStartQuestClick =
        (if (isWide)
            Location(-350, -160)
        else Location(-160, -90))
            .xFromRight()
            .yFromBottom()

    val menuStorySkipYesClick = Location(320, 1100).xFromCenter()

    val retryRegion = Region(20, 1000, 700, 300).xFromCenter()

    val staminaScreenRegion = Region(-680, 200, 300, 300).xFromCenter()
    val staminaOkClick = Location(370, 1120).xFromCenter()
    val staminaCloseClick = Location(0, 1240).xFromCenter()

    val withdrawRegion = Region(-880, 540, 1800, 333).xFromCenter()
    val withdrawAcceptClick = Location(485, 720).xFromCenter()
    val withdrawCloseClick = Location(-10, 1140).xFromCenter()
    val commandSpellReviveRegion = Region(-360, 520, 720, 240).xFromCenter()
    val commandSpellReviveClick = Location(0, 645).xFromCenter()
    val commandSpellReviveConfirmRegion = Region(0, 520, 900, 500).xFromCenter()
    val commandSpellReviveConfirmClick = Location(350, 790).xFromCenter()

    fun locate(refillResource: RefillResourceEnum): List<Location> {
        //scroll bar click location
        val scrollBarLoc = when (refillResource) {
            RefillResourceEnum.Copper -> 1040
            else -> 300
        }.let { y -> Location(750, y).xFromCenter() }

        val resourceLoc = when (refillResource) {
            RefillResourceEnum.Copper -> 980
            RefillResourceEnum.Bronze -> 1140
            RefillResourceEnum.Silver -> 922
            RefillResourceEnum.Gold -> 634
            RefillResourceEnum.SQ -> 345
        }.let { y -> Location(-530, y).xFromCenter() }
        return listOf(scrollBarLoc, resourceLoc)
    }

    fun locate(boost: BoostItem.Enabled) = when (boost) {
        BoostItem.Enabled.Skip -> Location(1652, 1304)
        BoostItem.Enabled.BoostItem1 -> Location(1280, 418)
        BoostItem.Enabled.BoostItem2 -> Location(1280, 726)
        BoostItem.Enabled.BoostItem3 -> Location(1280, 1000)
    }.xFromCenter()

    val selectedPartyRegion = Region(-370, 62, 740, 72).xFromCenter()
    
    val partySelectionArray: List<Location> = (0..14).map {
        // Party 8 is on the center
        Location(((it - 7) * 50), 100).xFromCenter()
    }

    val menuStorySkipRegion = Region(960, 20, 300, 120).xFromCenter()
    val menuStorySkipClick = Location(1080, 80).xFromCenter()

    val resultFriendRequestRegion = Region(600, 150, 100, 94).xFromCenter()
    val resultFriendRequestRejectClick = Location(-680, 1200).xFromCenter()
    val resultMatRewardsRegion = Region(800, 1220, 280, 130).xFromCenter()
    val resultClick = Location(320, 1350).xFromCenter()
    val resultQuestRewardRegion = Region(350, 140, 370, 250).xFromCenter()
    val resultDropScrollbarRegion = Region(980, 167, 100, 88).xFromCenter()
    val resultDropScrollEndClick = Location(1030, 968).xFromCenter()
    val resultMasterExpRegion = Region(0, 350, 400, 110).xFromCenter()
    val resultMasterLvlUpRegion = Region(710, 160, 250, 270).xFromCenter()
    val resultScreenRegion = Region(-1180, 300, 700, 200).xFromCenter()
    val resultBondRegion = Region(720, 600, 120, 400).xFromCenter()

    val resultCeRewardRegion = Region(-230, 1216, 33, 28).xFromCenter()
    val resultCeRewardDetailsRegion = Region(if (isWide) 193 else 0, 512, 135, 115)
    val resultCeRewardCloseClick = Location(if (isWide) 265 else 80, 60)

    val giftBoxSwipeStart = Location(120, if (canLongSwipe) 1200 else 1050).xFromCenter()
    val giftBoxSwipeEnd = Location(120, if (canLongSwipe) 350 else 575).xFromCenter()

    val emptyEnhanceRegion = when (isWide) {
        true -> Region(-1100, 600, 400, 400).xFromCenter()
        false -> Region(200, 600, 400, 400)
    }
    val ceEnhanceClick = Location(200, 600)
    val levelOneCERegion = Region(160, 380, 1840, 900)

    val npStartedRegion = Region(-400, 500, 800, 400).xFromCenter()

    val rankUpRegion = Region(270, 730, 220, 340).xFromCenter()

    val middleOfScreenClick = Location(0, 720).xFromCenter()

    // Auto Chaldean Flame.
    val dreamFireTopLeftRegion = Region(80, 0, 390, 145)
    val dreamFireTopLeftClick = Location(250, 75)
    val dreamFireStrengthenClick = Location(-155, 1260).xFromCenter()
    val dreamFireEnhancementTextRegion = Region(-1250, 120, 1200, 1160).xFromRight()
    val dreamFireEnhancementSwipeStart = Location(-170, 330).xFromRight()
    val dreamFireEnhancementSwipeEnd = Location(-170, 1110).xFromRight()
    val dreamFireBondUnlockClick = Location(-630, 400).xFromRight()
    val dreamFirePlusClick = Location(545, 700)
    val dreamFirePickerCells: List<Region> = listOf(365, 635, 905, 1175, 1445, 1715, 1985).flatMap { x ->
        listOf(260, 625, 990).map { y -> Region(x, y, 245, 315) }
    }
    val dreamFirePickerSwipeStart = Location(-190, 1110).xFromRight()
    val dreamFirePickerSwipeEnd = Location(-190, 310).xFromRight()
    val dreamFireOpenClick = Location(-315, -145).xFromRight().yFromBottom()
    val dreamFireConfirmClick = Location(330, 950).xFromCenter()
    val dreamFireAnimationClick = Location(0, 720).xFromCenter()
    val dreamFireListSwipeStart = Location(-170, 1110).xFromRight()
    val dreamFireListSwipeEnd = Location(-170, 310).xFromRight()

    // Auto Dream Fire v2. All coordinates are in FGA's 2560x1440 script space and are
    // transformed by the normal game-area scaler at runtime.
    val dreamFireBondCapLane = Region(-1200, 930, 2400, 190).xFromCenter()
    val dreamFireMenuRegion = Region(-580, 1180, 500, 220).xFromRight()
    val dreamFireStrengthenRegion = Region(1080, 1000, 430, 380)
    val dreamFireStrengthenTitleRegion = Region(-520, 0, 520, 190).xFromRight()
    val dreamFireEnhancementListRegion = Region(-1500, 120, 1450, 1200).xFromRight()
    // Keep every Auto Dream Fire gesture inside the same safe vertical lane, but use only
    // half of the former 810-unit travel. Smaller steps prevent narrow entrance rows from
    // jumping completely past their comparison region on high-resolution devices.
    val dreamFireSafeSwipeStart = Location(-650, 1110).xFromRight()
    val dreamFireSafeSwipeEnd = Location(-650, 705).xFromRight()
    val dreamFireSafeSwipeDownStart = dreamFireSafeSwipeEnd
    val dreamFireSafeSwipeDownEnd = dreamFireSafeSwipeStart
    val dreamFirePlusRegion = Region(480, 540, 460, 480)
    val dreamFireEligibleLane = Region(420, 500, 2050, 220)
    val dreamFireOpenRegion = Region(-700, -270, 620, 240).xFromRight().yFromBottom()
    val dreamFireConfirmButtonRegion = Region(120, 1060, 620, 250).xFromCenter()
    // Dedicated top-left "关闭" search area shared by the servant picker and the empty '+' page.
    // Keeping this tight avoids matching unrelated close text elsewhere on enhancement screens.
    val dreamFireBackRegion = Region(240, 20, 320, 140)
    val dreamFireTerminalRegion = Region(420, 1020, 420, 360)
    val dreamFireNotificationRegion = Region(80, 0, 520, 190)
    val dreamFireRightHalfRegion = Region(
        scriptArea.center.x,
        scriptArea.y,
        scriptArea.right - scriptArea.center.x,
        scriptArea.height
    )
    // Level-1 entrance search excludes the notification panel in the upper-right. The source
    // reference is a 2736x1264 device screenshot; its marked panel ends at about y=480, which
    // maps to y=547 in FGA's 2560x1440 script coordinates. Round to 550 and keep the entire
    // remaining right half searchable. Level-2 entrance search intentionally uses the full
    // [dreamFireRightHalfRegion].
    val dreamFireLevel1SearchRegion = Region(
        scriptArea.center.x,
        550,
        scriptArea.right - scriptArea.center.x,
        scriptArea.bottom - 550
    )
    val dreamFireScrollbarRegion = Region(-120, 130, 120, 1180).xFromRight()
    val dreamFireScrollbarBottomRegion = Region(-120, 1010, 120, 350).xFromRight()
    val dreamFireMapRegion = scriptArea
    val dreamFireQuestConfirmRegion = Region(0, 120, 460, 1240).xFromCenter()
    val dreamFireMapClickOffset = Location(-170, -80)
    val dreamFireQuestClickOffset = Location(380, 120)

    /**
     * The following region are used for the various enhancement screen listed below:
     * Skill Upgrade, Ascension, Append Upgrade and Grail
     */
    val enhancementBannerRegion = when(isWide) {
        true -> Region(-412, 282, 241, 37).xFromCenter()
        false -> Region(-413, 324, 241, 37).xFromCenter()
    }

    val enhancementClick = when (isWide) {
        false -> Location(-281, 1343).xFromRight()
        true -> Location(-396, 1284).xFromRight()
    }

    val tempServantEnhancementRegion = Region(252, 1096, 301, 57).xFromCenter()

    val enhancementSkipRapidClick = Location(0, 1400).xFromCenter()

    val tempServantEnhancementLocation = Location(402, 1124).xFromCenter()
}
