package net.spacetivity.blocko.dice

import com.destroystokyo.paper.profile.PlayerProfile
import com.destroystokyo.paper.profile.ProfileProperty
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.extensions.*
import net.spacetivity.blocko.phase.GamePhaseMode
import net.spacetivity.blocko.phase.impl.IngamePhase
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.scoreboard.GameScoreboardUtils
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.utils.HeadUtils
import net.spacetivity.blocko.utils.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.scheduler.BukkitTask
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class DiceHandler {

    val dicingPlayers: MutableMap<UUID, DiceSession> = mutableMapOf()

    private val diceSides: MutableMap<Int, String> = BlockoGame.instance.diceSidesFile.diceSides
    private var diceAnimationTask: BukkitTask? = null

    fun startDiceAnimation() {
        this.diceAnimationTask = Bukkit.getScheduler().runTaskTimer(BlockoGame.instance, Runnable {
            for (gameArena: GameArena in BlockoGame.instance.gameArenaHandler.cachedArenas) {
                if (!gameArena.phase.isIngame()) continue
                val ingamePhase: IngamePhase = gameArena.phase as IngamePhase

                for (gamePlayer: GamePlayer in gameArena.currentPlayers) {
                    if (!ingamePhase.isInControllingTeam(gamePlayer.uuid)) continue
                    if (!gamePlayer.isDicing()) continue
                    val diceSession: DiceSession = gamePlayer.getDiceSession() ?: continue

                    val endTimestamp: Long = diceSession.dicingEndTimestamp
                    val currentTimestamp: Long = System.currentTimeMillis()

                    if (currentTimestamp >= endTimestamp) {
                        ingamePhase.phaseMode = GamePhaseMode.PICK_ENTITY

                        val dicedNumber: Int = diceSession.currentDiceNumber

                        gamePlayer.dicedNumber = dicedNumber
                        gamePlayer.playSound(Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM)
                        gamePlayer.translateActionBar("blocko.main_game_loop.diced_number", Placeholder.parsed("diced_number", dicedNumber.toString()))

                        this.dicingPlayers.remove(gamePlayer.uuid)

                        if (this.dicingPlayers.containsKey(gamePlayer.uuid))
                            this.dicingPlayers.remove(gamePlayer.uuid, diceSession)

                        continue
                    }

                    rollDice(gamePlayer, diceSession)
                }
            }
        }, 0L, 4L)
    }

    fun stopDiceAnimation() {
        if (this.diceAnimationTask != null) {
            this.diceAnimationTask!!.cancel()
            this.diceAnimationTask = null
        }
    }

    fun getDiceItem(): ItemStack {
        return ItemBuilder(Material.PLAYER_HEAD)
            .setOwner(this.diceSides[1]!!)
            .setName(getDiceDisplayName(1))
            .build()
    }

    fun startDicing(gamePlayer: GamePlayer, ingamePhase: IngamePhase) {
        if (!ingamePhase.isInControllingTeam(gamePlayer.uuid)) return

        if (gamePlayer.isDicing()) {
            gamePlayer.translateMessage("blocko.main_game_loop.already_dicing")
            return
        }

        this.dicingPlayers[gamePlayer.uuid] = DiceSession(1, System.currentTimeMillis() + (1000 * 2))
    }

    private fun rollDice(gamePlayer: GamePlayer, diceSession: DiceSession) {
        if (!gamePlayer.isDicing()) return

        val blockNumber: Int = diceSession.currentDiceNumber
        val diceSide: Pair<Int, String> = getDiceSide(blockNumber)

        if (!gamePlayer.isAI) {
            val storageContents: Array<ItemStack?> = gamePlayer.accessStorageContents() ?: return
            val itemStack: ItemStack = storageContents.find { it != null && it.type == Material.PLAYER_HEAD } ?: return
            val skullMeta: SkullMeta = itemStack.itemMeta as SkullMeta
            val diceProfile: PlayerProfile = Bukkit.createProfile(UUID.randomUUID().toString().split("-")[0])
            diceProfile.setProperty(ProfileProperty("textures", diceSide.second))

            skullMeta.playerProfile = diceProfile
            skullMeta.displayName(getDiceDisplayName(diceSide.first))
            itemStack.itemMeta = skullMeta
        }

        BlockoGame.instance.gameArenaHandler.getArena(gamePlayer.arenaId)?.sendArenaSound(Sound.BLOCK_BAMBOO_BREAK, 0.2F)
        diceSession.currentDiceNumber = diceSide.first

        gamePlayer.translateActionBar("blocko.main_game_loop.current_dice_number", Placeholder.parsed("dice_number", diceSide.first.toString()))

        GameScoreboardUtils.updateDicedNumberLine(gamePlayer.arenaId, diceSession.currentDiceNumber)
    }

    private fun getDiceDisplayName(diceNumber: Int): Component {
        val translation: Translation = BlockoGame.instance.translationHandler.getSelectedTranslation()
        return translation.validateLine("blocko.main_game_loop.dice_display_name", Placeholder.parsed("dice_number", diceNumber.toString()))
    }

    private fun getDiceSide(blockedDiceNumber: Number): Pair<Int, String> {
        val randomNumber: Int = ThreadLocalRandom.current().nextInt(1, 7)
        val diceSide: Pair<Int, String>? = this.diceSides.entries.find { it.key == randomNumber }?.toPair()

        if (diceSide == null) {
            val translation: Translation = BlockoGame.instance.translationHandler.getSelectedTranslation()
            Bukkit.getConsoleSender().sendMessage(translation.validateLine("blocko.main_game_loop.dice_error", Placeholder.parsed("number", randomNumber.toString())))
            return Pair(1, HeadUtils.DICE_ONE)
        }

        if (diceSide.first == blockedDiceNumber) return getDiceSide(blockedDiceNumber)
        return diceSide
    }

}