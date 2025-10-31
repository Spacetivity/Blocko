package net.spacetivity.blocko.utils

import net.kyori.adventure.text.format.NamedTextColor
import net.spacetivity.blocko.team.GameTeam

object Constants {

    const val BOT_SKULL = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmFiYmY1NGQ2ZDI1MDE0N2U2ZTdhYjA3OWM5ZThjNzYyOTAwNTBjMDA4NmUyNDRjOWZmODFjMTU4M2Q5MDg5YSJ9fX0="

    const val NORTH_SKULL = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjEyYzdhZmVhNDhlNTMzMjVlNTEyOTAzOGE0NWFlYzUxYWZlMjU2YWJjYTk0MWI2YmM4MjA2ZmFlMWNlZiJ9fX0="
    const val SOUTH_SKULL = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWYyMmQ3Y2Q1M2Q1YmZlNjFlYWZiYzJmYjFhYzk0NDQzZWVjMjRmNDU1MjkyMTM5YWM5ZmJkYjgzZDBkMDkifX19"
    const val EAST_SKULL = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2VkOWY0MzFhOTk3ZmNlMGQ4YmUxODQ0ZjYyMDkwYjE3ODNhYzU2OWM5ZDI3OTc1MjgzNDlkMzdjMjE1ZmNjIn19fQ=="
    const val WEST_SKULL = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzljYmM0NjU1MjVlMTZhODk0NDFkNzg5YjcyZjU1NGU4ZmY0ZWE1YjM5MzQ0N2FlZjNmZjE5M2YwNDY1MDU4In19fQ=="

    const val DICE_ONE_SKULL = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTMxMzVlYTMxYmMxNWJlMTM0NjJiZjEwZTkxMmExNDBlNWE3ZDY4ZWY0YmQyNmUzZDc1MDU1OWQ1MDJiZjk1In19fQ=="
    const val DICE_TWO_SKULL = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjFhZmM1YzkzZmM1MzMyMzNkZWY1ODU4ZDE5YTNhMWI1NzY0YzViMmRjZTZiNWQxZjc5Mzg2ZTk2NDA1MDNhZiJ9fX0="
    const val DICE_THREE_SKULL = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODQ1NTVhMzY0MTE5NWMxNjg2MGU4MmYzODlmZDI3Y2JkMTE3ODA0OWJkN2IxYmI3N2IwMzFmYjM5OGE2NDQ4MiJ9fX0="
    const val DICE_FOUR_SKULL = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2NmMTliYmJiMTNhMWYzNWFjOGYxNDFjZmNlZjlkMDA4NGQxNzZlY2I0ZjRlZWZiNThhZmRhMzUzMGQwYTcyNyJ9fX0="
    const val DICE_FIVE_SKULL = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDM1MWFmNDk5ZjRiZjBiNmNmYWI3YTFmNjI2MWM1YzExYWUyY2RjMDE5ODI1YWFkYjk2OWQ1NjdmZjM1NDUzNSJ9fX0="
    const val DICE_SIX_SKULL = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmNjZDc1M2RiMTlmYmZjZDNhNTRmNmZkZDBhYTQ1ZDFhM2JmMjVjNjM3ZDY2N2M0M2U2NDZiMWEzOTBmYTYyZCJ9fX0="

    const val INTERACTIVE_ITEMSTACK_KEY = "interactive_item_stack"
    const val BALANCE_ITEM_KEY = "balance_item"
    const val GAME_ENTITY_TYPE_KEY = "game_entity_type"
    const val TEAM_NAME_KEY = "team_name"
    const val ENTITY_SELECTOR_KEY = "entity_selector"
    const val SETUP_TOOL_KEY = "setup_tool"

    const val TIMEOUT_BOSSBAR_NAME = "timeout_bar"
    const val PLACEHOLDER = "-/-"

    // Task tick intervals (in ticks, where 1 tick = 50ms)
    const val MAIN_TASK_TICK_INTERVAL = 1L
    const val MOVEMENT_TASK_TICK_INTERVAL = 10L
    const val PLAYER_TASK_TICK_INTERVAL = 10L

    // Arena/Player related constants
    const val Y_LEVEL_FALL_THRESHOLD = 10.0
    
    // Time thresholds for bossbar colors (in seconds)
    const val BOSSBAR_GREEN_THRESHOLD = 30
    const val BOSSBAR_YELLOW_THRESHOLD = 10

    // Action timeout (in milliseconds)
    const val TOTAL_ACTION_TIME_MS = 60_000L

    val GAME_TEAMS = listOf(
        GameTeam("red", NamedTextColor.RED, 0),
        GameTeam("green", NamedTextColor.DARK_GREEN, 1),
        GameTeam("blue", NamedTextColor.BLUE, 2),
        GameTeam("yellow", NamedTextColor.YELLOW, 3),
    )

}