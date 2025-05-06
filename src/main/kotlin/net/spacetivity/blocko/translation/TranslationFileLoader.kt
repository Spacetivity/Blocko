package net.spacetivity.blocko.translation

import net.spacetivity.blocko.Blocko
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URISyntaxException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

object TranslationFileLoader {

    fun getFileContent(languageName: String): Map<String, String> {
        val file = Paths.get(Blocko.instance.dataFolder.path, "locales", "$languageName.yml").toFile()
        val result = mutableMapOf<String, String>()
        val yamlConfiguration = YamlConfiguration.loadConfiguration(file)

        fun parseSection(path: String, section: ConfigurationSection) {
            section.getKeys(false).forEach { key ->
                val fullPath = if (path.isEmpty()) key else "$path.$key"
                when (val value = section.get(key)) {
                    is String -> result[fullPath] = value
                    is ConfigurationSection -> parseSection(fullPath, value)
                }
            }
        }

        parseSection("", yamlConfiguration)
        return result
    }

    fun copyTranslationFileToDataFolder(languageName: String) {
        val localesDirectory = File(Blocko.instance.dataFolder, "locales")
        if (!localesDirectory.exists()) localesDirectory.mkdirs()

        val file = File(localesDirectory, "$languageName.yml")
        if (file.exists()) return

        val inputStream = Blocko.instance.getResource("lang/$languageName.yml") ?: throw NullPointerException("File (lang/$languageName.yml) not found!")
        inputStream.use { source -> FileOutputStream(file).use { output -> source.copyTo(output) } }
    }

    fun getLangFileNamesFromJar(rawPath: String, clazz: Class<*>): Set<String> {
        val languageNames: MutableSet<String> = HashSet()

        try {
            val fileSystem = FileSystems.newFileSystem(Objects.requireNonNull(clazz.getResource("")).toURI(), emptyMap<String, Any>())
            val pathStream = Files.list(fileSystem.rootDirectories.iterator().next().resolve(rawPath))

            if (!rawPath.contains("lang/shared")) {
                pathStream.filter { !it.toString().contains("lang/shared") }.forEach { languageNames.add(it.toString()) }
            } else {
                pathStream.forEach { path -> languageNames.add(path.toString()) }
            }

            fileSystem.close()
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return languageNames
    }

}