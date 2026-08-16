package io.github.fate_grand_automata.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.fate_grand_automata.IStorageProvider
import io.github.fate_grand_automata.R
import io.github.fate_grand_automata.SupportImageKind
import io.github.fate_grand_automata.imaging.DroidCvPattern
import io.github.fate_grand_automata.scripts.IImageLoader
import io.github.fate_grand_automata.scripts.Images
import io.github.fate_grand_automata.scripts.enums.GameServer
import io.github.fate_grand_automata.scripts.enums.MaterialEnum
import io.github.fate_grand_automata.scripts.prefs.IPreferences
import io.github.lib_automata.ColorManager
import io.github.lib_automata.Pattern
import io.github.lib_automata.Size
import org.opencv.android.Utils
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import kotlin.math.roundToInt

class ImageLoader @Inject constructor(
    val storageProvider: IStorageProvider,
    val prefs: IPreferences,
    @ApplicationContext val context: Context,
    private val colorManager: ColorManager
) : IImageLoader {
    private fun createPattern(gameServer: GameServer, FileName: String): Pattern {
        val gameServerPath = gameServer.simple
        val filePath = "$gameServerPath/$FileName"

        val assets = context.assets

        // load image from En by default or from current game server if a custom image exists
        val gameServerWithImage = if (assets.list(gameServerPath)?.contains(FileName) == true) {
            gameServerPath
        } else GameServer.default.simple

        val inputStream = assets.open("$gameServerWithImage/${FileName}")

        inputStream.use {
            return DroidCvPattern(it, colorManager.isColor, filePath)
        }
    }

    private data class CacheKey(val name: String, val gameServer: GameServer?, val isColor: Boolean)

    private fun key(name: String, gameServer: GameServer? = null) = CacheKey(name, gameServer, colorManager.isColor)

    private var currentGameServer: GameServer = GameServer.default
    private var regionCachedPatterns = mutableMapOf<CacheKey, Pattern>()

    override operator fun get(img: Images, gameServer: GameServer?): Pattern = synchronized(regionCachedPatterns) {
        val path = img.path

        val server = prefs.gameServer

        // Reload Patterns on Server change
        if (currentGameServer != server) {
            clearImageCache()

            currentGameServer = server
        }

        return regionCachedPatterns.getOrPut(key(path, gameServer)) {
            loadPatternWithFallback(path, gameServer)
        }
    }

    /**
     * When image is not available for the current server, use the image from NA server.
     */
    private fun loadPatternWithFallback(path: String, gameServer: GameServer?): Pattern {
        return createPattern(gameServer ?: currentGameServer, path)
    }

    override fun clearImageCache() = synchronized(regionCachedPatterns) {
        for (pattern in regionCachedPatterns.values) {
            pattern.close()
        }

        regionCachedPatterns.clear()

        clearSupportCache()
    }

    private var supportCachedPatterns = mutableMapOf<CacheKey, List<Pattern>>()
    private var questCachedPatterns = mutableMapOf<CacheKey, Pattern>()
    private var dreamFireMarkerPatterns: List<Pattern>? = null

    override fun clearSupportCache() = synchronized(supportCachedPatterns) {
        for (patterns in supportCachedPatterns.values) {
            patterns.forEach { it.close() }
        }

        supportCachedPatterns.clear()

        for (pattern in questCachedPatterns.values) {
            pattern.close()
        }
        questCachedPatterns.clear()

        dreamFireMarkerPatterns?.forEach { it.close() }
        dreamFireMarkerPatterns = null
    }

    private fun fileLoader(kind: SupportImageKind, name: String): List<Pattern> {
        val inputStreams = storageProvider.readSupportImage(kind, name)
        if (inputStreams.isEmpty()) {
            throw SupportImageNotFoundException(kind, name)
        }
        return inputStreams.withIndex().map { (i, stream) ->
            stream.use {
                DroidCvPattern(it, colorManager.isColor, "$name:$i")
            }
        }
    }

    override fun loadSupportPattern(kind: SupportImageKind, name: String): List<Pattern> = synchronized(supportCachedPatterns) {
        return supportCachedPatterns.getOrPut(key("$kind:$name")) {
            fileLoader(kind, name)
        }
    }

    override fun loadMaterial(material: MaterialEnum) =
        regionCachedPatterns.getOrPut(key("materials/$material")) {
            DroidCvPattern(
                Utils.loadResource(context, material.drawable, Imgcodecs.IMREAD_GRAYSCALE),
                tag = "MAT:$material"
            )
        }

    override fun loadQuestPattern(
        name: String,
        deviceToCompareScale: Double
    ): Pattern = synchronized(questCachedPatterns) {
        // access/ crops come from the same device that is currently running the script. Use the
        // exact scale of that script's live screenshot pipeline instead of independently deriving
        // it from display metrics. This keeps user templates and live screenshots pixel-aligned on
        // devices whose physical, logical, game-render, or MediaProjection sizes differ.
        val normalizedScale = deviceToCompareScale.takeIf { it.isFinite() && it > 0.0 } ?: 1.0
        questCachedPatterns.getOrPut(key("quest:$name@$normalizedScale")) {
            val source = storageProvider.readQuestImage(name).use {
                DroidCvPattern(it, false, "ACCESS:$name")
            }

            if (normalizedScale == 1.0) {
                source
            } else {
                val normalizedSize = Size(
                    (source.width * normalizedScale).roundToInt().coerceAtLeast(1),
                    (source.height * normalizedScale).roundToInt().coerceAtLeast(1)
                )
                source.resize(normalizedSize).also { source.close() }
            }
        }
    }

    override fun loadDreamFireMarkerPatterns(): List<Pattern> {
        dreamFireMarkerPatterns?.let { return it }

        fun baseMarker(): Mat {
            val mat = Mat.zeros(52, 52, CvType.CV_8UC1)
            val white = Scalar(255.0)
            val top = arrayOf(Point(26.0, 3.0), Point(42.0, 18.0), Point(26.0, 33.0), Point(10.0, 18.0), Point(26.0, 3.0))
            val bottom = arrayOf(Point(26.0, 19.0), Point(42.0, 34.0), Point(26.0, 49.0), Point(10.0, 34.0), Point(26.0, 19.0))
            Imgproc.polylines(mat, listOf(org.opencv.core.MatOfPoint(*top)), false, white, 4)
            Imgproc.polylines(mat, listOf(org.opencv.core.MatOfPoint(*bottom)), false, white, 4)
            return mat
        }

        val source = baseMarker()
        val frames = (0 until 8).map { frame ->
            val output = Mat.zeros(source.size(), source.type())
            val rotation = Imgproc.getRotationMatrix2D(Point(26.0, 26.0), frame * 22.5, 1.0)
            Imgproc.warpAffine(source, output, rotation, source.size())
            rotation.release()
            DroidCvPattern(output, tag = "DREAM-FIRE-MARKER:$frame")
        }
        source.release()
        dreamFireMarkerPatterns = frames
        return frames
    }
}

