package net.spacetivity.ludo.dice

import com.destroystokyo.paper.profile.PlayerProfile
import com.destroystokyo.paper.profile.ProfileProperty
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.extensions.*
import net.spacetivity.ludo.player.GamePlayer
import net.spacetivity.ludo.utils.HeadUtils
import net.spacetivity.ludo.utils.ItemUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
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
                for (gamePlayer: GamePlayer in gameArena.currentPlayers) {
                    if (!gamePlayer.isDicing()) continue
                    rollDice(gamePlayer)
                }
            }
        }, 0L, 4L)
    }

    fun stopDiceAnimation() {
        if (this.diceAnimationTask != null) this.diceAnimationTask!!.cancel()
    }

    fun getDiceItem(player: Player) {
        player.inventory.setItem(4, ItemUtils(Material.PLAYER_HEAD)
            .setOwner(this.diceSides[1]!!)
            .setName(getDiceDisplayName(1))
            .build())
    }

    fun getDiceItem(): ItemStack {
        return ItemUtils(Material.PLAYER_HEAD)
            .setOwner(this.diceSides[1]!!)
            .setName(getDiceDisplayName(1))
            .build()
    }

    fun startDicing(gamePlayer: GamePlayer) {
        if (gamePlayer.isDicing()) {
            gamePlayer.sendMessage(Component.text("You are already dicing!"))
            return
        }

        this.dicingPlayers[gamePlayer.uuid] = DiceSession(1)
    }

    fun stopDicing(gamePlayer: GamePlayer) {
        if (!gamePlayer.isDicing()) {
            gamePlayer.sendMessage(Component.text("You are not dicing!"))
            return
        }

        val diceSession: DiceSession = this.dicingPlayers[gamePlayer.uuid] ?: return
        val dicedNumber: Int = diceSession.currentDiceNumber

        this.dicingPlayers.remove(gamePlayer.uuid)

        gamePlayer.dicedNumber = dicedNumber

        gamePlayer.clearSlot(4)
        gamePlayer.playSound(Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM)
        gamePlayer.sendActionBar(Component.text("You diced: $dicedNumber", NamedTextColor.GREEN, TextDecoration.BOLD))
    }

    private fun rollDice(gamePlayer: GamePlayer) {
        if (!gamePlayer.isDicing()) return

        val storageContents: Array<ItemStack?> = gamePlayer.accessStorageContents() ?: return
        val itemStack: ItemStack = storageContents.find { it != null && it.type == Material.PLAYER_HEAD } ?: return
        val skullMeta: SkullMeta = itemStack.itemMeta as SkullMeta

        val blockNumber: Int? = gamePlayer.getCurrentDiceNumber()
        val diceSide: Pair<Int, String> = getDiceSide(blockNumber)

        gamePlayer.playSound(Sound.BLOCK_BAMBOO_BREAK)
        gamePlayer.setCurrentDiceNumber(diceSide.first)
        gamePlayer.sendActionBar(Component.text("Current number: ${diceSide.first}", NamedTextColor.AQUA, TextDecoration.BOLD))

        val diceProfile: PlayerProfile = Bukkit.createProfile(UUID.randomUUID().toString().split("-")[0])
        diceProfile.setProperty(ProfileProperty("textures", diceSide.second))

        skullMeta.playerProfile = diceProfile
        skullMeta.displayName(getDiceDisplayName(diceSide.first))
        itemStack.itemMeta = skullMeta
    }

    private fun getDiceDisplayName(diceNumber: Int): Component {
        return Component.text("$diceNumber", NamedTextColor.YELLOW, TextDecoration.BOLD)
    }

    private fun getDiceSide(blockedDiceNumber: Number?): Pair<Int, String> {
        val randomNumber: Int = ThreadLocalRandom.current().nextInt(1, 7)
        val diceSide: Pair<Int, String>? = this.diceSides.entries.find { it.key == randomNumber }?.toPair()

        if (diceSide == null) {
            Bukkit.getConsoleSender().sendMessage(Component.text("ERROR (Check dice_sides.json! No dice side for number $randomNumber found...", NamedTextColor.DARK_RED))
            return Pair(1, HeadUtils.DICE_ONE)
        }

        if (blockedDiceNumber != null && diceSide.first == blockedDiceNumber) return getDiceSide(blockedDiceNumber)
        return diceSide
    }

}