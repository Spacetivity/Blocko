package net.spacetivity.ludo

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.arena.GameArenaDAO
import net.spacetivity.ludo.arena.GameArenaHandler
import net.spacetivity.ludo.arena.setup.GameArenaSetupHandler
import net.spacetivity.ludo.arena.sign.GameArenaSignDAO
import net.spacetivity.ludo.arena.sign.GameArenaSignHandler
import net.spacetivity.ludo.command.LudoCommand
import net.spacetivity.ludo.command.api.CommandProperties
import net.spacetivity.ludo.command.api.LudoCommandExecutor
import net.spacetivity.ludo.command.api.LudoCommandHandler
import net.spacetivity.ludo.command.api.impl.BukkitCommandExecutor
import net.spacetivity.ludo.database.DatabaseFile
import net.spacetivity.ludo.dice.DiceHandler
import net.spacetivity.ludo.dice.DiceSidesFile
import net.spacetivity.ludo.entity.GameEntityHandler
import net.spacetivity.ludo.field.GameFieldDAO
import net.spacetivity.ludo.field.GameFieldHandler
import net.spacetivity.ludo.garageField.GameGarageFieldDAO
import net.spacetivity.ludo.garageField.GameGarageFieldHandler
import net.spacetivity.ludo.listener.PlayerListener
import net.spacetivity.ludo.phase.GamePhaseHandler
import net.spacetivity.ludo.team.GameTeamHandler
import net.spacetivity.ludo.team.GameTeamLocationDAO
import net.spacetivity.ludo.utils.FileUtils
import org.bukkit.Bukkit
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

class LudoGame : JavaPlugin() {

    lateinit var diceSidesFile: DiceSidesFile

    lateinit var commandHandler: LudoCommandHandler
    lateinit var gamePhaseHandler: GamePhaseHandler
    lateinit var gameArenaHandler: GameArenaHandler
    lateinit var gameArenaSetupHandler: GameArenaSetupHandler
    lateinit var gameTeamHandler: GameTeamHandler
    lateinit var gameEntityHandler: GameEntityHandler
    lateinit var gameFieldHandler: GameFieldHandler
    lateinit var gameGarageFieldHandler: GameGarageFieldHandler
    lateinit var gameArenaSignHandler: GameArenaSignHandler
    lateinit var diceHandler: DiceHandler

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
                    GameGarageFieldDAO,
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

        this.commandHandler = LudoCommandHandler()
        this.gamePhaseHandler = GamePhaseHandler()
        this.gameArenaHandler = GameArenaHandler()
        this.gameArenaSetupHandler = GameArenaSetupHandler()
        this.gameTeamHandler = GameTeamHandler()
        this.gameEntityHandler = GameEntityHandler()
        this.gameFieldHandler = GameFieldHandler()
        this.gameGarageFieldHandler = GameGarageFieldHandler()
        this.gameArenaSignHandler = GameArenaSignHandler()
        this.gameArenaSignHandler.loadArenaSigns()

        this.diceHandler = DiceHandler()
        this.diceHandler.startDiceAnimation()

        //TODO: Load all worlds from all game arenas!

        registerCommand(LudoCommand())
        PlayerListener(this)
    }

    override fun onDisable() {
        this.idleTask?.cancel()
        this.gameArenaHandler.resetArenas()
        this.diceHandler.stopDiceAnimation()

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
        val GSON: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

        @JvmStatic
        lateinit var instance: LudoGame
            private set
    }

    fun tryStartupIdleScheduler() {
        if (!emptyArenasPreset().first) return

        this.idleTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, Runnable {
            val validArenas = emptyArenasPreset().second

            if (!emptyArenasPreset().first) {
                this.idleTask?.cancel()
                this.idleTask = null
            }

            for (gameArena: GameArena in validArenas) {
                if (!gameArena.phase.isIdle()) continue
                gameArena.sendArenaMessage(Component.text("Waiting for more players...", NamedTextColor.RED))
            }
        }, 0L, 20L)
    }

    fun tryShutdownIdleScheduler() {
        if (this.idleTask == null) return
        if (emptyArenasPreset().first) return

        this.idleTask?.cancel()
        this.idleTask = null
    }

    private fun emptyArenasPreset(): Pair<Boolean, List<GameArena>> {
        val emptyArenas = this.gameArenaHandler.cachedArenas.filter { it.currentPlayers.size < it.maxPlayers && it.phase.isIdle() }.toList()
        return Pair(emptyArenas.isNotEmpty(), emptyArenas)
    }

    private fun createOrLoadDatabaseProperties(): DatabaseFile {
        val databaseFilePath = File("${dataFolder.toPath()}/database")
        val result: DatabaseFile

        if (!Files.exists(databaseFilePath.toPath())) Files.createDirectories(databaseFilePath.toPath())

        val file: File = Paths.get("${databaseFilePath}/mysql.json").toFile()

        if (!Files.exists(file.toPath())) {
            result = DatabaseFile("37.114.34.28", 3306, "ludo_game", "root", "-")
            FileUtils.save(file, result)
        } else {
            result = FileUtils.read(file, DatabaseFile::class.java)!!
        }

        return result
    }

    private fun createOrLoadDiceSidesFile(): DiceSidesFile {
        val diceSidesFilePath = File("${dataFolder.toPath()}/dice")
        val result: DiceSidesFile

        if (!Files.exists(diceSidesFilePath.toPath())) Files.createDirectories(diceSidesFilePath.toPath())

        val file: File = Paths.get("${diceSidesFilePath}/dice_sides.json").toFile()

        if (!Files.exists(file.toPath())) {
            result = DiceSidesFile(mutableMapOf(
                Pair(1, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTMxMzVlYTMxYmMxNWJlMTM0NjJiZjEwZTkxMmExNDBlNWE3ZDY4ZWY0YmQyNmUzZDc1MDU1OWQ1MDJiZjk1In19fQ=="),
                Pair(2, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjFhZmM1YzkzZmM1MzMyMzNkZWY1ODU4ZDE5YTNhMWI1NzY0YzViMmRjZTZiNWQxZjc5Mzg2ZTk2NDA1MDNhZiJ9fX0="),
                Pair(3, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODQ1NTVhMzY0MTE5NWMxNjg2MGU4MmYzODlmZDI3Y2JkMTE3ODA0OWJkN2IxYmI3N2IwMzFmYjM5OGE2NDQ4MiJ9fX0="),
                Pair(4, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2NmMTliYmJiMTNhMWYzNWFjOGYxNDFjZmNlZjlkMDA4NGQxNzZlY2I0ZjRlZWZiNThhZmRhMzUzMGQwYTcyNyJ9fX0="),
                Pair(5, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDM1MWFmNDk5ZjRiZjBiNmNmYWI3YTFmNjI2MWM1YzExYWUyY2RjMDE5ODI1YWFkYjk2OWQ1NjdmZjM1NDUzNSJ9fX0="),
                Pair(6, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmNjZDc1M2RiMTlmYmZjZDNhNTRmNmZkZDBhYTQ1ZDFhM2JmMjVjNjM3ZDY2N2M0M2U2NDZiMWEzOTBmYTYyZCJ9fX0=")
            ))
            FileUtils.save(file, result)
        } else {
            result = FileUtils.read(file, DiceSidesFile::class.java)!!
        }

        return result
    }

}