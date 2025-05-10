package net.spacetivity.blocko.dice

import com.destroystokyo.paper.profile.ProfileProperty
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.item.itemStack
import net.spacetivity.blocko.item.meta
import net.spacetivity.blocko.item.name
import net.spacetivity.blocko.phase.GamePhaseMode
import net.spacetivity.blocko.phase.impl.IngamePhase
import net.spacetivity.blocko.player.*
import net.spacetivity.blocko.scoreboard.ScoreboardUtils
import net.spacetivity.blocko.translation.translateActionBar
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.Constants
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.scheduler.BukkitTask
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class DiceHandler {

    val dicingPlayers = mutableMapOf<UUID, DiceSession>()

    private val diceSides = Blocko.instance.diceSidesFile.diceSides
    private var diceAnimationTask: BukkitTask? = null

    fun startDiceAnimation() {
        this.diceAnimationTask = Bukkit.getScheduler().runTaskTimer(Blocko.instance, Runnable {
            for (gameArena in Blocko.instance.arenaHandler.cachedArenas) {
                if (!gameArena.phase.isIngame()) continue
                val ingamePhase = gameArena.phase as IngamePhase

                for (gamePlayer in gameArena.currentPlayers) {
                    if (!ingamePhase.isInControllingTeam(gamePlayer.uuid)) continue
                    if (!gamePlayer.isDicing()) continue
                    val diceSession = gamePlayer.getDiceSession() ?: continue

                    val endTimestamp = diceSession.dicingEndTimestamp
                    val currentTimestamp = System.currentTimeMillis()

                    if (currentTimestamp >= endTimestamp) {
                        ingamePhase.phaseMode = GamePhaseMode.PICK_ENTITY

                        val dicedNumber = diceSession.currentDiceNumber

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
        val profile = Bukkit.createProfile(UUID.randomUUID().toString().split("-")[0])
        profile.setProperty(ProfileProperty("textures", this.diceSides[1]!!))

        return itemStack(Material.PLAYER_HEAD) {
            meta<SkullMeta> {
                name = getDiceDisplayName(1)
                playerProfile = profile
            }
        }
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

        val blockNumber = diceSession.currentDiceNumber
        val diceSide = getDiceSide(blockNumber)

        if (!gamePlayer.isAI) {
            val storageContents = gamePlayer.accessStorageContents() ?: return
            val itemStack = storageContents.find { it != null && it.type == Material.PLAYER_HEAD } ?: return
            val skullMeta = itemStack.itemMeta as SkullMeta
            val diceProfile = Bukkit.createProfile(UUID.randomUUID().toString().split("-")[0])
            diceProfile.setProperty(ProfileProperty("textures", diceSide.second))

            skullMeta.playerProfile = diceProfile
            skullMeta.displayName(getDiceDisplayName(diceSide.first))
            itemStack.itemMeta = skullMeta
        }

        Blocko.instance.arenaHandler.getArena(gamePlayer.arenaId)?.sendArenaSound(Sound.BLOCK_BAMBOO_BREAK, 0.2F)
        diceSession.currentDiceNumber = diceSide.first

        gamePlayer.translateActionBar("blocko.main_game_loop.current_dice_number", Placeholder.parsed("dice_number", diceSide.first.toString()))

        ScoreboardUtils.updateDicedNumberLine(gamePlayer.arenaId, diceSession.currentDiceNumber)
    }

    private fun getDiceDisplayName(diceNumber: Int): Component {
        val translation = Blocko.instance.translationHandler.getSelectedTranslation()
        return translation.line("blocko.main_game_loop.dice_display_name", Placeholder.parsed("dice_number", diceNumber.toString()))
    }

    private fun getDiceSide(blockedDiceNumber: Number): Pair<Int, String> {
        val randomNumber = ThreadLocalRandom.current().nextInt(1, 7)
        val diceSide = this.diceSides.entries.find { it.key == randomNumber }?.toPair()

        if (diceSide == null) {
            val translation = Blocko.instance.translationHandler.getSelectedTranslation()
            Bukkit.getConsoleSender().sendMessage(translation.line("blocko.main_game_loop.dice_error", Placeholder.parsed("number", randomNumber.toString())))
            return Pair(1, Constants.DICE_ONE_SKULL)
        }

        if (diceSide.first == blockedDiceNumber) return getDiceSide(blockedDiceNumber)
        return diceSide
    }

}