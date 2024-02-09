package net.spacetivity.ludo.files

data class ItemsFile(override val fileName: String, override val subFolderName: String, val setupItemType: String) : SpaceFile
