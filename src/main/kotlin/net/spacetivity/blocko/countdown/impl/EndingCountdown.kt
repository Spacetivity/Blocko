package net.spacetivity.blocko.countdown.impl

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.id.ArenaId
import net.spacetivity.blocko.countdown.GameCountdown
import net.spacetivity.blocko.stats.StatsType
import net.spacetivity.blocko.stats.UpdateOperation
import net.spacetivity.blocko.stats.addCoins
import org.bukkit.Sound
import org.bukkit.scheduler.BukkitTask

class EndingCountdown(arenaId: ArenaId) : GameCountdown(arenaId, BlockoGame.instance.globalConfigFile.endingCountdownSeconds) {

    override fun handleCountdownIdle(countdownTask: BukkitTask, remainingSeconds: Int) {
        val arena = BlockoGame.instance.arenaHandler.getArena(this.arenaId) ?: return
        val isOne = remainingSeconds == 1

        arena.sendArenaMessage("blocko.countdown.ending.running",
            Placeholder.parsed("time", (if (isOne) "one" else remainingSeconds).toString()),
            Placeholder.parsed("time_string", if (isOne) "second" else "seconds"))

        arena.sendArenaSound(Sound.ENTITY_PLAYER_LEVELUP,0.2F)
    }

    override fun handleCountdownEnd() {
        val arena = BlockoGame.instance.arenaHandler.getArena(this.arenaId) ?: return

        for (gamePlayer in arena.currentPlayers.filter { !it.isAI }) {
            val lobbySpawn = BlockoGame.instance.lobbySpawnHandler.lobbySpawn
            if (lobbySpawn != null) gamePlayer.toBukkitInstance()?.teleport(lobbySpawn.toBukkitInstance())

            val statsPlayer = BlockoGame.instance.statsPlayerHandler.getStatsPlayer(gamePlayer.uuid) ?: continue
            statsPlayer.update(StatsType.PLAYED_GAMES, UpdateOperation.INCREASE, 1)

            gamePlayer.addCoins(60, false)
            gamePlayer.matchStats.gainedCoins += 60
        }

        arena.sendArenaMessage("blocko.countdown.ending.end")
        arena.reset(false)
    }

}