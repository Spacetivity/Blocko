package net.spacetivity.blocko.arena.setup

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArenaStatus
import net.spacetivity.blocko.arena.setup.step.impl.ScanBoardStep
import net.spacetivity.blocko.arena.setup.step.impl.SetTeamEntrancesStep
import net.spacetivity.blocko.arena.setup.step.impl.SetTurningPointsStep
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.field.GameFieldProperties
import net.spacetivity.blocko.field.PathFace
import net.spacetivity.blocko.field.highlighting.scoreboard.impl.*
import net.spacetivity.blocko.team.GameTeamLocation
import net.spacetivity.blocko.translation.translateActionBar
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.LocationUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.*

class GameArenaSetupHandler {

    private val activeSetupSessions: MutableMap<UUID, GameArenaSetupSession> = mutableMapOf()

    private var setupTask: BukkitTask? = null

    private val isSetupEndless = BlockoGame.instance.setupConfigFile.setupSessionEndless

    init {
        this.setupTask = Bukkit.getScheduler().runTaskTimer(BlockoGame.instance, Runnable {
            for (player in Bukkit.getOnlinePlayers()) {
                val setupSession = player.getSetupSession() ?: continue
                val activeSetupStep = setupSession.getActiveSetupStep() ?: continue

                val facing: BlockFace = player.facing
                if ((activeSetupStep is SetTurningPointsStep || activeSetupStep is SetTeamEntrancesStep) && (facing == BlockFace.NORTH || facing == BlockFace.SOUTH || facing == BlockFace.EAST || facing == BlockFace.WEST)) {
                    player.translateActionBar("blocko.setup.turn_direction", Placeholder.parsed("face", facing.name))
                }

                if (isSetupEndless || (System.currentTimeMillis() < setupSession.timeoutTimestamp)) continue
                handleSetupEnd(player, false)
            }
        }, 0L, 20L)
    }

    fun stopTask() {
        if (this.setupTask == null) return
        this.setupTask!!.cancel()
        this.setupTask = null
    }

    fun getSetupData(uuid: UUID): GameArenaSetupSession? {
        return this.activeSetupSessions[uuid]
    }

    fun startSetup(player: Player, arenaId: String) {
        if (this.activeSetupSessions.contains(player.uniqueId)) {
            player.translateMessage("blocko.setup.already_in_setup_mode")
            return
        }

        if (this.activeSetupSessions.entries.any { it.value.arenaId.equals(arenaId, true) }) {
            player.translateMessage("blocko.setup.arena_already_configurated_by_player")
            return
        }

        val setupTool = GameArenaSetupTool(player)
        setupTool.setToPlayer()

        player.translateMessage("blocko.setup.setup_mode_activated")

        this.activeSetupSessions[player.uniqueId] = GameArenaSetupSession(arenaId, setupTool)
    }

    fun handleSetupEnd(player: Player, success: Boolean) {
        val setupSession = player.getSetupSession()
        if (setupSession == null) {
            player.translateMessage("blocko.setup.not_in_setup_mode")
            return
        }

        if (success) {
            val setupStep = setupSession.getSetupStep(ScanBoardStep::class) ?: return

            val hasNotConfiguredFieldsAlready = setupStep.gameFields.isEmpty()
            if (hasNotConfiguredFieldsAlready) {
                player.translateMessage("blocko.setup.no_fields_configured")
                return
            }

            val hasNotConfiguredAllGarageFields = setupStep.gameFields.filter { it.isGarageField }.size < 16
            if (hasNotConfiguredAllGarageFields) {
                player.translateMessage("blocko.setup.no_garage_fields_configured")
                return
            }

            val hasNotConfiguredAllTeamSpawnLocations = setupStep.gameTeamLocations.size < (setupSession.gameTeams.size * 4)
            if (hasNotConfiguredAllTeamSpawnLocations) {
                player.translateMessage("blocko.setup.not_enough_team_spawns_configured")
                return
            }

            BlockoGame.instance.gameFieldHandler.initFields(setupStep.gameFields)
            BlockoGame.instance.gameTeamHandler.initTeamSpawns(setupStep.gameTeamLocations)

            val gameArena = BlockoGame.instance.gameArenaHandler.getArena(setupSession.arenaId) ?: return
            BlockoGame.instance.gameArenaSignHandler.loadArenaSigns()
            BlockoGame.instance.gameArenaSignHandler.updateArenaSign(gameArena)

            BlockoGame.instance.gameArenaHandler.updateArenaStatus(setupSession.arenaId, GameArenaStatus.READY)
        }

        BlockoGame.instance.gameFieldHighlightHandler.removeHighlightEntities(
            setupSession.arenaId,
            GameFieldHighlightMode::class,
            GarageFieldHighlightMode::class,
            TurningPointHighlightMode::class,
            TeamPathHighlightMode::class,
            TeamSpawnHighlightMode::class
        )

        player.inventory.remove(setupSession.setupTool.itemStack)
        player.translateMessage("blocko.setup.setup_mode_deactivated")
        this.activeSetupSessions.remove(player.uniqueId)
    }

