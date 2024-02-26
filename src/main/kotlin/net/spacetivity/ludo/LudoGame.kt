package net.spacetivity.ludo

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.arena.GameArenaDAO
import net.spacetivity.ludo.arena.GameArenaHandler
import net.spacetivity.ludo.arena.setup.GameArenaSetupHandler
import net.spacetivity.ludo.arena.sign.GameArenaSignDAO
import net.spacetivity.ludo.arena.sign.GameArenaSignHandler
import net.spacetivity.ludo.bossbar.BossbarHandler
import net.spacetivity.ludo.command.LudoCommand
import net.spacetivity.ludo.command.api.CommandProperties
import net.spacetivity.ludo.command.api.LudoCommandExecutor
import net.spacetivity.ludo.command.api.LudoCommandHandler
import net.spacetivity.ludo.command.api.impl.BukkitCommandExecutor
import net.spacetivity.ludo.dice.DiceHandler
import net.spacetivity.ludo.dice.DiceSidesFile
import net.spacetivity.ludo.entity.GameEntityHandler
import net.spacetivity.ludo.field.GameFieldDAO
import net.spacetivity.ludo.field.GameFieldHandler
import net.spacetivity.ludo.field.GameFieldProperties
import net.spacetivity.ludo.field.GameFieldPropertiesTypeAdapter
import net.spacetivity.ludo.files.DatabaseFile
import net.spacetivity.ludo.files.GlobalConfigFile
import net.spacetivity.ludo.files.SpaceFile
import net.spacetivity.ludo.listener.PlayerListener
import net.spacetivity.ludo.listener.PlayerSetupListener
import net.spacetivity.ludo.phase.GamePhaseHandler
import net.spacetivity.ludo.player.GamePlayActionHandler
import net.spacetivity.ludo.team.GameTeamHandler
import net.spacetivity.ludo.team.GameTeamLocationDAO
import net.spacetivity.ludo.translation.TranslationHandler
import net.spacetivity.ludo.utils.FileUtils
import net.spacetivity.ludo.utils.HeadUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.reflect.KClass

class LudoGame : JavaPlugin() {

    lateinit var diceSidesFile: DiceSidesFile
    lateinit var globalConfigFile: GlobalConfigFile

    lateinit var translationHandler: TranslationHandler
    lateinit var commandHandler: LudoCommandHandler
    lateinit var bossbarHandler: BossbarHandler
    lateinit var gamePhaseHandler: GamePhaseHandler
    lateinit var diceHandler: DiceHandler
    lateinit var gameArenaHandler: GameArenaHandler
    lateinit var gameArenaSetupHandler: GameArenaSetupHandler
    lateinit var gameTeamHandler: GameTeamHandler
    lateinit var gameEntityHandler: GameEntityHandler
    lateinit var gameFieldHandler: GameFieldHandler
    lateinit var gameArenaSignHandler: GameArenaSignHandler

    lateinit var gamePlayActionHandler: GamePlayActionHandler

    private var idleTask: BukkitTask? = null

    override fun onEnable() {
        instance = this

        try {
            val dbProperties: DatabaseFile = createOrLoadDatabaseProperties()
            Database.connect(
                "jdbc:mariadb://${dbProperties.hostname}:${dbProperties.port}/${dbProperties.database}",
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
                    GameArenaSignDAO
                )
            }
        } catch (e: Exception) {
            val message = Component.text("Database connection failed für LudoGame!", NamedTextColor.RED)
            Bukkit.getConsoleSender().sendMessage(message)
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }

        this.diceSidesFile = createOrLoadDiceSidesFile()

        this.translationHandler = TranslationHandler()
        this.translationHandler.generateTranslations(this::class.java)

        this.globalConfigFile = createOrLoadGlobalConfigFile()

        this.commandHandler = LudoCommandHandler()
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

        this.gamePlayActionHandler = GamePlayActionHandler()
        this.gamePlayActionHandler.startMovementTask()
        this.gamePlayActionHandler.startPlayerTask()

        registerCommand(LudoCommand())
        PlayerSetupListener(this)
        PlayerListener(this)
    }

    override fun onDisable() {
        this.idleTask?.cancel()
        this.gameArenaHandler.resetArenas()
        this.diceHandler.stopDiceAnimation()
        this.gamePlayActionHandler.stopTasks()
        this.gameArenaSetupHandler.stopTask()

        for (entities: MutableList<Entity> in Bukkit.getWorlds().map { it.entities }) {
            for (entity: Entity in entities) {
                if (!entity.hasMetadata("displayEntity")) continue
                entity.remove()
            }
        }
    }

    private fun registerCommand(commandExecutor: LudoCommandExecutor) {
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
        lateinit var instance: LudoGame
            private set
    }

    private fun createOrLoadDatabaseProperties(): DatabaseFile {
        return createOrLoadFile("global", "mysql", DatabaseFile::class, DatabaseFile("-", 3306, "blocko_game", "-", "-"))
    }

    private fun createOrLoadDiceSidesFile(): DiceSidesFile {
        return createOrLoadFile("dice", "dice_sides", DiceSidesFile::class, DiceSidesFile(mutableMapOf(
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
        val languageName: String = if (availableTranslationLanguages.contains("en_US")) "en_US" else availableTranslationLanguages[0]
        return createOrLoadFile("global", "config", GlobalConfigFile::class, GlobalConfigFile(languageName, Material.GOLDEN_HOE.name))
    }

    fun <T : SpaceFile> createOrLoadFile(subFolderName: String, fileName: String, clazz: KClass<T>, content: T): T {
        val filePath = File("${dataFolder.toPath()}/$subFolderName")
        val result: T

        if (!Files.exists(filePath.toPath())) Files.createDirectories(filePath.toPath())
        val file: File = Paths.get("${filePath}/$fileName.json").toFile()

        if (!Files.exists(file.toPath())) {
            result = content
            FileUtils.save(file, result)
        } else {
            result = FileUtils.read(file, clazz.java)!!
        }

        return result
    }

    fun <T : SpaceFile> readFile(subFolderName: String, fileName: String, clazz: KClass<T>): T? {
        val filePath = File("${dataFolder.toPath()}/$subFolderName")

        if (!Files.exists(filePath.toPath())) return null
        val file: File = Paths.get("${filePath}/$fileName.json").toFile()

        return FileUtils.read(file, clazz.java)
    }

    fun readRawFile(subFolderName: String, fileName: String): File? {
        val filePath = File("${dataFolder.toPath()}/$subFolderName")

        if (!Files.exists(filePath.toPath())) return null
        val file: File = Paths.get("${filePath}/$fileName.json").toFile()

        return file
    }

}