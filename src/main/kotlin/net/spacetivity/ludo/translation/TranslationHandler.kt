package net.spacetivity.ludo.translation

import net.spacetivity.ludo.LudoGame


class TranslationHandler {

    val cachedTranslations: MutableList<Translation> = mutableListOf()

    fun getSelectedTranslation(): Translation {
        return this.cachedTranslations.find { it.name == LudoGame.instance.globalConfigFile.language }!!
    }

    fun getTranslation(name: String): Translation? {
        return this.cachedTranslations.find { it.name == name }
    }

    fun generateTranslations(mainClass: Class<*>) {
        for (translationFileName in TranslationFileLoader.getLangFileNamesFromJar("lang", mainClass)) {
            val splittedName: List<String> = translationFileName.split("/")
            val validatedLanguageFileName = splittedName[splittedName.size - 1].split(".")[0]

            println("$translationFileName - $validatedLanguageFileName")

            val translation = Translation(validatedLanguageFileName, mutableMapOf())
            this.cachedTranslations.add(translation)

            TranslationFileLoader.copyTranslationFileToDataFolder(validatedLanguageFileName)

            val messagesInFile: Map<String, String> = TranslationFileLoader.getFileContent(validatedLanguageFileName)
            translation.cachedMessages.putAll(messagesInFile)
        }
    }

}