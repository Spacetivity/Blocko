package net.spacetivity.ludo.phase

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.countdown.GameCountdown
import net.spacetivity.ludo.phase.impl.EndingPhase
import net.spacetivity.ludo.phase.impl.IdlePhase
import net.spacetivity.ludo.phase.impl.IngamePhase
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

abstract class GamePhase(protected val arenaId: String, val name: String, val priority: Int, var countdown: GameCountdown?) {

    private val hotbarItems: MutableMap<Int, ItemStack> = mutableMapOf()

    init {
        this.initPhaseHotbarItems(this.hotbarItems)
    }

    abstract fun start()
    abstract fun stop()
    abstract fun initPhaseHotbarItems(hotbarItems: MutableMap<Int, ItemStack>)

    fun isIdle(): Boolean = this is IdlePhase
    fun isIngame(): Boolean = this is IngamePhase
    fun isEnding(): Boolean = this is EndingPhase

    fun setupPlayerInventory(player: Player) {
        clearPlayerInventory(player)

        for (entry: MutableMap.MutableEntry<Int, ItemStack> in this.hotbarItems.entries) {
            player.inventory.setItem(entry.key, entry.value)
        }
    }

    fun clearPlayerInventory(player: Player) {
        player.inventory.clear()
        player.exp = 0.0F
        player.level = 0
    }

    protected fun getArenaPlayers(): MutableList<Player> {
        val players: MutableList<Player> = mutableListOf()

        for (uuid: UUID in getArena().currentPlayers) {
            val player: Player = Bukkit.getPlayer(uuid) ?: continue
            players.add(player)
        }

        return players
    }

    protected fun getArena(): GameArena {
        return LudoGame.instance.gameArenaHandler.getArena(this.arenaId)!!
    }

}