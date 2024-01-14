package net.spacetivity.ludo.utils

import com.destroystokyo.paper.profile.PlayerProfile
import com.destroystokyo.paper.profile.ProfileProperty
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.SkullMeta
import java.util.*

class ItemUtils(material: Material) {

    private var itemStack: ItemStack = ItemStack(material)
    private lateinit var itemMeta: ItemMeta

    init {
        if (itemStack.type != Material.AIR) itemMeta = itemStack.itemMeta
    }

    fun setName(name: Component): ItemUtils {
        itemMeta.displayName(name)
        itemStack.itemMeta = itemMeta
        return this
    }

    fun setName(name: String): ItemUtils {
        itemMeta.displayName(Component.text(name))
        itemStack.itemMeta = itemMeta
        return this
    }

    fun setLoreByString(lore: MutableList<String>): ItemUtils {
        itemMeta.lore(lore.map { s: String -> Component.text(s) })
        itemStack.itemMeta = itemMeta
        return this
    }

    fun setLoreByComponent(lore: MutableList<Component>): ItemUtils {
        itemMeta.lore(lore)
        itemStack.itemMeta = itemMeta
        return this
    }

    fun setAmount(amount: Int): ItemUtils {
        itemStack.amount = amount
        return this
    }

    fun setOwner(value: String): ItemUtils {
        val profile: PlayerProfile = Bukkit.createProfile(UUID.randomUUID().toString().split("-")[0])
        profile.setProperty(ProfileProperty("textures", value))
        val skullMeta: SkullMeta = this.itemMeta as SkullMeta
        skullMeta.playerProfile = profile;
        itemStack.itemMeta = skullMeta
        return this
    }

    fun setArmorColor(color: Color): ItemUtils {
        val armorMeta = itemMeta as LeatherArmorMeta
        armorMeta.setColor(color)
        itemStack.itemMeta = itemMeta
        return this
    }

    fun addEnchantment(enchantment: Enchantment, level: Int): ItemUtils {
        itemMeta.addEnchant(enchantment, level, true)
        itemStack.itemMeta = itemMeta
        return this
    }

    fun addFlags(vararg flag: ItemFlag): ItemUtils {
        itemMeta.addItemFlags(*flag)
        itemStack.itemMeta = itemMeta
        return this
    }

    fun setUnbreakable(): ItemUtils {
        itemMeta.isUnbreakable = true
        itemStack.itemMeta = itemMeta
        return this
    }

    fun build(): ItemStack {
        return itemStack
    }

}
