package net.spacetivity.blocko.utils

import net.spacetivity.blocko.BlockoGame
import org.bukkit.NamespacedKey
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

object PersistentDataUtils {

    fun apply(itemMeta: ItemMeta, key: String, data: Any) {
        val namespacedKey = NamespacedKey(BlockoGame.instance, key)

        if (itemMeta.persistentDataContainer.has(namespacedKey, PersistentDataType.STRING)) return
        itemMeta.persistentDataContainer.set(namespacedKey, PersistentDataType.STRING, BlockoGame.GSON.toJson(data))
    }

    fun remove(itemMeta: ItemMeta, key: String) {
        val namespacedKey = NamespacedKey(BlockoGame.instance, key)

        if (!itemMeta.persistentDataContainer.has(namespacedKey, PersistentDataType.STRING)) return
        itemMeta.persistentDataContainer.remove(namespacedKey)
    }

    fun has(itemMeta: ItemMeta, key: String): Boolean {
        val namespacedKey = NamespacedKey(BlockoGame.instance, key)
        return itemMeta.persistentDataContainer.has(namespacedKey, PersistentDataType.STRING)
    }

    fun <T> get(itemMeta: ItemMeta, key: String, clazz: Class<T>): T {
        val namespacedKey = NamespacedKey(BlockoGame.instance, key)
        val jsonString: String = itemMeta.persistentDataContainer.getOrDefault(namespacedKey, PersistentDataType.STRING, "")
        return BlockoGame.GSON.fromJson(jsonString, clazz)
    }

}