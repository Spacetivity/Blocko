package net.spacetivity.ludo.team

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.utils.MetadataUtils
import org.bukkit.entity.LivingEntity

class GameTeamHandler {

    val gameTeams: Multimap<String, GameTeam> = ArrayListMultimap.create()

    init {
        for (gameArena: GameArena in LudoGame.instance.gameArenaHandler.cachedArenas) {
            addTeam(gameArena.id, GameTeam("red", NamedTextColor.RED))
            addTeam(gameArena.id, GameTeam("green", NamedTextColor.GREEN))
            addTeam(gameArena.id, GameTeam("blue", NamedTextColor.BLUE))
            addTeam(gameArena.id, GameTeam("yellow", NamedTextColor.YELLOW))
        }
    }

    fun addTeam(arenaId: String, gameTeam: GameTeam) {
        this.gameTeams.put(arenaId, gameTeam)
    }

    fun getTeamOfEntity(arenaId: String, entity: LivingEntity): GameTeam? {
        val teamName: String = MetadataUtils.get<String>(entity, "teamName") ?: return null
        return getTeam(arenaId, teamName)
    }

    fun getTeam(arenaId: String, name: String): GameTeam? {
        return this.gameTeams.get(arenaId).find { it.name.equals(name, true) }
    }

}