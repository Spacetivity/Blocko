package net.spacetivity.blocko

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.spacetivity.blocko.achievement.AchievementHandler
import net.spacetivity.blocko.achievement.AchievementPlayerDAO
import net.spacetivity.blocko.achievement.impl.*
import net.spacetivity.blocko.arena.ArenaDAO
import net.spacetivity.blocko.arena.ArenaHandler
import net.spacetivity.blocko.arena.setup.ArenaSetupHandler
import net.spacetivity.blocko.arena.setup.tooltips.TooltipHandler
import net.spacetivity.blocko.arena.setup.tooltips.TooltipViewersDAO
import net.spacetivity.blocko.arena.sign.ArenaSignDAO
import net.spacetivity.blocko.arena.sign.ArenaSignHandler
import net.spacetivity.blocko.bossbar.BossbarHandler
import net.spacetivity.blocko.command.api.BukkitCommandExecutor
import net.spacetivity.blocko.command.api.SpaceCommand
import net.spacetivity.blocko.command.api.SpaceCommandController
import net.spacetivity.blocko.command.api.SpaceMainCommandExecutor
import net.spacetivity.blocko.command.commands.BlockoCommand
import net.spacetivity.blocko.dice.DiceHandler
import net.spacetivity.blocko.entity.GameEntityHandler
import net.spacetivity.blocko.entity.GameEntityHistoryDAO
import net.spacetivity.blocko.entity.GameEntityTypeDAO
import net.spacetivity.blocko.field.GameFieldDAO
import net.spacetivity.blocko.field.GameFieldHandler
import net.spacetivity.blocko.field.GameFieldProperties
import net.spacetivity.blocko.field.GameFieldPropertiesTypeAdapter
import net.spacetivity.blocko.field.highlighting.GameFieldHighlightHandler
import net.spacetivity.blocko.files.*
import net.spacetivity.blocko.listener.PlayerListener
import net.spacetivity.blocko.listener.PlayerSetupListener
import net.spacetivity.blocko.listener.ProtectionListener
import net.spacetivity.blocko.lobby.LobbySpawnDAO
import net.spacetivity.blocko.lobby.LobbySpawnHandler
import net.spacetivity.blocko.phase.GamePhaseHandler
import net.spacetivity.blocko.player.EntityAiHandler
import net.spacetivity.blocko.player.GamePlayActionHandler
import net.spacetivity.blocko.scoreboard.PlayerFormatHandler
import net.spacetivity.blocko.scoreboard.SidebarHandler
import net.spacetivity.blocko.stats.StatsPlayerDAO
import net.spacetivity.blocko.stats.StatsPlayerHandler
import net.spacetivity.blocko.team.GameTeamHandler
import net.spacetivity.blocko.team.GameTeamLocationDAO
import net.spacetivity.blocko.translation.TranslationHandler
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.reflect.KClass

class Blocko : JavaPlugin() {

    val interactiveActions = mutableMapOf<UUID, (PlayerInteractEvent) -> Unit>()

    lateinit var diceSidesFile: DiceSidesFile
    lateinit var setupConfigFile: SetupConfigFile
    lateinit var blockoBoardFile: BlockoBoardFile
    lateinit var globalConfigFile: GlobalConfigFile
    lateinit var botNamesFile: BotNamesFile

    lateinit var translationHandler: TranslationHandler
    lateinit var sidebarHandler: SidebarHandler
    lateinit var playerFormatHandler: PlayerFormatHandler
    lateinit var commandController: SpaceCommandController
    lateinit var bossbarHandler: BossbarHandler
    lateinit var gamePhaseHandler: GamePhaseHandler
    lateinit var gameFieldHighlightHandler: GameFieldHighlightHandler
    lateinit var diceHandler: DiceHandler
    lateinit var arenaHandler: ArenaHandler
    lateinit var tooltipHandler: TooltipHandler
    lateinit var arenaSetupHandler: ArenaSetupHandler
    lateinit var gameTeamHandler: GameTeamHandler
    lateinit var gameEntityHandler: GameEntityHandler
    lateinit var gameFieldHandler: GameFieldHandler
    lateinit var arenaSignHandler: ArenaSignHandler

    lateinit var statsPlayerHandler: StatsPlayerHandler
    lateinit var achievementHandler: AchievementHandler

    lateinit var lobbySpawnHandler: LobbySpawnHandler

    lateinit var entityAiHandler: EntityAiHandler

    private lateinit var gamePlayActionHandler: GamePlayActionHandler

