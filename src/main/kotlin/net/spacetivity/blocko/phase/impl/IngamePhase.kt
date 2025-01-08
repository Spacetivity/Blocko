package net.spacetivity.blocko.phase.impl

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.achievement.AchievementHandler
import net.spacetivity.blocko.achievement.impl.RushExpertAchievement
import net.spacetivity.blocko.achievement.impl.WinMonsterAchievement
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.extensions.getArena
import net.spacetivity.blocko.extensions.playSound
import net.spacetivity.blocko.extensions.toStatsPlayerInstance
import net.spacetivity.blocko.extensions.translateMessage
import net.spacetivity.blocko.phase.GamePhase
import net.spacetivity.blocko.phase.GamePhaseMode
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.scoreboard.GameScoreboardUtils
import net.spacetivity.blocko.stats.StatsPlayer
import net.spacetivity.blocko.team.GameTeam
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.blocko.utils.ItemBuilder
import net.spacetivity.inventory.api.SpaceInventoryProvider
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import java.time.Duration
import java.util.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class IngamePhase(arenaId: String) : GamePhase(arenaId, "ingame", 1, null) {

    var lastControllingTeamId: Int? = null
    var controllingTeamId: Int? = null
    var phaseMode: GamePhaseMode = GamePhaseMode.DICE
    var matchStartTime: Long? = null

    override fun start() {
        BlockoGame.instance.gameArenaSignHandler.updateArenaSign(getArena())

        this.phaseMode = GamePhaseMode.DICE

        if (this.matchStartTime == null) this.matchStartTime = System.currentTimeMillis()

        for (gamePlayer: GamePlayer in getArena().currentPlayers.filter { !it.isAI }) {
            val player: Player = gamePlayer.toBukkitInstance() ?: continue
            setupPlayerInventory(player)
        }
    }

    override fun stop() {
        for (gamePlayer: GamePlayer in getArena().currentPlayers) {
            val player: Player = gamePlayer.toBukkitInstance() ?: return

            val statsPlayer: StatsPlayer? = gamePlayer.toStatsPlayerInstance()
            if (statsPlayer != null) statsPlayer.wonGames += 1

            BlockoGame.instance.bossbarHandler.unregisterBossbar(player, "timeoutBar")

            val achievementHandler: AchievementHandler = BlockoGame.instance.achievementHandler
            achievementHandler.getAchievement(RushExpertAchievement::class.java)?.grantIfCompletedBy(gamePlayer)
            achievementHandler.getAchievement(WinMonsterAchievement::class.java)?.grantIfCompletedBy(gamePlayer)

            val matchDuration: kotlin.time.Duration = (System.currentTimeMillis() - this.matchStartTime!!).toDuration(DurationUnit.MILLISECONDS)

            matchDuration.toComponents { hours, minutes, seconds, _ ->
                val hoursString: String = if (hours in 0..9) "0$hours" else hours.toString()
                val minutesString: String = if (minutes in 0..9) "0$minutes" else minutes.toString()
                val secondsString: String = if (seconds in 0..9) "0$seconds" else seconds.toString()

                val timeString = "$hoursString:$minutesString:$secondsString"

                val lastPosition: Int = getArena().teamOptions.playerCount
                val positionString: String = if (gamePlayer.matchStats.position == null) lastPosition.toString() else gamePlayer.matchStats.position!!.toString()

                gamePlayer.toBukkitInstance()?.translateMessage("blocko.stats.show_match_stats",
                    Placeholder.parsed("eliminations", gamePlayer.matchStats.eliminations.toString()),
                    Placeholder.parsed("knockouts", gamePlayer.matchStats.knockedOutByOpponent.toString()),
                    Placeholder.parsed("coins", gamePlayer.matchStats.gainedCoins.toString()),
                    Placeholder.parsed("place", positionString),
                    Placeholder.parsed("time", timeString))
            }
        }

        this.lastControllingTeamId = null
        this.controllingTeamId = null
        this.matchStartTime = null
    }

    override fun initPhaseHotbarItems(hotbarItems: MutableMap<Int, ItemStack>) {
        val translation: Translation = BlockoGame.instance.translationHandler.getSelectedTranslation()

        hotbarItems[0] = BlockoGame.instance.diceHandler.getDiceItem()

        for ((entityIndex, i) in (1..4).withIndex()) {
            hotbarItems[i] = ItemBuilder(Material.ARMOR_STAND)
                .setName(translation.validateItemName("blocko.main_game_loop.entity_selector_display_name", Placeholder.parsed("count", (entityIndex + 1).toString())))
                .setData("entitySelector", entityIndex)
                .build()
        }

        hotbarItems[7] = ItemBuilder(Material.CLOCK)
            .setName(translation.validateItemName("blocko.items.profile.display_name"))
            .setLoreByComponent(translation.validateItemLore("blocko.items.profile.lore"))
            .onInteract { event: PlayerInteractEvent ->
                val player: Player = event.player
                InventoryUtils.openProfileInventory(player, false)
            }
            .build()

        hotbarItems[8] = ItemBuilder(Material.SLIME_BALL)
            .setName(translation.validateItemName("blocko.items.leave.display_name"))
            .onInteract { event: PlayerInteractEvent ->
                val player: Player = event.player
                val gameArena: GameArena = player.getArena() ?: return@onInteract

                SpaceInventoryProvider.api.openConfirmationInventory(
                    player,
                    translation.validateItemName("blocko.inventory.leave.title"),
                    ItemBuilder(Material.OAK_DOOR).setName(translation.validateItemName("blocko.inventory.leave.display_item.display_name")).build(),
                    {
                        gameArena.quit(player)
                    },
                    {
                        player.closeInventory()
                    }
                )
            }
            .build()

    }

    override fun initSpectatorHotbarItems(hotbarItems: MutableMap<Int, ItemStack>) {
        val translation: Translation = BlockoGame.instance.translationHandler.getSelectedTranslation()

        hotbarItems[8] = ItemBuilder(Material.SLIME_BALL)
            .setName(translation.validateItemName("blocko.items.leave.display_name"))
            .onInteract { event: PlayerInteractEvent ->
                val player: Player = event.player
                val gameArena: GameArena = player.getArena() ?: return@onInteract
                gameArena.quitAsSpectator(player)
            }
            .build()
    }

    fun isInControllingTeam(uuid: UUID): Boolean {
        return getControllingTeam()?.teamMembers?.contains(uuid) ?: false
    }

    fun setNextControllingTeam(): GameTeam? {
        getArena().currentPlayers.filter { !it.isAI }.forEach { gamePlayer: GamePlayer ->
            BlockoGame.instance.bossbarHandler.unregisterBossbar(gamePlayer.toBukkitInstance()!!, "timeoutBar")
        }

        GameScoreboardUtils.updateDicedNumberLine(this.arenaId, null)

        val oldControllingGamePlayer: GamePlayer? = getControllingGamePlayer()
        val oldControllingGamePlayerDicedNumber: Int? = oldControllingGamePlayer?.dicedNumber

        oldControllingGamePlayer?.dicedNumber = null

        if (this.controllingTeamId != null && oldControllingGamePlayer != null)
            getHighlightedEntities(oldControllingGamePlayer, getArena()).forEach { it.toggleHighlighting(false) }

        val availableTeams: List<GameTeam> = BlockoGame.instance.gameTeamHandler.gameTeams[this.arenaId].filter { it.teamMembers.size == 1 && !it.deactivated }

        val newControllingTeam: GameTeam? = if (hasControllingTeamMemberDicedSix(oldControllingGamePlayerDicedNumber)) getControllingTeam() else availableTeams.find { it.teamId > this.controllingTeamId!! }
        val newControllingTeamId: Int = newControllingTeam?.teamId ?: availableTeams.minOf { it.teamId }

        this.lastControllingTeamId = if (this.controllingTeamId == null) null else this.controllingTeamId
        this.controllingTeamId = newControllingTeamId

        val controllingTeam: GameTeam? = getControllingTeam()

        if (controllingTeam != null) {
            GameScoreboardUtils.updateControllingTeamLine(getArena(), controllingTeam)
            GameScoreboardUtils.updateAllEntityStatusLines(this.arenaId, controllingTeam)

            val gamePlayer: GamePlayer? = getArena().currentPlayers.find { it.uuid == controllingTeam.teamMembers.first() }

            if (gamePlayer != null) {
                gamePlayer.playSound(Sound.BLOCK_NOTE_BLOCK_PLING)
                if (gamePlayer.actionTimeoutTimestamp == null) gamePlayer.actionTimeoutTimestamp = System.currentTimeMillis() + Duration.ofMinutes(1).toMillis()
            }
        }

        return controllingTeam
    }

    fun getControllingTeam(): GameTeam? {
        return BlockoGame.instance.gameTeamHandler.gameTeams.get(this.arenaId).find { it.teamId == this.controllingTeamId }
    }

    fun getControllingGamePlayer(): GamePlayer? {
        val controllingTeam: GameTeam = getControllingTeam() ?: return null
        val currentPlayers: MutableSet<GamePlayer> = getArena().currentPlayers
        if (currentPlayers.isEmpty()) return null

        val uuid: UUID = controllingTeam.teamMembers.firstOrNull() ?: return null
        return currentPlayers.find { it.uuid == uuid }
    }

    fun getControllingGamePlayerTimeLeftFraction(): Float {
        val totalActionTime = 60_000L
        val controllingGamePlayer: GamePlayer = getControllingGamePlayer() ?: return 0f
        val timeoutTimestamp: Long = controllingGamePlayer.actionTimeoutTimestamp ?: return 0f
        val currentTimeMillis = System.currentTimeMillis()
        val timeLeftMillis: Long = timeoutTimestamp - currentTimeMillis

        val positiveTimeLeftMillis = if (timeLeftMillis > 0) timeLeftMillis else 0L
        val timeLeftFraction = positiveTimeLeftMillis.toFloat() / totalActionTime.toFloat()

        return timeLeftFraction.coerceIn(0.0f, 1.0f)
    }

    fun getControllingGamePlayerTimeLeft(): Long {
        val controllingGamePlayer: GamePlayer = getControllingGamePlayer() ?: return 0L
        val timeoutTimestamp: Long = controllingGamePlayer.actionTimeoutTimestamp ?: return 0L
        val timeLeft: Long = timeoutTimestamp - System.currentTimeMillis()
        return timeLeft.toDuration(DurationUnit.MILLISECONDS).inWholeSeconds
    }

    fun getAmountOfFinishedTeams(): Int {
        return getArena().currentPlayers.filter { it.hasSavedAllEntities() }.size
    }

    private fun hasControllingTeamMemberDicedSix(dicedNumber: Int?): Boolean {
        val controllingTeam: GameTeam = getControllingTeam() ?: return false
        if (controllingTeam.deactivated) return false

        var hasDicedSix = false

        for (teamMemberUniqueId: UUID in controllingTeam.teamMembers) {
            if (dicedNumber == null || dicedNumber != 6) continue
            hasDicedSix = true
        }

        return hasDicedSix
    }

    private fun getHighlightedEntities(gamePlayer: GamePlayer, gameArena: GameArena): List<GameEntity> {
        return BlockoGame.instance.gameEntityHandler.getEntitiesFromTeam(gameArena.id, gamePlayer.teamName!!).filter { it.isHighlighted }
    }

}