package net.spacetivity.blocko.translation

import net.spacetivity.blocko.BlockoGame


class TranslationHandler {

    val cachedTranslations = mutableListOf<Translation>()

    fun getSelectedTranslation(): Translation {
        return this.cachedTranslations.find { it.name == BlockoGame.instance.globalConfigFile.language }!!
    }

    fun getTranslation(name: String): Translation? {
        return this.cachedTranslations.find { it.name == name }
    }

    fun generateTranslations(mainClass: Class<*>) {
        for (translationFileName in TranslationFileLoader.getLangFileNamesFromJar("lang", mainClass)) {
            val splittedName = translationFileName.split("/")
            val validatedLanguageFileName = splittedName[splittedName.size - 1].split(".")[0]

            val translation = Translation(validatedLanguageFileName, mutableMapOf())
            this.cachedTranslations.add(translation)

            //TODO: later implement checking if messages were added or deleted
            TranslationFileLoader.copyTranslationFileToDataFolder(validatedLanguageFileName)

            val messagesInFile = TranslationFileLoader.getFileContent(validatedLanguageFileName)
            translation.cachedMessages.putAll(messagesInFile)
        }
    }

}