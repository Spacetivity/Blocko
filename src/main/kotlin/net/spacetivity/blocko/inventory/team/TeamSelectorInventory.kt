package net.spacetivity.blocko.inventory.team

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.arena.toGamePlayerInstance
import net.spacetivity.blocko.item.*
import net.spacetivity.blocko.player.GamePlayer
import net.spacetivity.blocko.scoreboard.GameScoreboardUtils
import net.spacetivity.blocko.team.GameTeam
import net.spacetivity.blocko.translation.Translation
import net.spacetivity.blocko.utils.Constants.TEAM_NAME_KEY
import net.spacetivity.blocko.utils.PersistentDataUtils
import net.spacetivity.inventory.api.inventory.InventoryController
import net.spacetivity.inventory.api.inventory.InventoryProperties
import net.spacetivity.inventory.api.inventory.InventoryProvider
import net.spacetivity.inventory.api.item.InteractiveItem
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.meta.LeatherArmorMeta
import java.util.*

@InventoryProperties(id = "team_selector_inv", rows = 1, columns = 9)
class TeamSelectorInventory(private val gameArena: GameArena) : InventoryProvider {

    override fun init(player: Player, controller: InventoryController) {
        val translation: Translation = BlockoGame.instance.translationHandler.getSelectedTranslation()
        val gameTeams: MutableCollection<GameTeam> = BlockoGame.instance.gameTeamHandler.gameTeams[this.gameArena.id]

        for (column in 0..<4) {
            val gameTeam: GameTeam = gameTeams.find { it.teamId == column } ?: continue
            controller.setItem(0, column * 2 + 1, getTeamItem(controller, gameTeam, translation))
        }
    }

    private fun getTeamItem(controller: InventoryController, gameTeam: GameTeam, translation: Translation): InteractiveItem {
        val teamColor: NamedTextColor = gameTeam.color

        return InteractiveItem.of(itemStack(Material.LEATHER_CHESTPLATE) {
            meta<LeatherArmorMeta> {
                name = buildTeamItemDisplayName(gameTeam, translation)
                lore(buildTeamItemLore(gameTeam, translation))
                hideExtraInfo()
                setColor(Color.fromRGB(teamColor.red(), teamColor.green(), teamColor.blue()))
                applyPersistentData(TEAM_NAME_KEY, gameTeam.name)
            }
        }) { _, item: InteractiveItem, event: InventoryClickEvent ->
            val player: Player = event.whoClicked as Player
            val gamePlayer: GamePlayer = player.toGamePlayerInstance() ?: return@of

            val isInTeam: Boolean = gamePlayer.teamName == gameTeam.name

            if (isInTeam) {
                gameTeam.quit(gamePlayer)
            } else {
                if (gamePlayer.teamName != null) {
                    val oldTeamName: String = gamePlayer.teamName!!
                    val oldGameTeam: GameTeam = BlockoGame.instance.gameTeamHandler.getTeam(this.gameArena.id, oldTeamName)
                        ?: return@of

                    BlockoGame.instance.gameTeamHandler.getTeamOfPlayer(gamePlayer.arenaId, gamePlayer.uuid)?.quit(gamePlayer)

                    val oldTeamItem: InteractiveItem = controller.contents.values
                        .filter { it != null && it.item.type == Material.LEATHER_CHESTPLATE }
                        .filter { PersistentDataUtils.has(it!!.item.itemMeta, TEAM_NAME_KEY) }
                        .first { PersistentDataUtils.get(it!!.item.itemMeta, TEAM_NAME_KEY, String::class.java) == oldTeamName }
                        ?: return@of

                    oldTeamItem.update(controller, InteractiveItem.Modification.DISPLAY_NAME, buildTeamItemDisplayName(oldGameTeam, translation))
                    oldTeamItem.update(controller, InteractiveItem.Modification.LORE, buildTeamItemLore(oldGameTeam, translation))
                }

                gameTeam.join(gamePlayer)
            }

            item.update(controller, InteractiveItem.Modification.DISPLAY_NAME, buildTeamItemDisplayName(gameTeam, translation))
            item.update(controller, InteractiveItem.Modification.LORE, buildTeamItemLore(gameTeam, translation))

            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F)

            GameScoreboardUtils.updateTeamLine(player)
        }
    }

    private fun buildTeamItemDisplayName(gameTeam: GameTeam, translation: Translation): Component {
        return translation.displayName("blocko.inventory.team_selector.team_item.display_name",
            Placeholder.parsed("team_color", "<${gameTeam.color.asHexString()}>"),
            Placeholder.parsed("team_name", gameTeam.name),
            Placeholder.parsed("member_count", gameTeam.teamMembers.size.toString()),
            Placeholder.parsed("max_member_count", "1"))
    }

    private fun buildTeamItemLore(gameTeam: GameTeam, translation: Translation): MutableList<Component> {
        val teamMemberUniqueId: UUID? = gameTeam.teamMembers.firstOrNull()
        val memberName: String = if (teamMemberUniqueId == null) "-/-" else Bukkit.getPlayer(teamMemberUniqueId)?.name
            ?: "-/-"

        return translation.lore("blocko.inventory.team_selector.team_item.lore",
            Placeholder.parsed("team_color", "<${gameTeam.color.asHexString()}>"),
            Placeholder.parsed("member_name", memberName),
            Placeholder.parsed("team_name", gameTeam.name))
            .toMutableList()
    }

}