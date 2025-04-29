package net.spacetivity.blocko.phase

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.Arena
import net.spacetivity.blocko.arena.id.ArenaId
import net.spacetivity.blocko.arena.isSpectating
import net.spacetivity.blocko.countdown.GameCountdown
import net.spacetivity.blocko.phase.impl.EndingPhase
import net.spacetivity.blocko.phase.impl.IdlePhase
import net.spacetivity.blocko.phase.impl.IngamePhase
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

abstract class GamePhase(protected val arenaId: ArenaId, val name: String, val priority: Int, var countdown: GameCountdown?) {

    private val hotbarItems = mutableMapOf<Int, ItemStack>()
    private val spectatorItems = mutableMapOf<Int, ItemStack>()

    init {
        this.initPhaseHotbarItems(this.hotbarItems)
        this.initSpectatorHotbarItems(this.spectatorItems)
    }

    abstract fun start()
    abstract fun stop()
    abstract fun initPhaseHotbarItems(hotbarItems: MutableMap<Int, ItemStack>)
    abstract fun initSpectatorHotbarItems(hotbarItems: MutableMap<Int, ItemStack>)

    fun isIdle(): Boolean = this is IdlePhase
    fun isIngame(): Boolean = this is IngamePhase
    fun isEnding(): Boolean = this is EndingPhase

    fun setupPlayerInventory(player: Player) {
        clearPlayerInventory(player)

        for (entry in if (player.isSpectating()) this.spectatorItems.entries else this.hotbarItems.entries) {
            player.inventory.setItem(entry.key, entry.value)
        }
    }

    fun clearPlayerInventory(player: Player) {
        player.inventory.clear()
        player.exp = 0.0F
        player.level = 0
    }

    protected fun getArena(): Arena {
        return BlockoGame.instance.arenaHandler.getArena(this.arenaId)!!
    }

}