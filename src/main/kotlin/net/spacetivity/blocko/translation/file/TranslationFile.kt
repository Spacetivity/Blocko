package net.spacetivity.blocko.translation.file

import net.spacetivity.blocko.files.SpaceFile

data class TranslationFile(val messages: MutableMap<String, String>) : SpaceFile