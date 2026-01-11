package net.spacetivity.blocko.inventory.setup

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.field.GameFieldRotation
import net.spacetivity.blocko.item.itemStack
import net.spacetivity.blocko.item.meta
import net.spacetivity.blocko.item.name
import net.spacetivity.blocko.item.setValue
import net.spacetivity.blocko.setup.getSetupSession
import net.spacetivity.blocko.setup.step.impl.step.SetTeamEntrancesStep
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.ScoreboardUtils
import net.spacetivity.inventorylib.api.GuiProvider
import net.spacetivity.inventorylib.api.inventory.Gui
import net.spacetivity.inventorylib.api.inventory.GuiController
import net.spacetivity.inventorylib.api.inventory.GuiProperties
import net.spacetivity.inventorylib.api.item.GuiItem
import net.spacetivity.inventorylib.api.item.GuiPos
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.SkullMeta

@GuiProperties(id = "turn_inv", rows = 1, columns = 9, playSoundOnOpen = false, playSoundOnClose = false)
class GameFieldTurnSetupInventory(private val gameField: GameField?, private val isTeamEntrance: Boolean) : Gui {

    override fun init(player: Player, controller: GuiController) {
        val translation = Blocko.instance.translationHandler.getSelectedTranslation()

        val availablePositions = listOf(
            GuiPos.of(0, 2),
            GuiPos.of(0, 3),
            GuiPos.of(0, 5),
            GuiPos.of(0, 6)
        )

        val items = initItems(translation, player)

        for (i in items.indices) {
            controller.setItem(availablePositions[i], items[i])
        }
    }

    private fun initItems(translation: Translation, player: Player): List<GuiItem> {
        val items = mutableListOf<GuiItem>()
        val setupSession = player.getSetupSession() ?: return items

        for (pathFace in GameFieldRotation.entries) {
            items.add(GuiProvider.api.of(itemStack(Material.PLAYER_HEAD) {
                meta<SkullMeta> {
                    name = translation.displayName("blocko.inventory.game_field_set_turn.turn_item.display_name", Placeholder.parsed("face", pathFace.name))
                    lore(translation.lore("blocko.inventory.game_field_set_turn.turn_item.lore"))
                    setValue(pathFace.headValue)
                }
            })
            { _, _, _ ->
                player.closeInventory()

                if (this.gameField == null) {
                    player.translateMessage("blocko.setup.no_field_found_at_location")
                    return@of
                }

                Blocko.instance.arenaSetupHandler.setTurningPoint(player, gameField, pathFace)

                if (this.isTeamEntrance) {
                    val teamEntranceStep = setupSession.getSetupStep<SetTeamEntrancesStep>()!!
                    ScoreboardUtils.updateSetupDataLines(player, teamEntranceStep)
                }

                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 0.5f)
            })
        }

        return items
    }

}