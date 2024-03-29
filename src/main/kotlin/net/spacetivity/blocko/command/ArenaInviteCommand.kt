package net.spacetivity.blocko.command

import net.spacetivity.blocko.BlockoGame
import net.spacetivity.blocko.arena.GameArena
import net.spacetivity.blocko.command.api.CommandProperties
import net.spacetivity.blocko.command.api.SpaceCommandExecutor
import net.spacetivity.blocko.command.api.SpaceCommandSender
import net.spacetivity.blocko.extensions.getArena
import net.spacetivity.blocko.extensions.getPossibleInvitationDestination
import net.spacetivity.blocko.extensions.toGamePlayerInstance
import net.spacetivity.blocko.extensions.translateMessage
import net.spacetivity.blocko.player.GamePlayer
import org.bukkit.Bukkit
import org.bukkit.entity.Player

@CommandProperties(name = "arenainvite", "blocko.arenainvite", ["ai"])
class ArenaInviteCommand : SpaceCommandExecutor {

    override fun execute(sender: SpaceCommandSender, args: List<String>) {
        if (!sender.isPlayer) return

        val player: Player = sender.castTo(Player::class.java)

        if (args.size == 2 && args[0].equals("send", true)) {
            val gameArena: GameArena? = player.getArena()

            if (gameArena == null) {
                player.translateMessage("blocko.command.arena_invite.not_in_a_game")
                return
            }

            if (gameArena.arenaHost != null && gameArena.arenaHost!!.uuid != player.uniqueId) {
                player.translateMessage("blocko.command.arena_invite.not_the_host_player")
                return
            }

            val name: String = args[1]
            val gamePlayer: GamePlayer = player.toGamePlayerInstance() ?: return
            gameArena.sendArenaInvite(gamePlayer, name)
            return
        }

        if (args.size == 2 && args[0].equals("accept", true)) {
            val arenaId: String = args[1]

            validateInvitation(arenaId, player) { gameArena: GameArena ->
                val wasJoinSuccessful: Boolean = gameArena.join(player.uniqueId, false)
                if (wasJoinSuccessful) gameArena.invitedPlayers.remove(player.uniqueId)
            }

            return
        }

        if (args.size == 2 && args[0].equals("deny", true)) {
            val arenaId: String = args[1]

            validateInvitation(arenaId, player) { gameArena: GameArena ->
                gameArena.invitedPlayers.remove(player.uniqueId)
                player.translateMessage("blocko.command.arena_invite.invitation_denied")
            }

            return
        }

        sendUsage(sender)
    }

    override fun sendUsage(sender: SpaceCommandSender) {
        sender.castTo(Player::class.java).translateMessage("blocko.command.arena.arena_invite.usage")
    }

    override fun onTabComplete(sender: SpaceCommandSender, args: List<String>): MutableList<String> {
        val result: MutableList<String> = mutableListOf()

        if (!sender.isPlayer) return result
        val player: Player = sender.castTo(Player::class.java)

        if (args.size == 1)
            result.addAll(listOf("send", "accept", "deny"))

        if (args.size == 2 && args[0].equals("send", true)) {
            result.addAll(Bukkit.getOnlinePlayers().filter { it.getArena() == null && it.uniqueId != player.uniqueId }.map { it.name })
        }

        if (args.size == 2 && (args[0].equals("accept", true) || args[0].equals("deny", true))) {
            val gameArena: GameArena = player.getPossibleInvitationDestination() ?: return mutableListOf()
            result.add(gameArena.id)
        }

        return result
    }

    private fun validateInvitation(arenaId: String, player: Player, result: (GameArena) -> Unit) {
        val gameArena: GameArena? = BlockoGame.instance.gameArenaHandler.getArena(arenaId)

        if (gameArena == null) {
            player.translateMessage("blocko.command.blocko.arena_not_exists")
            return
        }

        if (!gameArena.invitedPlayers.contains(player.uniqueId)) {
            player.translateMessage("blocko.command.arena_invite.no_open_invitation")
            return
        }

        if (!gameArena.phase.isIdle()) {
            player.translateMessage("blocko.command.arena_invite.invitation_expired")
            return
        }

        result.invoke(gameArena)
    }

}