    fun selectCorner(player: Player, isLeftClick: Boolean, location: Location) {
        val setupSession = player.getSetupSession()

        if (setupSession == null) {
            player.translateMessage("blocko.setup.not_in_setup_mode")
            return
        }

        val setupStep = setupSession.getSetupStep(ScanBoardStep::class) ?: return

        if (isLeftClick) {
            setupStep.corner1 = location
        } else {
            setupStep.corner2 = location
        }

        val messageKeyPart = if (isLeftClick) "first" else "second"
        player.translateMessage("blocko.setup.scanning_board.select_${messageKeyPart}_corner")

        if (setupStep.areCornersSet()) {
            player.translateMessage("blocko.setup.scanning_board.confirm", Placeholder.parsed("id", setupSession.arenaId))
        }
    }

    fun scanBoard(player: Player) {
        val setupSession: GameArenaSetupSession = player.getSetupSession() ?: return
        val setupStep = setupSession.getSetupStep(ScanBoardStep::class) ?: return

        if (!setupStep.areCornersSet()) {
            player.translateMessage("blocko.setup.scanning_board.corners_not_set")
            return
        }

        player.translateMessage("blocko.setup.scanning_board.running")

        val regionData: Multimap<Location, Pair<ScannerResult, String?>> = RegionScanner.scanRegion(setupSession)
        val inOrderResults: Multimap<Location, Pair<ScannerResult, String?>> = ArrayListMultimap.create()

        val validResultsFound: MutableMap<ScannerResult, Int> = mutableMapOf()

        for (entry: Map.Entry<Location, Pair<ScannerResult, String?>> in regionData.entries()) {
            val scannedDataForLocation: Pair<ScannerResult, String?> = entry.value

            val location: Location = entry.key
            val scannerResult: ScannerResult = scannedDataForLocation.first
            val teamName: String? = scannedDataForLocation.second

            if (scannerResult != ScannerResult.UNKNOWN) {
                if (validResultsFound.containsKey(scannerResult)) {
                    val newAmount: Int = validResultsFound[scannerResult]?.plus(1) ?: continue
                    validResultsFound[scannerResult] = newAmount
                } else {
                    validResultsFound[scannerResult] = 1
                }
            }

            when (scannerResult) {
                ScannerResult.TEAM_SPAWN -> {
                    if (teamName == null) throw NullPointerException("The team name is required")
                    setTeamSpawnLocation(setupSession, player, teamName, location)
                }

                ScannerResult.GARAGE_FIELD, ScannerResult.GAME_FIELD -> {
                    inOrderResults.put(location, Pair(scannerResult, teamName))
                }

                ScannerResult.UNKNOWN -> {}
            }
        }

        val sortedResults: List<Map.Entry<Location, Pair<ScannerResult, String?>>> = inOrderResults.entries().sortedBy { it.value.first.priority }

        val scanningCompleted: Boolean = ScannerResult.containsAllValidResults(validResultsFound.keys)

        if (scanningCompleted) {
            for (pipelineItem in sortedResults) {
                val location: Location = pipelineItem.key
                val scannerResult: ScannerResult = pipelineItem.value.first
                val teamName: String? = pipelineItem.value.second

                if (scannerResult == ScannerResult.GAME_FIELD) {
                    addField(setupSession, player, location)
                    continue
                }

                addGarageField(setupSession, player, teamName!!, location.block.location)
            }
        } else {
            setupStep.missingResults.putAll(ScannerResult.getMissingResults(regionData.values().map { it.first }))
        }

        val translation = BlockoGame.instance.translationHandler.getSelectedTranslation()
        val statusKey = "blocko.setup.scanning_board.finished.${if (scanningCompleted) "satisfied" else "unsatisfied"}"
        val statusString = translation.line(statusKey, Placeholder.parsed("id", setupSession.arenaId))

        player.translateMessage("blocko.setup.scanning_board.finished.title",
            Placeholder.component("status", statusString))

        for ((scannerResult: ScannerResult, amount: Int) in validResultsFound) {
            val resultName = scannerResult.name
                .replace('_', ' ')
                .lowercase()
                .replaceFirstChar { it.uppercase() }

            player.translateMessage("blocko.setup.scanning_board.finished.line",
                Placeholder.parsed("result", resultName),
                Placeholder.parsed("amount", amount.toString()))
        }
    }

