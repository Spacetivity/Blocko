package net.spacetivity.blocko.inventory.setup

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.Blocko
import net.spacetivity.blocko.field.GameField
import net.spacetivity.blocko.item.hideExtraInfo
import net.spacetivity.blocko.item.itemStack
import net.spacetivity.blocko.item.meta
import net.spacetivity.blocko.item.name
import net.spacetivity.blocko.setup.getSetupSession
import net.spacetivity.blocko.setup.step.impl.step.ScanBoardStep
import net.spacetivity.blocko.setup.step.impl.step.SetTeamPathsStep
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.translation.translateMessage
import net.spacetivity.blocko.utils.InventoryUtils
import net.spacetivity.blocko.utils.ScoreboardUtils
import net.spacetivity.inventorylib.api.GuiProvider
import net.spacetivity.inventorylib.api.inventory.Gui
import net.spacetivity.inventorylib.api.inventory.GuiController
import net.spacetivity.inventorylib.api.inventory.GuiProperties
import net.spacetivity.inventorylib.api.item.GuiItem
import net.spacetivity.inventorylib.api.item.GuiPos
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.LeatherArmorMeta

@GuiProperties(id = "garage_field_inv", rows = 1, columns = 9)
class GameTeamSetupInventory(private val type: InvType, private val gameField: GameField?) : Gui {

    private val highlightHandler = Blocko.instance.gameFieldHighlightHandler

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

        for (gameTeam in setupSession.gameTeams) {
            items.add(
                GuiProvider.api.of(itemStack(Material.LEATHER_CHESTPLATE) {
                    meta<LeatherArmorMeta> {
                        name = translation.displayName(
                            "blocko.inventory.game_team_setup.team_item.display_name",
                            Placeholder.parsed("team_color", "<${gameTeam.color.asHexString()}>"),
                            Placeholder.parsed(
                                "team_name",
                                gameTeam.name.lowercase().replaceFirstChar { it.uppercase() })
                        )

                        lore(translation.lore("blocko.inventory.game_team_setup.team_item.lore.entrance"))
                        setColor(Color.fromRGB(gameTeam.color.red(), gameTeam.color.green(), gameTeam.color.blue()))
                        hideExtraInfo()
                    }
                })
                { _, _, _ ->
                    player.closeInventory()

                    val setupStep = setupSession.getSetupStep<ScanBoardStep>() ?: return@of

                    when (this.type) {
                        InvType.IDS -> {
                            if (setupSession.currentTeamName == gameTeam.name) return@of

                            for (gameField in setupStep.gameFields) {
                                val oldHighlightMode = gameField.currentHighlightMode
                                if (oldHighlightMode == null) continue
                                this.highlightHandler.spawnOrUpdateHighlightEntity(
                                    setupSession.arenaId,
                                    gameField.getWorldPosition(true),
                                    oldHighlightMode
                                )
                            }

                            setupSession.currentTeamName = gameTeam.name
                            setupStep.fieldIndex = 0

                            val teamPathsStep = setupSession.getSetupStep<SetTeamPathsStep>()!!
                            ScoreboardUtils.updateSetupDataLines(player, teamPathsStep)

                            player.translateMessage(
                                "blocko.inventory.game_team_setup.team_item.click.set_field_ids",
                                Placeholder.parsed("team_color", "<${gameTeam.color.asHexString()}>"),
                                Placeholder.parsed(
                                    "team_name",
                                    gameTeam.name.lowercase().replaceFirstChar { it.uppercase() })
                            )
                        }

                        else -> {
                            if (this.gameField == null) {
                                player.translateMessage("blocko.setup.no_field_found_at_location")
                                return@of
                            }

                            this.gameField.properties.teamEntrance = gameTeam.name
                            InventoryUtils.openGameFieldTurnInventory(player, gameField, true)
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