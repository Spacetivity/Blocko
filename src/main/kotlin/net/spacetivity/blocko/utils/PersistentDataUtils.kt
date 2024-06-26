package net.spacetivity.blocko.utils

import net.spacetivity.blocko.BlockoGame
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType


object PersistentDataUtils {

    fun hasData(itemMeta: ItemMeta, key: String): Boolean {
        val namespacedKey = NamespacedKey(BlockoGame.instance, key)
        return itemMeta.persistentDataContainer.has(namespacedKey, PersistentDataType.STRING)
    }

    fun <T> getData(itemMeta: ItemMeta, key: String, clazz: Class<T>): T {
        val namespacedKey = NamespacedKey(BlockoGame.instance, key)
        val jsonString: String = itemMeta.persistentDataContainer.getOrDefault(namespacedKey, PersistentDataType.STRING, "")
        return BlockoGame.GSON.fromJson(jsonString, clazz)
    }

    fun setData(itemStack: ItemStack, itemMeta: ItemMeta, key: String, data: Any) {
        val namespacedKey = NamespacedKey(BlockoGame.instance, key)
        val dataContainer: PersistentDataContainer = itemMeta.persistentDataContainer
        if (dataContainer.has(namespacedKey, PersistentDataType.STRING)) return
        dataContainer.set(namespacedKey, PersistentDataType.STRING, BlockoGame.GSON.toJson(data))
        itemStack.setItemMeta(itemMeta)
    }

}