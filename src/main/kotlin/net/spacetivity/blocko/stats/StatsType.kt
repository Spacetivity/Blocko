package net.spacetivity.blocko.stats

enum class StatsType(val nameKey: String) {
    ELIMINATED_OPPONENTS("blocko.stats.type.eliminations"),
    KNOCKED_OUT_BY_OPPONENTS("blocko.stats.type.knocked_out_by_opponents"),
    COINS("blocko.stats.type.coins"),
    PLAYED_GAMES("blocko.stats.type.played_games"),
    WON_GAMES("blocko.stats.type.won_games");
}