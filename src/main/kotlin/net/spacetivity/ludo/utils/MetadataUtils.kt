@file:Suppress("UNCHECKED_CAST")

package net.spacetivity.ludo.utils

import net.spacetivity.ludo.LudoGame
import org.bukkit.entity.LivingEntity
import org.bukkit.metadata.FixedMetadataValue

object MetadataUtils {

    fun setIfAbsent(entity: LivingEntity, key: String, value: Any) {
        if (has(entity, key)) return
        entity.setMetadata(key, FixedMetadataValue(LudoGame.instance, value))
    }

    fun apply(entity: LivingEntity, key: String, value: Any) {
        if (has(entity, key)) remove(entity, key)
        entity.setMetadata(key, FixedMetadataValue(LudoGame.instance, value))
    }

    fun remove(entity: LivingEntity, key: String) {
        if (!has(entity, key)) return
        entity.removeMetadata(key, LudoGame.instance)
    }

    fun has(entity: LivingEntity, key: String): Boolean = entity.hasMetadata(key)
    fun <T> get(entity: LivingEntity, key: String): T? = entity.getMetadata(key)[0].value() as T?

}