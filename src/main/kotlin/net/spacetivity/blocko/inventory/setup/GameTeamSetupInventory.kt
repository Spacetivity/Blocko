package net.spacetivity.blocko.inventory.setup

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.setup.getSetupSession
import net.spacetivity.blocko.arena.setup.step.impl.ScanBoardStep
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.item.hideExtraInfo
import net.spacetivity.blocko.item.itemStack
import net.spacetivity.blocko.item.meta
import net.spacetivity.blocko.item.name
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import net.spacetivity.inventory.api.item.InventoryPos
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.LeatherArmorMeta

@InventoryProperties(id = "garage_field_inv", rows = 1, 9)
class GameTeamSetupInventory(private val type: InvType, private val gameField: GameField?) : InventoryProvider {

    private val highlightHandler = BlockoGame.instance.gameFieldHighlightHandler

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
        val setupSession = player.getSetupSession() ?: return items

        for (gameTeam in setupSession.gameTeams) {
            items.add(InteractiveItem.of(itemStack(Material.LEATHER_CHESTPLATE) {
                meta<LeatherArmorMeta> {
                    name = translation.displayName("blocko.inventory.game_team_setup.team_item.display_name",
                        Placeholder.parsed("team_color", "<${gameTeam.color.asHexString()}>"),
                        Placeholder.parsed("team_name", gameTeam.name.lowercase().replaceFirstChar { it.uppercase() }))

                    lore(translation.lore("blocko.inventory.game_team_setup.team_item.lore.entrance"))
                    setColor(Color.fromRGB(gameTeam.color.red(), gameTeam.color.green(), gameTeam.color.blue()))
                    hideExtraInfo()
                }
            })
            { _, _, _ ->
                player.closeInventory()

                if (this.gameField == null) {
                    player.translateMessage("blocko.setup.no_field_found_at_location")
                    return@of
                }

                val setupStep = setupSession.getSetupStep(ScanBoardStep::class) ?: return@of

                when (this.type) {
                    InvType.IDS -> {
                        if (setupSession.currentTeamName == gameTeam.name) {
                            //TODO: send message that you already have selected this team and show hint in item lore!
                            return@of
                        }

                        for (gameField in setupStep.gameFields) {
                            val oldHighlightMode = gameField.currentHighlightMode
                            if (oldHighlightMode == null) continue
                            this.highlightHandler.spawnOrUpdateHighlightEntity(setupSession.arenaId, gameField.getWorldPosition(true), oldHighlightMode)
                        }

                        setupSession.currentTeamName = gameTeam.name
                        setupStep.fieldIndex = 0

                        player.translateMessage("blocko.inventory.game_team_setup.team_item.click.set_field_ids",
                            Placeholder.parsed("team_color", "<${gameTeam.color.asHexString()}>"),
                            Placeholder.parsed("team_name", gameTeam.name.lowercase().replaceFirstChar { it.uppercase() }))
                    }

                    else -> {
                        this.gameField.properties.teamEntrance = gameTeam.name
                        InventoryUtils.openGameFieldTurnInventory(player, gameField)
                    }
                }
            })
        }

        return items
    }

}

enum class InvType {
    IDS,
    ENTRANCE
}