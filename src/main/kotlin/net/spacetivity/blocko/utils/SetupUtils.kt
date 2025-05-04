package net.spacetivity.blocko.utils

import net.spacetivity.blocko.arena.setup.ArenaSetupSession
import net.spacetivity.blocko.arena.setup.getSetupSession
import net.spacetivity.blocko.translation.translateMessage
import org.bukkit.entity.Player

object SetupUtils {

    fun checkSetupMode(player: Player, result: (ArenaSetupSession) -> Unit) {
        val setupSession = player.getSetupSession()
        if (setupSession == null) {
            player.translateMessage("blocko.setup.not_in_setup_mode")
            return
        }

        result.invoke(setupSession)
    }

}