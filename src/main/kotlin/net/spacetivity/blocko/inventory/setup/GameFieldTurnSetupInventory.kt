package net.spacetivity.blocko.inventory.setup

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.setup.getSetupSession
import net.spacetivity.blocko.arena.setup.step.impl.ScanBoardStep
import net.spacetivity.blocko.field.PathFace
import net.spacetivity.blocko.item.itemStack
import net.spacetivity.blocko.item.meta
import net.spacetivity.blocko.item.name
import net.spacetivity.blocko.item.setValue
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import net.spacetivity.inventory.api.item.InventoryPos
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.SkullMeta

@InventoryProperties(id = "turn_inv", rows = 1, columns = 9)
class GameFieldTurnSetupInventory(private val blockLocation: Location) : InventoryProvider {

    override fun init(player: Player, controller: InventoryController) {
        val translation = BlockoGame.instance.translationHandler.getSelectedTranslation()

        val availablePositions = listOf(
            InventoryPos.of(0, 2),
            InventoryPos.of(0, 3),
            InventoryPos.of(0, 5),
            InventoryPos.of(0, 6)
        )

        val items = initItems(translation, player)

        for (i in items.indices) {
            controller.setItem(availablePositions[i], items[i])
        }
    }

    private fun initItems(translation: Translation, player: Player): List<InteractiveItem> {
        val items = mutableListOf<InteractiveItem>()

        for (pathFace in PathFace.entries) {
            items.add(InteractiveItem.of(itemStack(Material.PLAYER_HEAD) {
                meta<SkullMeta> {
                    name = translation.displayName("blocko.inventory.game_field_set_turn.turn_item.display_name", Placeholder.parsed("face", pathFace.name))
                    lore(translation.lore("blocko.inventory.game_field_set_turn.turn_item.lore"))
                    setValue(pathFace.headValue)
                }
            })
            { _, _, _ ->
                player.closeInventory()
                val setupSession = player.getSetupSession() ?: return@of
                val setupStep = setupSession.getSetupStep(ScanBoardStep::class) ?: return@of
                val gameField = setupStep.gameFields.find { it.x == this.blockLocation.x && it.z == this.blockLocation.z } ?: return@of

                BlockoGame.instance.arenaSetupHandler.setTurningPoint(player, gameField, this.blockLocation, pathFace)
            })
        }

        return items
    }

}