    fun setTeamSpawnLocation(setupSession: GameArenaSetupSession, player: Player, teamName: String, location: Location) {
        val setupStep = setupSession.getSetupStep(ScanBoardStep::class) ?: return

        if (setupStep.gameTeamLocations.any { it.x == location.x && it.y == location.y && it.z == location.z }) {
            player.translateMessage("blocko.setup.team_spawn_already_set")
            return
        }

        val centeredLocation: Location = LocationUtils.centerLocation(location)
        val yLevel: Double = BlockoGame.instance.gameArenaHandler.getArena(setupSession.arenaId)!!.yLevel

        val teamSpawn = GameTeamLocation(
            setupSession.arenaId,
            teamName,
            location.world.name,
            centeredLocation.x,
            yLevel,
            centeredLocation.z,
            location.yaw,
            location.pitch,
            false
        )

        setupStep.gameTeamLocations.add(teamSpawn)

        BlockoGame.instance.gameFieldHighlightHandler.spawnOrUpdateHighlightEntity(
            setupSession.arenaId,
            centeredLocation,
            TeamSpawnHighlightMode::class
        )
    }

    fun addField(setupSession: GameArenaSetupSession, player: Player, location: Location) {
        val setupStep = setupSession.getSetupStep(ScanBoardStep::class) ?: return

        val x: Double = location.x
        val z: Double = location.z

        if (setupStep.gameFields.any { it.x == x && it.z == z }) {
            player.translateMessage("blocko.setup.game_field_already_set")
            return
        }

        setupStep.gameFields.add(
            GameField(
                setupSession.arenaId,
                location.world,
                x,
                z,
                GameFieldProperties(mutableMapOf(), null, null, null),
                false,
                false
            )
        )

        val centeredLocation: Location = LocationUtils.centerLocation(location)
        BlockoGame.instance.gameFieldHighlightHandler.spawnOrUpdateHighlightEntity(
            setupSession.arenaId,
            centeredLocation,
            GameFieldHighlightMode::class
        )
    }

    fun addGarageField(setupSession: GameArenaSetupSession, player: Player, teamName: String, location: Location) {
        val setupStep = setupSession.getSetupStep(ScanBoardStep::class) ?: return

        val x = location.x
        val z = location.z

        val possibleField = setupStep.gameFields.find { it.world == location.world && it.x == x && it.z == z }

        if (possibleField == null) {
            player.translateMessage("blocko.setup.no_field_found_at_location")
            return
        }

        if (possibleField.isGarageField) {
            player.translateMessage("blocko.setup.field_already_a_garage_field")
            return
        }

        if (possibleField.properties.rotation != null) {
            player.translateMessage("blocko.setup.field_not_able_for_garage_field")
            return
        }

        possibleField.isGarageField = true
        possibleField.properties.garageForTeam = teamName

        val centeredLocation: Location = LocationUtils.centerLocation(location)
        BlockoGame.instance.gameFieldHighlightHandler.spawnOrUpdateHighlightEntity(
            setupSession.arenaId,
            centeredLocation,
            GarageFieldHighlightMode::class
        )
    }

    fun setTurningPoint(player: Player, gameField: GameField, location: Location, face: PathFace) {
        val setupSession = player.getSetupSession()
        if (setupSession == null) {
            player.translateMessage("blocko.setup.not_in_setup_mode")
            return
        }

        if (gameField.isGarageField) {
            player.translateMessage("blocko.setup.turn_not_creatable_at_garage_field")
            return
        }

        gameField.properties.rotation = face

        val centeredLocation: Location = LocationUtils.centerLocation(location)
        BlockoGame.instance.gameFieldHighlightHandler.spawnOrUpdateHighlightEntity(
            setupSession.arenaId,
            centeredLocation,
            TurningPointHighlightMode::class
        )
    }

    fun setFieldTeamId(player: Player, teamName: String, location: Location) {
        val setupSession = player.getSetupSession()
        if (setupSession == null) {
            player.translateMessage("blocko.setup.not_in_setup_mode")
            return
        }

        val setupStep = setupSession.getSetupStep(ScanBoardStep::class) ?: return
        val possibleField = setupStep.gameFields.find { it.world == location.world && it.x == location.x && it.z == location.z }

        if (possibleField == null) {
            player.translateMessage("blocko.setup.no_field_found_at_location")
            return
        }

        val hasConfiguredCompleteTeamPath = setupStep.gameFields.all { it.properties.getFieldId(teamName) != null }
        if (hasConfiguredCompleteTeamPath) {
            val gameTeam = BlockoGame.instance.gameTeamHandler.getTeam(setupSession.arenaId, teamName) ?: return
            player.translateMessage("blocko.setup.team_path_already_completed",
                Placeholder.parsed("team_color", "<${gameTeam.color.asHexString()}>"),
                Placeholder.parsed("team_name", gameTeam.name.lowercase().replaceFirstChar { it.uppercase() }))
            return
        }

        if (possibleField.properties.getFieldId(teamName) != null) {
            player.translateMessage("blocko.setup.field_already_has_id")
            return
        }

        possibleField.properties.setFieldId(teamName, setupStep.fieldIndex)
        setupStep.fieldIndex++

        val centeredLocation: Location = LocationUtils.centerLocation(location)
        BlockoGame.instance.gameFieldHighlightHandler.spawnOrUpdateHighlightEntity(
            setupSession.arenaId,
            centeredLocation,
            TeamPathHighlightMode::class
        )
    }

}