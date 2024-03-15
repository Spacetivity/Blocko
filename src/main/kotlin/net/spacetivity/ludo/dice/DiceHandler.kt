package net.spacetivity.ludo.dice

import com.destroystokyo.paper.profile.PlayerProfile
import com.destroystokyo.paper.profile.ProfileProperty
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.extensions.*
import net.spacetivity.ludo.phase.GamePhaseMode
import net.spacetivity.ludo.phase.impl.IngamePhase
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.scoreboard.GameScoreboardUtils
import net.spacetivity.ludo.utils.HeadUtils
import net.spacetivity.ludo.utils.ItemBuilder
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

    private val diceSides: MutableMap<Int, String> = LudoGame.instance.diceSidesFile.diceSides
    private var diceAnimationTask: BukkitTask? = null

    fun startDiceAnimation() {
        this.diceAnimationTask = Bukkit.getScheduler().runTaskTimer(LudoGame.instance, Runnable {
            for (gameArena: GameArena in LudoGame.instance.gameArenaHandler.cachedArenas) {
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
                        gamePlayer.sendActionBar(Component.text("You diced: $dicedNumber", NamedTextColor.GREEN, TextDecoration.BOLD))

                        this.dicingPlayers.remove(gamePlayer.uuid)

                        if (this.dicingPlayers.containsKey(gamePlayer.uuid))
                            this.dicingPlayers.remove(gamePlayer.uuid, diceSession)

                        continue
                    }

                    rollDice(gamePlayer, diceSession)
                }
            }
        }, 0L, 5L)
    }

    fun stopDiceAnimation() {
        if (this.diceAnimationTask != null) {
            this.diceAnimationTask!!.cancel()
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
            gamePlayer.sendMessage(Component.text("You are already dicing!"))
            return
        }

        if (this.dicingPlayers.isNotEmpty()) {
            val stillDicing: MutableList<GamePlayer> = mutableListOf()

            for (uuid in this.dicingPlayers.keys) {
                val currentGp = LudoGame.instance.gameArenaHandler.getArena(gamePlayer.arenaId)!!.currentPlayers.find { it.uuid == uuid }
                    ?: continue
                stillDicing.add(currentGp)
            }

            Bukkit.broadcast(Component.text("ERROR!!!! THERE IS ALREADY A DICING PLAYER >> ${this.dicingPlayers.keys} ${stillDicing.map { it.teamName }}"))
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

        LudoGame.instance.gameArenaHandler.getArena(gamePlayer.arenaId)?.sendArenaSound(Sound.BLOCK_BAMBOO_BREAK, 0.2F)
        diceSession.currentDiceNumber = diceSide.first

        gamePlayer.sendActionBar(Component.text("Current number: ${diceSide.first}", NamedTextColor.AQUA, TextDecoration.BOLD))

        GameScoreboardUtils.updateDicedNumberLine(gamePlayer.arenaId, diceSession.currentDiceNumber)
    }

    private fun getDiceDisplayName(diceNumber: Int): Component {
        return Component.text("$diceNumber", NamedTextColor.YELLOW, TextDecoration.BOLD)
    }

    private fun getDiceSide(blockedDiceNumber: Number): Pair<Int, String> {
        val randomNumber: Int = ThreadLocalRandom.current().nextInt(1, 7)
        val diceSide: Pair<Int, String>? = this.diceSides.entries.find { it.key == randomNumber }?.toPair()

        if (diceSide == null) {
            Bukkit.getConsoleSender().sendMessage(Component.text("ERROR (Check dice_sides.json! No dice side for number $randomNumber found...", NamedTextColor.DARK_RED))
            return Pair(1, HeadUtils.DICE_ONE)
        }

        if (diceSide.first == blockedDiceNumber) return getDiceSide(blockedDiceNumber)
        return diceSide
    }

}