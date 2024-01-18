package net.spacetivity.ludo.dice

import com.destroystokyo.paper.profile.PlayerProfile
import com.destroystokyo.paper.profile.ProfileProperty
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.extensions.getCurrentDiceNumber
import net.spacetivity.ludo.extensions.isDicing
import net.spacetivity.ludo.extensions.setCurrentDiceNumber
import net.spacetivity.ludo.utils.MetadataUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.scheduler.BukkitTask
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class DiceHandler {

    val dicingPlayers: MutableMap<UUID, DiceSession> = mutableMapOf()

    private val diceFileHandler: DiceFileHandler = DiceFileHandler()
    private var diceAnimationTask: BukkitTask? = null

    fun startDiceAnimation() {
        this.diceAnimationTask = Bukkit.getScheduler().runTaskTimer(LudoGame.instance, Runnable {
            for (player in Bukkit.getOnlinePlayers()) {
                if (!player.isDicing()) continue
                roll(player)
            }
        }, 0L, 10L)
    }

    fun stopDiceAnimation() {
        if (this.diceAnimationTask != null) this.diceAnimationTask!!.cancel()
    }

    fun startDicing(uuid: UUID) {
        this.dicingPlayers[uuid] = DiceSession(1)
    }

    fun stopDicing(player: Player) {
        val diceSession: DiceSession = this.dicingPlayers[player.uniqueId] ?: return
        val dicedNumber: Int =  diceSession.currentDiceNumber
        MetadataUtils.set(player, "dicedNumber", dicedNumber)
        this.dicingPlayers.remove(player.uniqueId)
    }

    private fun roll(player: Player) {
        if (!player.isDicing()) return

        val storageContents: Array<ItemStack?> = player.inventory.storageContents
        val itemStack: ItemStack = storageContents.find { it != null && it.type == Material.PLAYER_HEAD } ?: return
        val skullMeta: SkullMeta = itemStack.itemMeta as SkullMeta

        val blockNumber: Int? = player.getCurrentDiceNumber()
        val diceSide: Pair<Int, String> = getDiceSide(player, blockNumber)

        player.setCurrentDiceNumber(diceSide.first)

        val diceProfile: PlayerProfile = Bukkit.createProfile(UUID.randomUUID().toString().split("-")[0])
        diceProfile.setProperty(ProfileProperty("textures", diceSide.second))

        skullMeta.playerProfile = diceProfile
        itemStack.itemMeta = skullMeta
    }

    private fun getDiceSide(player: Player, blockedDiceNumber: Number?): Pair<Int, String> {
        val diceSide: Pair<Int, String> = this.diceFileHandler.diceSides[ThreadLocalRandom.current().nextInt(1, 6)]
        if (blockedDiceNumber != null && diceSide.first == blockedDiceNumber) return getDiceSide(player, blockedDiceNumber)
        return diceSide
    }

}