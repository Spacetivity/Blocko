package net.spacetivity.blocko

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.spacetivity.blocko.achievement.AchievementHandler
import net.spacetivity.blocko.achievement.AchievementPlayerDAO
import net.spacetivity.blocko.achievement.impl.*
import net.spacetivity.blocko.arena.GameArenaDAO
import net.spacetivity.blocko.arena.GameArenaHandler
import net.spacetivity.blocko.arena.setup.GameArenaSetupHandler
import net.spacetivity.blocko.arena.sign.GameArenaSignDAO
import net.spacetivity.blocko.arena.sign.GameArenaSignHandler
import net.spacetivity.blocko.bossbar.BossbarHandler
import net.spacetivity.blocko.command.ArenaInviteCommand
import net.spacetivity.blocko.command.BlockoCommand
import net.spacetivity.blocko.command.api.CommandProperties
import net.spacetivity.blocko.command.api.SpaceCommandExecutor
import net.spacetivity.blocko.command.api.SpaceCommandHandler
import net.spacetivity.blocko.command.api.impl.BukkitCommandExecutor
import net.spacetivity.blocko.dice.DiceHandler
import net.spacetivity.blocko.dice.DiceSidesFile
import net.spacetivity.blocko.entity.GameEntityHandler
import net.spacetivity.blocko.entity.GameEntityTypeDAO
import net.spacetivity.blocko.field.GameFieldDAO
import net.spacetivity.blocko.field.GameFieldHandler
import net.spacetivity.blocko.field.GameFieldProperties
import net.spacetivity.blocko.field.GameFieldPropertiesTypeAdapter
import net.spacetivity.blocko.files.BotNamesFile
import net.spacetivity.blocko.files.DatabaseFile
import net.spacetivity.blocko.files.GlobalConfigFile
import net.spacetivity.blocko.listener.PlayerListener
import net.spacetivity.blocko.listener.PlayerSetupListener
import net.spacetivity.blocko.listener.ProtectionListener
import net.spacetivity.blocko.phase.GamePhaseHandler
import net.spacetivity.blocko.player.GamePlayActionHandler
import net.spacetivity.blocko.scoreboard.SidebarHandler
import net.spacetivity.blocko.stats.StatsPlayerDAO
import net.spacetivity.blocko.stats.StatsPlayerHandler
import net.spacetivity.blocko.team.GameTeamHandler
import net.spacetivity.blocko.team.GameTeamLocationDAO
import net.spacetivity.blocko.translation.TranslationHandler
import net.spacetivity.blocko.utils.FileUtils
import net.spacetivity.blocko.utils.HeadUtils
import net.spacetivity.blocko.utils.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class BlockoGame : JavaPlugin() {

    val clickableItems: MutableMap<UUID, ItemBuilder> = mutableMapOf()

    lateinit var diceSidesFile: DiceSidesFile
    lateinit var globalConfigFile: GlobalConfigFile
    lateinit var botNamesFile: BotNamesFile

    lateinit var translationHandler: TranslationHandler
    lateinit var sidebarHandler: SidebarHandler
    lateinit var commandHandler: SpaceCommandHandler
    lateinit var bossbarHandler: BossbarHandler
    lateinit var gamePhaseHandler: GamePhaseHandler
    lateinit var diceHandler: DiceHandler
    lateinit var gameArenaHandler: GameArenaHandler
    lateinit var gameArenaSetupHandler: GameArenaSetupHandler
    lateinit var gameTeamHandler: GameTeamHandler
    lateinit var gameEntityHandler: GameEntityHandler
    lateinit var gameFieldHandler: GameFieldHandler
    lateinit var gameArenaSignHandler: GameArenaSignHandler

    lateinit var statsPlayerHandler: StatsPlayerHandler
    lateinit var achievementHandler: AchievementHandler

    private lateinit var gamePlayActionHandler: GamePlayActionHandler

    override fun onEnable() {
        instance = this

        val dbProperties: DatabaseFile = createOrLoadDatabaseProperties()

        Database.connect(
            url = "jdbc:mariadb://${dbProperties.hostname}:${dbProperties.port}/${dbProperties.database}",
            driver = "org.mariadb.jdbc.Driver",
            user = dbProperties.user,
            password = dbProperties.password,
        )

        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(
                GameArenaDAO,
                GameFieldDAO,
                GameTeamLocationDAO,
                GameArenaSignDAO,
                AchievementPlayerDAO,
                StatsPlayerDAO,
                GameEntityTypeDAO
            )
        }

        this.diceSidesFile = createOrLoadDiceSidesFile()

        this.translationHandler = TranslationHandler()
        this.translationHandler.generateTranslations(this::class.java)

        this.globalConfigFile = createOrLoadGlobalConfigFile()
        this.botNamesFile = createOrLoadBotNamesFile()

        this.sidebarHandler = SidebarHandler()

        this.commandHandler = SpaceCommandHandler()
        this.bossbarHandler = BossbarHandler()
        this.gamePhaseHandler = GamePhaseHandler()
        this.diceHandler = DiceHandler()
        this.diceHandler.startDiceAnimation()
        this.gameArenaHandler = GameArenaHandler()
        this.gameArenaSetupHandler = GameArenaSetupHandler()
        this.gameTeamHandler = GameTeamHandler()
        this.gameEntityHandler = GameEntityHandler()
        this.gameFieldHandler = GameFieldHandler()
        this.gameArenaSignHandler = GameArenaSignHandler()
        this.gameArenaSignHandler.loadArenaSigns()

        this.statsPlayerHandler = StatsPlayerHandler()

        this.achievementHandler = AchievementHandler()
        this.achievementHandler.registerAchievement(PlayFirstGameAchievement("first_game"))
        this.achievementHandler.registerAchievement(FairPlayAchievement("fair_play"))
        this.achievementHandler.registerAchievement(FirstKnockoutAchievement("first_knockout"))
        this.achievementHandler.registerAchievement(FirstEliminationAchievement("first_elimination"))
        this.achievementHandler.registerAchievement(MasterEliminatorAchievement("master_eliminator"))

        this.gamePlayActionHandler = GamePlayActionHandler()
        this.gamePlayActionHandler.startMainTask()
        this.gamePlayActionHandler.startMovementTask()
        this.gamePlayActionHandler.startPlayerTask()

        registerCommand(BlockoCommand())
        registerCommand(ArenaInviteCommand())

        PlayerSetupListener(this)
        PlayerListener(this)
        ProtectionListener(this)
    }

    override fun onDisable() {
        this.diceHandler.stopDiceAnimation()
        this.gamePlayActionHandler.stopTasks()
        this.gameArenaSetupHandler.stopTask()
        this.gameArenaHandler.resetArenas(true)
        this.gameArenaHandler.cachedArenas.map { it.gameWorld }.map { it.entities }.forEach { it.filter { entity -> entity.type != EntityType.PLAYER }.forEach(Entity::remove) }
    }

    private fun registerCommand(commandExecutor: SpaceCommandExecutor) {
        BukkitCommandExecutor::class.java.getDeclaredConstructor(CommandProperties::class.java, this::class.java)
            .newInstance(this.commandHandler.registerCommand(commandExecutor), this)
    }

    companion object {
        val GSON: Gson = GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(GameFieldProperties::class.java, GameFieldPropertiesTypeAdapter())
            .create()

        @JvmStatic
        lateinit var instance: BlockoGame
            private set
    }

    fun getAchievementKey(isName: Boolean, title: String): String {
        return "blocko.achievement.$title.${if (isName) "display_name" else "requirement"}"
    }

    private fun createOrLoadDatabaseProperties(): DatabaseFile {
        return FileUtils.createOrLoadFile(dataFolder.toPath(), "global", "mysql", DatabaseFile::class, DatabaseFile("-", 3306, "-", "-", "-"))
    }

    private fun createOrLoadDiceSidesFile(): DiceSidesFile {
        return FileUtils.createOrLoadFile(dataFolder.toPath(), "dice", "dice_sides", DiceSidesFile::class, DiceSidesFile(mutableMapOf(
            Pair(1, HeadUtils.DICE_ONE),
            Pair(2, HeadUtils.DICE_TWO),
            Pair(3, HeadUtils.DICE_THREE),
            Pair(4, HeadUtils.DICE_FOUR),
            Pair(5, HeadUtils.DICE_FIVE),
            Pair(6, HeadUtils.DICE_SIX)
        )))
    }

    private fun createOrLoadGlobalConfigFile(): GlobalConfigFile {
        val availableTranslationLanguages: List<String> = this.translationHandler.cachedTranslations.map { it.name }
        val defaultLanguageName: String = if (availableTranslationLanguages.contains("en_US")) "en_US" else availableTranslationLanguages[0]
        return FileUtils.createOrLoadFile(dataFolder.toPath(), "global", "config", GlobalConfigFile::class, GlobalConfigFile(
            defaultLanguageName,
            Material.GOLDEN_HOE.name,
            "BasicLudoBoard",
            true,
            false,
            10,
            20
        ))
    }

    private fun createOrLoadBotNamesFile(): BotNamesFile {
        return FileUtils.createOrLoadFile(dataFolder.toPath(), "global", "bot_names", BotNamesFile::class, BotNamesFile())
    }

}