package net.spacetivity.ludo.utils

import com.destroystokyo.paper.profile.PlayerProfile
import com.destroystokyo.paper.profile.ProfileProperty
import net.kyori.adventure.text.Component
import net.spacetivity.ludo.LudoGame
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.SkullMeta
import java.util.*

class ItemBuilder(material: Material) {

    private var itemStack: ItemStack = ItemStack(material)

    private lateinit var itemMeta: ItemMeta
    lateinit var action: (PlayerInteractEvent) -> (Unit)

    init {
        if (itemStack.type != Material.AIR) itemMeta = itemStack.itemMeta
    }

    fun setName(name: Component): ItemBuilder {
        itemMeta.displayName(name)
        itemStack.itemMeta = itemMeta
        return this
    }

    fun setName(name: String): ItemBuilder {
        itemMeta.displayName(Component.text(name))
        itemStack.itemMeta = itemMeta
        return this
    }

    fun setLoreByString(lore: List<String>): ItemBuilder {
        itemMeta.lore(lore.map { s: String -> Component.text(s) })
        itemStack.itemMeta = itemMeta
        return this
    }

    fun setLoreByComponent(lore: List<Component>): ItemBuilder {
        itemMeta.lore(lore)
        itemStack.itemMeta = itemMeta
        return this
    }

    fun setAmount(amount: Int): ItemBuilder {
        itemStack.amount = amount
        return this
    }

    fun setOwner(value: String): ItemBuilder {
        val profile: PlayerProfile = Bukkit.createProfile(UUID.randomUUID().toString().split("-")[0])
        profile.setProperty(ProfileProperty("textures", value))
        val skullMeta: SkullMeta = this.itemMeta as SkullMeta
        skullMeta.playerProfile = profile;
        itemStack.itemMeta = skullMeta
        return this
    }

    fun setArmorColor(color: Color): ItemBuilder {
        val armorMeta = itemMeta as LeatherArmorMeta
        armorMeta.setColor(color)
        itemStack.itemMeta = itemMeta
        return this
    }

    fun addEnchantment(enchantment: Enchantment, level: Int): ItemBuilder {
        itemMeta.addEnchant(enchantment, level, true)
        itemStack.itemMeta = itemMeta
        return this
    }

    fun addFlags(vararg flag: ItemFlag): ItemBuilder {
        itemMeta.addItemFlags(*flag)
        itemStack.itemMeta = itemMeta
        return this
    }

    fun setUnbreakable(): ItemBuilder {
        itemMeta.isUnbreakable = true
        itemStack.itemMeta = itemMeta
        return this
    }

    fun setData(key: String, value: Any): ItemBuilder {
        PersistentDataUtils.setData(this.itemStack, this.itemMeta, key, value)
        return this
    }

    fun onInteract(action: (PlayerInteractEvent) -> (Unit)): ItemBuilder {
        this.action = action
        val clickableItemId: UUID = UUID.randomUUID()
        setData("clickableItem", clickableItemId)
        LudoGame.instance.clickableItems[clickableItemId] = this
        return this
    }

    fun build(): ItemStack {
        return itemStack
    }

}
