@file:Suppress("UNCHECKED_CAST")

package net.spacetivity.blocko.utils

import net.spacetivity.blocko.Blocko
import org.bukkit.entity.LivingEntity
import org.bukkit.metadata.FixedMetadataValue

object MetadataUtils {

    fun apply(entity: LivingEntity, key: String, value: Any) {
        if (has(entity, key)) remove(entity, key)
        entity.setMetadata(key, FixedMetadataValue(Blocko.instance, value))
    }

    fun remove(entity: LivingEntity, key: String) {
        if (!has(entity, key)) return
        entity.removeMetadata(key, Blocko.instance)
    }

    fun has(entity: LivingEntity, key: String): Boolean = entity.hasMetadata(key)
    fun <T> get(entity: LivingEntity, key: String): T? = entity.getMetadata(key)[0].value() as T?

}