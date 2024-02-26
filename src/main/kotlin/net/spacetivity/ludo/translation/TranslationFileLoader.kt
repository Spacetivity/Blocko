package net.spacetivity.ludo.translation

import net.spacetivity.ludo.LudoGame
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Stream

object TranslationFileLoader {

    fun getLangFileNamesFromJar(rawPath: String, clazz: Class<*>): Set<String> {
        val languageNames: MutableSet<String> = HashSet()

        try {
            val fileSystem: FileSystem = FileSystems.newFileSystem(Objects.requireNonNull(clazz.getResource("")).toURI(), emptyMap<String, Any>())
            val pathStream: Stream<Path> = Files.list(fileSystem.rootDirectories.iterator().next().resolve(rawPath))

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

    fun getSharedLangFileNamesFromJar(clazz: Class<*>): Set<String> {
        val languageNames: MutableSet<String> = HashSet()

        try {
            val fileSystem: FileSystem = FileSystems.newFileSystem(Objects.requireNonNull(clazz.getResource("")).toURI(), emptyMap<String, Any>())
            val pathStream: Stream<Path> = Files.list(fileSystem.rootDirectories.iterator().next().resolve("/lang/shared"))
            pathStream.forEach { path -> languageNames.add(path.toString()) }
            fileSystem.close()
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return languageNames
    }

    fun getFileContent(clazz: Class<*>, fileName: String): Map<String, String> {
        val content: MutableMap<String, String> = ConcurrentHashMap()

        try {
            InputStreamReader(getFileFromResourceAsStream(clazz, fileName), StandardCharsets.UTF_8).use { streamReader ->
                BufferedReader(streamReader).use { reader ->
                    val map: Map<*, *> = LudoGame.GSON.fromJson(reader, MutableMap::class.java)
                    for ((key, value) in map) {
                        content[key.toString()] = value.toString()
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return content
    }

    private fun getFileFromResourceAsStream(clazz: Class<*>, fileName: String): InputStream {
        val classLoader = clazz.classLoader
        val inputStream = classLoader.getResourceAsStream(fileName)
        requireNotNull(inputStream) { "file not found! $fileName" }
        return inputStream
    }

}