    override fun onEnable() {
        instance = this

        val dataFolderPath = this.dataFolder.toPath()
        val databaseFile = DatabaseFile().createOrLoad(dataFolderPath) as DatabaseFile

        if (databaseFile.databaseType == DatabaseType.SQLITE) {
            val sqlitePath = dataFolderPath.resolve("blocko.db").toAbsolutePath().toString()
            Database.connect("jdbc:sqlite:$sqlitePath", driver = "org.sqlite.JDBC")
        } else {
            Database.connect(
                "jdbc:mariadb://${databaseFile.hostname}:${databaseFile.port}/${databaseFile.database}",
                "org.mariadb.jdbc.Driver",
                databaseFile.user,
                databaseFile.password,
            )
        }

        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(
                TooltipViewersDAO,
                ArenaDAO,
                GameFieldDAO,
                GameTeamLocationDAO,
                ArenaSignDAO,
                AchievementPlayerDAO,
                StatsPlayerDAO,
                GameEntityTypeDAO,
                GameEntityHistoryDAO,
                LobbySpawnDAO
            )
        }

        this.diceSidesFile = DiceSidesFile().createOrLoad(dataFolderPath) as DiceSidesFile

        this.translationHandler = TranslationHandler()
        this.translationHandler.generateTranslations(this::class.java)

        this.setupConfigFile = SetupConfigFile().createOrLoad(dataFolderPath) as SetupConfigFile
        this.blockoBoardFile = BlockoBoardFile().createOrLoad(dataFolderPath) as BlockoBoardFile
        this.globalConfigFile = GlobalConfigFile().createOrLoad(dataFolderPath) as GlobalConfigFile
        this.botNamesFile = BotNamesFile().createOrLoad(dataFolderPath) as BotNamesFile

        this.sidebarHandler = SidebarHandler()
        this.playerFormatHandler = PlayerFormatHandler()

        this.commandController = SpaceCommandController()
        this.bossbarHandler = BossbarHandler()
        this.gamePhaseHandler = GamePhaseHandler()
        this.gameFieldHighlightHandler = GameFieldHighlightHandler()
        this.diceHandler = DiceHandler()
        this.diceHandler.startDiceAnimation()
        this.arenaHandler = ArenaHandler()
        this.tooltipHandler = TooltipHandler()
        this.arenaSetupHandler = ArenaSetupHandler()
        this.gameTeamHandler = GameTeamHandler()
        this.gameEntityHandler = GameEntityHandler()
        this.gameFieldHandler = GameFieldHandler()
        this.arenaSignHandler = ArenaSignHandler()
        this.arenaSignHandler.loadArenaSigns()

        this.statsPlayerHandler = StatsPlayerHandler()

        this.achievementHandler = AchievementHandler()
        this.achievementHandler.registerAchievement(PlayFirstGameAchievement("first_game"))
        this.achievementHandler.registerAchievement(FairPlayAchievement("fair_play"))
        this.achievementHandler.registerAchievement(BadMannersAchievement("bad_manners"))
        this.achievementHandler.registerAchievement(FirstKnockoutAchievement("first_knockout"))
        this.achievementHandler.registerAchievement(FirstEliminationAchievement("first_elimination"))
        this.achievementHandler.registerAchievement(MasterEliminatorAchievement("master_eliminator"))
        this.achievementHandler.registerAchievement(RushExpertAchievement("rush_expert"))
        this.achievementHandler.registerAchievement(WinMonsterAchievement("win_monster"))
        this.achievementHandler.registerAchievement(EntityCollectorAchievement("entity_collector"))
        this.achievementHandler.registerAchievement(BadLuckAchievement("bad_luck"))

        this.lobbySpawnHandler = LobbySpawnHandler()

        this.entityAiHandler = EntityAiHandler()

        this.gamePlayActionHandler = GamePlayActionHandler()
        this.gamePlayActionHandler.startMainTask()
        this.gamePlayActionHandler.startMovementTask()
        this.gamePlayActionHandler.startPlayerTask()

        registerCommand(BlockoCommand::class)

        PlayerSetupListener(this)
        PlayerListener(this)
        ProtectionListener(this)
    }

    override fun onDisable() {
        for (player in Bukkit.getOnlinePlayers()) {
            for (team in player.scoreboard.teams) {
                if (team.hasEntry(player.name))
                    team.removeEntry(player.name)

                team.unregister()
            }
        }

        this.diceHandler.stopDiceAnimation()
        this.gamePlayActionHandler.stopTasks()
        this.arenaSetupHandler.stopTask()
        this.arenaHandler.resetArenas(true)
        this.arenaHandler.cachedArenas.map { it.gameWorld }.map { it.entities }.forEach { it.filter { entity -> entity.type != EntityType.PLAYER }.forEach(Entity::remove) }
    }

    private fun registerCommand(executorClass: KClass<out SpaceMainCommandExecutor>) {
        val commandExecutor = executorClass.java.getDeclaredConstructor().newInstance()
        val constructor = BukkitCommandExecutor::class.java.getDeclaredConstructor(SpaceCommand::class.java, Blocko::class.java)
        constructor.newInstance(this.commandController.registerCommand(commandExecutor), this)
    }

    companion object {
        val GSON: Gson = GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(GameFieldProperties::class.java, GameFieldPropertiesTypeAdapter())
            .create()

        @JvmStatic
        lateinit var instance: Blocko
            private set
    }

    fun getAchievementKey(isName: Boolean, title: String): String =
        "blocko.achievement.$title.${if (isName) "display_name" else "requirement"}"

}