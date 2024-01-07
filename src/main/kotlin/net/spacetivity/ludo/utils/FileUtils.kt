package net.spacetivity.ludo.utils

import net.spacetivity.ludo.LudoGame
import java.io.*

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

}