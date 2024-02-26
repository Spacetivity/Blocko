package net.spacetivity.ludo.utils

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.files.SpaceFile
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KClass

object FileUtils {

    fun <T> read(file: File, clazz: Class<T>): T? {
        return try {
            LudoGame.GSON.fromJson(FileReader(file), clazz)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    fun save(file: File, result: Any) {
        try {
            val fileWriter = FileWriter(file)
            LudoGame.GSON.toJson(result, fileWriter)
            fileWriter.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun <T : SpaceFile> createOrLoadFile(dataFolderPath: Path, subFolderName: String, fileName: String, clazz: KClass<T>, content: T): T {
        val filePath = File("${dataFolderPath}/$subFolderName")
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

    fun readRawFile(dataFolderPath: Path, subFolderName: String, fileName: String): File? {
        val filePath = File("${dataFolderPath}/$subFolderName")

        if (!Files.exists(filePath.toPath())) return null
        val file: File = Paths.get("${filePath}/$fileName.json").toFile()

        return file
    }

}