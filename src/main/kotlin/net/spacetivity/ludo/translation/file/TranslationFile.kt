package net.spacetivity.ludo.translation.file

import net.spacetivity.ludo.files.SpaceFile

data class TranslationFile(val messages: MutableMap<String, String>) : SpaceFile