package net.spacetivity.ludo.team

import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.utils.MetadataUtils
import net.spacetivity.ludo.utils.const.Heads
import org.bukkit.entity.LivingEntity

class GameTeamHandler {

    val gameTeams: MutableSet<GameTeam> = mutableSetOf()

    init {
        this.gameTeams.add(GameTeam("red", NamedTextColor.RED, Heads.RED))
        this.gameTeams.add(GameTeam("green", NamedTextColor.GREEN, Heads.GREEN))
        this.gameTeams.add(GameTeam("blue", NamedTextColor.BLUE, Heads.BLUE))
        this.gameTeams.add(GameTeam("yellow", NamedTextColor.YELLOW, Heads.YELLOW))
    }

    fun getTeamOfEntity(entity: LivingEntity): GameTeam? {
        val teamName: String = MetadataUtils.get<String>(entity, "teamName") ?: return null
        return getTeam(teamName)
    }

    fun getTeam(name: String): GameTeam? = this.gameTeams.find { it.name == name }

}