class SupportImageNotFoundException(kind: SupportImageKind, name: String) : 
    Exception("Support image not found for kind: $kind, name: $name")

val MaterialEnum.drawable
    get() = when (this) {
        MaterialEnum.Proof -> R.drawable.mat_proof
        MaterialEnum.Bone -> R.drawable.mat_bone
        MaterialEnum.Fang -> R.drawable.mat_fang
        MaterialEnum.Dust -> R.drawable.mat_dust
        MaterialEnum.Chain -> R.drawable.mat_chain
        MaterialEnum.Stinger -> R.drawable.mat_stinger
        MaterialEnum.Fluid -> R.drawable.mat_fluid
        MaterialEnum.Stake -> R.drawable.mat_stake
        MaterialEnum.Gunpowder -> R.drawable.mat_gunpowder
        MaterialEnum.AmnestyBell -> R.drawable.mat_amnesty_bell
        MaterialEnum.CeremonialBlade -> R.drawable.mat_ceremonial_blade
        MaterialEnum.UnforgettableAshes -> R.drawable.mat_ashes
        MaterialEnum.ObsidianEdge -> R.drawable.mat_obsidian_edge
        MaterialEnum.VestigeOfMadness -> R.drawable.mat_vestige

        MaterialEnum.Seed -> R.drawable.mat_seed
        MaterialEnum.GhostLantern -> R.drawable.mat_ghost_lantern
        MaterialEnum.OctupletCrystal -> R.drawable.mat_octuplet_crystal
        MaterialEnum.SerpentJewel -> R.drawable.mat_serpent_jewel
        MaterialEnum.Feather -> R.drawable.mat_feather
        MaterialEnum.Gear -> R.drawable.mat_gear
        MaterialEnum.Page -> R.drawable.mat_page
        MaterialEnum.HomunculusBaby -> R.drawable.mat_homunculus_baby
        MaterialEnum.Horseshoe -> R.drawable.mat_horseshoe
        MaterialEnum.Medal -> R.drawable.mat_medal
        MaterialEnum.ShellOfReminiscence -> R.drawable.mat_shell_of_reminiscence
        MaterialEnum.Magatama -> R.drawable.mat_magatama
        MaterialEnum.EternalIce -> R.drawable.mat_ice
        MaterialEnum.GiantRing -> R.drawable.mat_giant_ring
        MaterialEnum.AuroraSteel -> R.drawable.mat_steel
        MaterialEnum.SoundlessBell -> R.drawable.mat_bell
        MaterialEnum.Arrowhead -> R.drawable.mat_arrow
        MaterialEnum.Tiara -> R.drawable.mat_tiara
        MaterialEnum.DivineSpiritParticle -> R.drawable.mat_particle
        MaterialEnum.RainbowThreadBall -> R.drawable.mat_thread
        MaterialEnum.FantasyScales -> R.drawable.mat_fantasy_scales
        MaterialEnum.Sunscale -> R.drawable.mat_sunscale
        MaterialEnum.Converger -> R.drawable.mat_converger
        MaterialEnum.FlowerOfTheEnd -> R.drawable.mat_flower_of_the_end
        MaterialEnum.UniversalCube -> R.drawable.mat_universal_cube
        MaterialEnum.DivineLens -> R.drawable.mat_divine_lens
        MaterialEnum.HolyWaterOfDestiny -> R.drawable.mat_holy_water_of_destiny

        MaterialEnum.Claw -> R.drawable.mat_claw
        MaterialEnum.Heart -> R.drawable.mat_heart
        MaterialEnum.DragonScale -> R.drawable.mat_scale
        MaterialEnum.SpiritRoot -> R.drawable.mat_spirit_root
        MaterialEnum.YoungHorn -> R.drawable.mat_young_horn
        MaterialEnum.TearStone -> R.drawable.mat_tear_stone
        MaterialEnum.Grease -> R.drawable.mat_grease
        MaterialEnum.LampOfEvilSealing -> R.drawable.mat_lamp_of_evil_sealing
        MaterialEnum.Scarab -> R.drawable.mat_scarab
        MaterialEnum.Lanugo -> R.drawable.mat_lanugo
        MaterialEnum.Gallstone -> R.drawable.mat_gallstone
        MaterialEnum.MysteriousWine -> R.drawable.mat_mysterious_wine
        MaterialEnum.ReactorCore -> R.drawable.mat_core
        MaterialEnum.TsukumoMirror -> R.drawable.mat_mirror
        MaterialEnum.EggOfTruth -> R.drawable.mat_egg
        MaterialEnum.StarShard -> R.drawable.mat_star_shard
        MaterialEnum.FruitOfEternity -> R.drawable.mat_fruit
        MaterialEnum.DemonFlameLantern -> R.drawable.mat_demon_lantern
        MaterialEnum.GoldenCauldron -> R.drawable.mat_golden_cauldron
        MaterialEnum.MoonlightNucleus -> R.drawable.mat_moonlight_nucleus
        MaterialEnum.ReliquaryOfDepartedSoul -> R.drawable.mat_reliquary

        MaterialEnum.MonumentSaber -> R.drawable.mat_monument_saber
        MaterialEnum.MonumentArcher -> R.drawable.mat_monument_archer
        MaterialEnum.MonumentLancer -> R.drawable.mat_monument_lancer
        MaterialEnum.MonumentRider -> R.drawable.mat_monument_rider
        MaterialEnum.MonumentCaster -> R.drawable.mat_monument_caster
        MaterialEnum.MonumentAssassin -> R.drawable.mat_monument_assassin
        MaterialEnum.MonumentBerserker -> R.drawable.mat_monument_berserker

        MaterialEnum.PieceSaber -> R.drawable.mat_piece_saber
        MaterialEnum.PieceArcher -> R.drawable.mat_piece_archer
        MaterialEnum.PieceLancer -> R.drawable.mat_piece_lancer
        MaterialEnum.PieceRider -> R.drawable.mat_piece_rider
        MaterialEnum.PieceCaster -> R.drawable.mat_piece_caster
        MaterialEnum.PieceAssassin -> R.drawable.mat_piece_assassin
        MaterialEnum.PieceBerserker -> R.drawable.mat_piece_berserker

        MaterialEnum.SkillGoldSaber -> R.drawable.mat_skill_gold_saber
        MaterialEnum.SkillGoldArcher -> R.drawable.mat_skill_gold_archer
        MaterialEnum.SkillGoldLancer -> R.drawable.mat_skill_gold_lancer
        MaterialEnum.SkillGoldRider -> R.drawable.mat_skill_gold_rider
        MaterialEnum.SkillGoldCaster -> R.drawable.mat_skill_gold_caster
        MaterialEnum.SkillGoldAssassin -> R.drawable.mat_skill_gold_assassin
        MaterialEnum.SkillGoldBerserker -> R.drawable.mat_skill_gold_berserker

        MaterialEnum.SkillRedSaber -> R.drawable.mat_skill_red_saber
        MaterialEnum.SkillRedArcher -> R.drawable.mat_skill_red_archer
        MaterialEnum.SkillRedLancer -> R.drawable.mat_skill_red_lancer
        MaterialEnum.SkillRedRider -> R.drawable.mat_skill_red_rider
        MaterialEnum.SkillRedCaster -> R.drawable.mat_skill_red_caster
        MaterialEnum.SkillRedAssassin -> R.drawable.mat_skill_red_assassin
        MaterialEnum.SkillRedBerserker -> R.drawable.mat_skill_red_berserker

        MaterialEnum.SkillBlueSaber -> R.drawable.mat_skill_blue_saber
        MaterialEnum.SkillBlueArcher -> R.drawable.mat_skill_blue_archer
        MaterialEnum.SkillBlueLancer -> R.drawable.mat_skill_blue_lancer
        MaterialEnum.SkillBlueRider -> R.drawable.mat_skill_blue_rider
        MaterialEnum.SkillBlueCaster -> R.drawable.mat_skill_blue_caster
        MaterialEnum.SkillBlueAssassin -> R.drawable.mat_skill_blue_assassin
        MaterialEnum.SkillBlueBerserker -> R.drawable.mat_skill_blue_berserker
    }
