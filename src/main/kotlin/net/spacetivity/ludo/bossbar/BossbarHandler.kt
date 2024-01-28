package net.spacetivity.ludo.bossbar

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.bossbar.BossBar.Color
import net.kyori.adventure.bossbar.BossBar.Overlay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import org.bukkit.entity.Player
import java.util.*

class BossbarHandler {

    private val cachedBossbars: Multimap<UUID, Pair<String, BossBar>> = ArrayListMultimap.create()

    fun getBossbar(uuid: UUID, key: String): BossBar? {
        return this.cachedBossbars[uuid]
            .filter { it.first.equals(key, true) }
            .map { it.second }
            .firstOrNull()
    }

    fun getBossbars(uuid: UUID): List<Pair<String, BossBar>> {
        return this.cachedBossbars[uuid].toList()
    }

    fun updateBossbar(uuid: UUID, key: String, updateType: BossBarUpdate, newValue: Any) {
        val bossbar: BossBar = getBossbar(uuid, key) ?: return
        when (updateType) {
            BossBarUpdate.NAME -> bossbar.name(newValue as ComponentLike)
            BossBarUpdate.PROGRESS -> bossbar.progress(newValue as Float)
            BossBarUpdate.COLOR -> bossbar.color(newValue as Color)
        }
    }

    fun registerBossbar(player: Player, key: String, name: Component, progress: Float, color: Color, style: Overlay) {
        val bossbar: BossBar = BossBar.bossBar(name, progress, color, style)
        this.cachedBossbars.put(player.uniqueId, Pair(key, bossbar))
        player.showBossBar(bossbar)
    }

    fun unregisterBossbar(player: Player, key: String) {
        val bossbar: BossBar = getBossbar(player.uniqueId, key) ?: return
        this.cachedBossbars.entries().removeIf { it.key == player.uniqueId && it.value.first.equals(key, true) }
        player.hideBossBar(bossbar)
    }

    fun clearBossbars(player: Player) {
        getBossbars(player.uniqueId).forEach { unregisterBossbar(player, it.first) }
    }

    enum class BossBarUpdate {
        NAME,
        PROGRESS,
        COLOR
    }

}