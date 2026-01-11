package net.spacetivity.blocko.item

import com.destroystokyo.paper.profile.ProfileProperty
import net.kyori.adventure.text.Component
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.utils.Constants
import net.spacetivity.blocko.utils.PersistentDataUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import java.util.*

inline fun itemStack(material: Material, builder: ItemStack.() -> Unit) = ItemStack(material).apply(builder)

inline fun <reified T : ItemMeta> ItemStack.meta(builder: T.() -> Unit) {
    val curMeta = itemMeta as? T
    itemMeta = if (curMeta != null) {
        curMeta.apply(builder)
        curMeta
    } else {
        itemMeta(type, builder)
    }
}

@JvmName("simpleMeta")
inline fun ItemStack.meta(builder: ItemMeta.() -> Unit) = meta<ItemMeta>(builder)

inline fun <reified T : ItemMeta> ItemStack.setMeta(builder: T.() -> Unit) {
    itemMeta = itemMeta(type, builder)
}

fun ItemStack.onInteract(action: (PlayerInteractEvent) -> Unit): ItemStack {
    val id = UUID.randomUUID()
    this.meta {
        applyPersistentData(Constants.INTERACTIVE_ITEMSTACK_KEY, id)
    }
    Blocko.instance.interactiveActions[id] = action
    return this
}

@JvmName("simpleSetMeta")
inline fun ItemStack.setMeta(builder: ItemMeta.() -> Unit) = setMeta<ItemMeta>(builder)

inline fun <reified T : ItemMeta> itemMeta(material: Material, builder: T.() -> Unit): T? {
    val meta = Bukkit.getItemFactory().getItemMeta(material)
    return if (meta is T) meta.apply(builder) else null
}

@JvmName("simpleItemMeta")
inline fun itemMeta(material: Material, builder: ItemMeta.() -> Unit) = itemMeta<ItemMeta>(material, builder)

fun SkullMeta.setValue(value: String) {
    val profile = Bukkit.createProfile(UUID.randomUUID().toString().split("-")[0])
    profile.setProperty(ProfileProperty("textures", value))
    playerProfile = profile
}

fun ItemMeta.flag(itemFlag: ItemFlag) = addItemFlags(itemFlag)

fun ItemMeta.flags(vararg itemFlag: ItemFlag) = addItemFlags(*itemFlag)

fun ItemMeta.hideExtraInfo() {
    flag(ItemFlag.HIDE_ATTRIBUTES)
    flag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
    flag(ItemFlag.HIDE_ENCHANTS)
    flag(ItemFlag.HIDE_UNBREAKABLE)
    flag(ItemFlag.HIDE_DYE)
    flag(ItemFlag.HIDE_ARMOR_TRIM)
}

fun ItemMeta.applyPersistentData(key: String, value: Any) =
    PersistentDataUtils.apply(this, key, value)

var ItemMeta.name: Component?
    get() = if (hasDisplayName()) displayName() else null
    set(value) = displayName(value ?: Component.space())