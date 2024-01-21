package net.spacetivity.ludo.dice

import com.destroystokyo.paper.profile.PlayerProfile
import com.destroystokyo.paper.profile.ProfileProperty
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.extensions.getCurrentDiceNumber
import net.spacetivity.ludo.extensions.isDicing
import net.spacetivity.ludo.extensions.setCurrentDiceNumber
import net.spacetivity.ludo.utils.HeadUtils
import net.spacetivity.ludo.utils.ItemUtils
import net.spacetivity.ludo.utils.MetadataUtils
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
            for (player in Bukkit.getOnlinePlayers()) {
                if (!player.isDicing()) continue
                roll(player)
            }
        }, 0L, 4L)
    }

    fun stopDiceAnimation() {
        if (this.diceAnimationTask != null) this.diceAnimationTask!!.cancel()
    }

    fun startDicing(player: Player) {
        if (player.isDicing()) {
            player.sendMessage(Component.text("You are already dicing!"))
            return
        }

        player.inventory.setItem(4, ItemUtils(Material.PLAYER_HEAD)
            .setOwner(this.diceSides[1]!!)
            .setName(getDiceDisplayName(1))
            .build())

        this.dicingPlayers[player.uniqueId] = DiceSession(1)
    }

    fun stopDicing(player: Player) {
        if (!player.isDicing()) {
            player.sendMessage(Component.text("You are not dicing!"))
            return
        }

        val diceSession: DiceSession = this.dicingPlayers[player.uniqueId] ?: return
        val dicedNumber: Int = diceSession.currentDiceNumber

        this.dicingPlayers.remove(player.uniqueId)

        MetadataUtils.set(player, "dicedNumber", dicedNumber)
        player.inventory.clear(4)
        player.playSound(player.location, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 10F, 1F)
        player.sendActionBar(Component.text("You diced: $dicedNumber", NamedTextColor.GREEN, TextDecoration.BOLD))
    }

    private fun roll(player: Player) {
        if (!player.isDicing()) return

        val storageContents: Array<ItemStack?> = player.inventory.storageContents
        val itemStack: ItemStack = storageContents.find { it != null && it.type == Material.PLAYER_HEAD } ?: return
        val skullMeta: SkullMeta = itemStack.itemMeta as SkullMeta

        val blockNumber: Int? = player.getCurrentDiceNumber()
        val diceSide: Pair<Int, String> = getDiceSide(player, blockNumber)

        player.playSound(player.location, Sound.BLOCK_BAMBOO_BREAK, 10F, 1F)
        player.setCurrentDiceNumber(diceSide.first)
        player.sendActionBar(Component.text("Current number: ${diceSide.first}", NamedTextColor.AQUA, TextDecoration.BOLD))

        val diceProfile: PlayerProfile = Bukkit.createProfile(UUID.randomUUID().toString().split("-")[0])
        diceProfile.setProperty(ProfileProperty("textures", diceSide.second))

        skullMeta.playerProfile = diceProfile
        skullMeta.displayName(getDiceDisplayName(diceSide.first))
        itemStack.itemMeta = skullMeta
    }

    private fun getDiceDisplayName(diceNumber: Int): Component {
        return Component.text("$diceNumber", NamedTextColor.YELLOW, TextDecoration.BOLD)
    }

    private fun getDiceSide(player: Player, blockedDiceNumber: Number?): Pair<Int, String> {
        val randomNumber: Int = ThreadLocalRandom.current().nextInt(1, 7)
        val diceSide: Pair<Int, String>? = this.diceSides.entries.find { it.key == randomNumber }?.toPair()

        if (diceSide == null) {
            Bukkit.getConsoleSender().sendMessage(Component.text("ERROR (Check dice_sides.json! No dice side for number $randomNumber found...", NamedTextColor.DARK_RED))
            return Pair(1, HeadUtils.DICE_ONE)
        }

        if (blockedDiceNumber != null && diceSide.first == blockedDiceNumber) return getDiceSide(player, blockedDiceNumber)
        return diceSide
    }

}