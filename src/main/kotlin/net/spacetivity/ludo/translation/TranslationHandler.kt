package net.spacetivity.ludo.translation

import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.translation.file.TranslationFile
import net.spacetivity.ludo.utils.FileUtils
import java.io.File
import java.nio.file.Path


class TranslationHandler {

    val cachedTranslations: MutableList<Translation> = mutableListOf()

    fun getSelectedTranslation(): Translation {
        return this.cachedTranslations.find { it.name == LudoGame.instance.globalConfigFile.language }!!
    }

    fun getTranslation(name: String): Translation? {
        return this.cachedTranslations.find { it.name == name }
    }

    fun generateTranslations(dataFolderPath: Path, mainClass: Class<*>) {
        for (translationFileName in TranslationFileLoader.getLangFileNamesFromJar("lang", mainClass)) {
            val splittedName: List<String> = translationFileName.split("/")
            val validatedLanguageFileName = splittedName[splittedName.size - 1].split(".")[0]
            val validatedFilePath = splittedName[1] + "/" + splittedName[2]

            this.cachedTranslations.add(Translation(validatedLanguageFileName, mutableMapOf()))

            val cachedMessages: Map<String, String> = TranslationFileLoader.getFileContent(mainClass, validatedFilePath)
            val translationFileResult: TranslationFile = FileUtils.createOrLoadFile(dataFolderPath, "translation", validatedLanguageFileName, TranslationFile::class, TranslationFile(mutableMapOf()))

            val translation: Translation = this.cachedTranslations.find { it.name == validatedLanguageFileName }
                ?: continue

            if (!areMapsEqual(cachedMessages, translationFileResult.messages)) {
                val missingMessages: MutableMap<String, String> = getMissingMessages(cachedMessages, translationFileResult.messages)
                translationFileResult.messages.putAll(missingMessages)

                val oldMessages: MutableMap<String, String> = getOldMessages(cachedMessages, translationFileResult.messages)
                oldMessages.forEach { translationFileResult.messages.remove(it.key) }

                val rawFile: File = FileUtils.readRawFile(dataFolderPath,"translation", validatedLanguageFileName) ?: continue
                FileUtils.save(rawFile, translationFileResult)

                translation.cachedMessages.putAll(missingMessages)
            } else {
                translation.cachedMessages.putAll(cachedMessages)
            }
        }
    }

    private fun getMissingMessages(currentMessages: Map<String, String>, fileMessages: Map<String, String>): MutableMap<String, String> {
        val newMessages: MutableMap<String, String> = mutableMapOf()

        for ((key, value) in currentMessages) {
            if (fileMessages.containsKey(key)) continue
            newMessages[key] = value
        }

        return newMessages
    }

    private fun getOldMessages(currentMessages: Map<String, String>, fileMessages: Map<String, String>): MutableMap<String, String> {
        val newMessages: MutableMap<String, String> = mutableMapOf()

        for ((key, value) in fileMessages) {
            if (currentMessages.containsKey(key)) continue
            newMessages[key] = value
        }

        return newMessages
    }

    private fun <K, V> areMapsEqual(map1: Map<K, V>, map2: Map<K, V>): Boolean {
        if (map1 === map2) return true
        if (map1.size != map2.size) return false
        for ((key, value) in map1) if (value != map2[key]) return false
        return true
    }

}