package net.spacetivity.blocko.phase.impl

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.achievement.grantIfCompletedBy
import net.spacetivity.blocko.achievement.impl.RushExpertAchievement
import net.spacetivity.blocko.achievement.impl.WinMonsterAchievement
import net.spacetivity.blocko.arena.Arena
import net.spacetivity.blocko.arena.getArena
import net.spacetivity.blocko.arena.id.ArenaId
import net.spacetivity.blocko.entity.GameEntity
import net.spacetivity.blocko.item.*
import net.spacetivity.blocko.phase.GamePhase
import net.spacetivity.blocko.phase.GamePhaseMode
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.player.playSound
import net.spacetivity.blocko.scoreboard.GameScoreboardUtils
import net.spacetivity.blocko.stats.toStatsPlayerInstance
import net.spacetivity.blocko.team.GameTeam
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.Constants
import net.spacetivity.blocko.utils.Constants.ENTITY_SELECTOR_KEY
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.inventory.api.SpaceInventoryProvider
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.inventory.ItemStack
import java.time.Duration
import java.util.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class IngamePhase(arenaId: ArenaId) : GamePhase(arenaId, "ingame", 1, null) {

    var lastControllingTeamId: Int? = null
    var controllingTeamId: Int? = null
    var phaseMode = GamePhaseMode.DICE
    var matchStartTime: Long? = null

    override fun start() {
        BlockoGame.instance.arenaSignHandler.updateArenaSign(getArena())

        this.phaseMode = GamePhaseMode.DICE

        if (this.matchStartTime == null) this.matchStartTime = System.currentTimeMillis()

        for (gamePlayer in getArena().currentPlayers.filter { !it.isAI }) {
            val player = gamePlayer.toBukkitInstance() ?: continue
            setupPlayerInventory(player)
        }
    }

    override fun stop() {
        for (gamePlayer in getArena().currentPlayers) {
            val player = gamePlayer.toBukkitInstance() ?: return

            val statsPlayer = gamePlayer.toStatsPlayerInstance()
            if (statsPlayer != null) statsPlayer.wonGames += 1

            BlockoGame.instance.bossbarHandler.unregisterBossbar(player, Constants.TIMEOUT_BOSSBAR_NAME)

            gamePlayer.grantIfCompletedBy(RushExpertAchievement::class)
            gamePlayer.grantIfCompletedBy(WinMonsterAchievement::class)

            val matchDuration = (System.currentTimeMillis() - this.matchStartTime!!).toDuration(DurationUnit.MILLISECONDS)

            matchDuration.toComponents { hours, minutes, seconds, _ ->
                val hoursString = if (hours in 0..9) "0$hours" else hours.toString()
                val minutesString = if (minutes in 0..9) "0$minutes" else minutes.toString()
                val secondsString = if (seconds in 0..9) "0$seconds" else seconds.toString()

                val timeString = "$hoursString:$minutesString:$secondsString"

                val lastPosition = getArena().teamOptions.playerCount
                val positionString = if (gamePlayer.matchStats.position == null) lastPosition.toString() else gamePlayer.matchStats.position!!.toString()

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
        val translation = BlockoGame.instance.translationHandler.getSelectedTranslation()

        hotbarItems[0] = BlockoGame.instance.diceHandler.getDiceItem()

        for ((entityIndex, i) in (1..4).withIndex()) {
            hotbarItems[i] = itemStack(Material.ARMOR_STAND) {
                meta {
                    name = translation.displayName("blocko.main_game_loop.entity_selector_display_name", Placeholder.parsed("count", (entityIndex + 1).toString()))
                    applyPersistentData(ENTITY_SELECTOR_KEY, entityIndex)
                }
            }
        }

        hotbarItems[7] = itemStack(Material.CLOCK) {
            meta {
                name = translation.displayName("blocko.items.profile.display_name")
                lore(translation.lore("blocko.items.profile.lore"))
            }
        }.onInteract { event ->
            InventoryUtils.openProfileInventory(event.player, false)
        }

        hotbarItems[8] = itemStack(Material.SLIME_BALL) {
            meta {
                name = translation.displayName("blocko.items.leave.display_name")
            }
        }.onInteract { event ->
            val player = event.player
            val gameArena = player.getArena() ?: return@onInteract

            SpaceInventoryProvider.api.openConfirmationInventory(
                player,
                translation.displayName("blocko.inventory.leave.title"),
                itemStack(Material.OAK_DOOR) {
                    meta {
                        name = translation.displayName("blocko.inventory.leave.display_item.display_name")
                    }
                },
                {
                    gameArena.quit(player)
                },
                {
                    player.closeInventory()
                }
            )
        }
    }

    override fun initSpectatorHotbarItems(hotbarItems: MutableMap<Int, ItemStack>) {
        val translation = BlockoGame.instance.translationHandler.getSelectedTranslation()

        hotbarItems[8] = itemStack(Material.SLIME_BALL) {
            meta {
                name = translation.displayName("blocko.items.leave.display_name")
            }
        }.onInteract { event ->
            val player = event.player
            val gameArena = player.getArena() ?: return@onInteract
            gameArena.quitAsSpectator(player)
        }
    }

    fun isInControllingTeam(uuid: UUID): Boolean {
        return getControllingTeam()?.teamMembers?.contains(uuid) ?: false
    }

    fun setNextControllingTeam(): GameTeam? {
        for (gamePlayer in getArena().currentPlayers) {
            if (gamePlayer.isAI) continue
            BlockoGame.instance.bossbarHandler.unregisterBossbar(gamePlayer.toBukkitInstance()!!, Constants.TIMEOUT_BOSSBAR_NAME)
        }

        GameScoreboardUtils.updateDicedNumberLine(this.arenaId, null)

        val oldControllingGamePlayer = getControllingGamePlayer()
        val oldControllingGamePlayerDicedNumber = oldControllingGamePlayer?.dicedNumber

        oldControllingGamePlayer?.dicedNumber = null

        if (this.controllingTeamId != null && oldControllingGamePlayer != null)
            getHighlightedEntities(oldControllingGamePlayer, getArena()).forEach { it.toggleHighlighting(false) }

        val availableTeams = BlockoGame.instance.gameTeamHandler.gameTeams[this.arenaId].filter { it.teamMembers.size == 1 && !it.deactivated }
        val newControllingTeam = if (hasControllingTeamMemberDicedSix(oldControllingGamePlayerDicedNumber)) getControllingTeam() else availableTeams.find { it.teamId > this.controllingTeamId!! }
        val newControllingTeamId = newControllingTeam?.teamId ?: availableTeams.minOf { it.teamId }

        this.lastControllingTeamId = if (this.controllingTeamId == null) null else this.controllingTeamId
        this.controllingTeamId = newControllingTeamId

        val controllingTeam = getControllingTeam()

        if (controllingTeam != null) {
            GameScoreboardUtils.updateControllingTeamLine(getArena(), controllingTeam)
            GameScoreboardUtils.updateAllEntityStatusLines(this.arenaId, controllingTeam)

            val gamePlayer = getArena().currentPlayers.find { it.uuid == controllingTeam.teamMembers.first() }

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
        val controllingTeam = getControllingTeam() ?: return null
        val currentPlayers = getArena().currentPlayers
        if (currentPlayers.isEmpty()) return null

        val uuid = controllingTeam.teamMembers.firstOrNull() ?: return null
        return currentPlayers.find { it.uuid == uuid }
    }

    fun getControllingGamePlayerTimeLeftFraction(): Float {
        val totalActionTime = 60_000L
        val controllingGamePlayer = getControllingGamePlayer() ?: return 0f
        val timeoutTimestamp = controllingGamePlayer.actionTimeoutTimestamp ?: return 0f
        val currentTimeMillis = System.currentTimeMillis()
        val timeLeftMillis = timeoutTimestamp - currentTimeMillis

        val positiveTimeLeftMillis = if (timeLeftMillis > 0) timeLeftMillis else 0L
        val timeLeftFraction = positiveTimeLeftMillis.toFloat() / totalActionTime.toFloat()

        return timeLeftFraction.coerceIn(0.0f, 1.0f)
    }

    fun getControllingGamePlayerTimeLeft(): Long {
        val controllingGamePlayer = getControllingGamePlayer() ?: return 0L
        val timeoutTimestamp = controllingGamePlayer.actionTimeoutTimestamp ?: return 0L
        val timeLeft = timeoutTimestamp - System.currentTimeMillis()
        return timeLeft.toDuration(DurationUnit.MILLISECONDS).inWholeSeconds
    }

    fun getAmountOfFinishedTeams(): Int {
        return getArena().currentPlayers.filter { it.hasSavedAllEntities() }.size
    }

    //TODO: check if this new impl works lol (old function was quite dumb...)
    private fun hasControllingTeamMemberDicedSix(dicedNumber: Int?): Boolean {
        val controllingTeam = getControllingTeam() ?: return false
        if (controllingTeam.deactivated) return false

        return dicedNumber != null && dicedNumber == 6
    }

    private fun getHighlightedEntities(gamePlayer: GamePlayer, arena: Arena): List<GameEntity> {
        return BlockoGame.instance.gameEntityHandler.getEntitiesFromTeam(arena.id, gamePlayer.teamName!!).filter { it.isHighlighted }
    }

}