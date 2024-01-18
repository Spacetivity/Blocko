package net.spacetivity.ludo.dice

import net.spacetivity.ludo.LudoGame
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class DiceFileHandler {

    val diceSides: MutableList<Pair<Int, String>> = loadDiceSidesFromConfig()

    private fun loadDiceSidesFromConfig(): MutableList<Pair<Int, String>> {
        val configFile = File(LudoGame.instance.dataFolder, "dice.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)

        val result: MutableList<Pair<Int, String>> = mutableListOf()

        if (!configFile.exists()) return result

        try {
            val diceSidesList = mutableListOf<Pair<Int, String>>()
            val diceSidesSection = config.getConfigurationSection("diceSides")

            diceSidesSection?.getKeys(false)?.forEach { key ->
                val number = key.toIntOrNull()
                val value = diceSidesSection.getString("$key.value")
                if (number != null && value != null) diceSidesList.add(number to value)
            }

            return result
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

}