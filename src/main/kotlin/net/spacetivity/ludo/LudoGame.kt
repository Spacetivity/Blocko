package net.spacetivity.ludo

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.ludo.arena.GameArena
import net.spacetivity.ludo.arena.GameArenaDAO
import net.spacetivity.ludo.arena.GameArenaHandler
import net.spacetivity.ludo.arena.setup.GameArenaSetupHandler
import net.spacetivity.ludo.command.LudoCommand
import net.spacetivity.ludo.command.api.CommandProperties
import net.spacetivity.ludo.command.api.LudoCommandExecutor
import net.spacetivity.ludo.command.api.LudoCommandHandler
import net.spacetivity.ludo.command.api.impl.BukkitCommandExecutor
import net.spacetivity.ludo.database.DatabaseFile
import net.spacetivity.ludo.entity.GameEntityHandler
import net.spacetivity.ludo.field.GameFieldDAO
import net.spacetivity.ludo.field.GameFieldHandler
import net.spacetivity.ludo.listener.PlayerSetupListener
import net.spacetivity.ludo.team.GameTeamHandler
import net.spacetivity.ludo.team.GameTeamSpawnDAO
import net.spacetivity.ludo.utils.FileUtils
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
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
import java.util.*

class LudoGame : JavaPlugin() {

    lateinit var commandHandler: LudoCommandHandler
    lateinit var gameArenaHandler: GameArenaHandler
    lateinit var gameArenaSetupHandler: GameArenaSetupHandler
    lateinit var gameTeamHandler: GameTeamHandler
    lateinit var gameEntityHandler: GameEntityHandler
    lateinit var gameFieldHandler: GameFieldHandler

    private var idleTask: BukkitTask? = null

    override fun onEnable() {
        instance = this

        Bukkit.getConsoleSender().sendMessage("""
            
            §b ____  _            _         
            §b| __ )| | ___   ___| | _____  
            §b|  _ \| |/ _ \ / __| |/ / _ \ 
            §3| |_) | | (_) | (__|   < (_) |
            §3|____/|_|\___/ \___|_|\_\___/ 
            
            §bBlocko §6v0.1 - ALPHA §bby spacetivity.net (§ehttps://github.com/Spacetivity§b)
            
        """.trimIndent())

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
                    GameTeamSpawnDAO
                )
            }
        } catch (e: Exception) {
            val message = Component.text("Database connection failed für LudoGame!", NamedTextColor.RED)
            Bukkit.getConsoleSender().sendMessage(message)
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }

        this.gameArenaHandler = GameArenaHandler()
        this.commandHandler = LudoCommandHandler()
        this.gameArenaSetupHandler = GameArenaSetupHandler()
        this.gameTeamHandler = GameTeamHandler()
        this.gameEntityHandler = GameEntityHandler()
        this.gameFieldHandler = GameFieldHandler()

        //TODO: Load all worlds from all game arenas!

        registerCommand(LudoCommand())
        PlayerSetupListener(this)
    }

    override fun onDisable() {
        this.idleTask?.cancel()
        this.gameArenaHandler.resetArenas()

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
                //TODO: Check if arena is in 'lobby' state!
                for (uuid: UUID in gameArena.currentPlayers) {
                    val player: Player = Bukkit.getPlayer(uuid) ?: continue
                    player.sendActionBar(Component.text("Waiting for more players...", NamedTextColor.RED))
                }
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
        val emptyArenas = this.gameArenaHandler.cachedArenas.filter { it.currentPlayers.size < it.maxPlayers }.toList()
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

}