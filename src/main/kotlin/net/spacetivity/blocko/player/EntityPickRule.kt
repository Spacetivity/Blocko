package net.spacetivity.blocko.player

enum class EntityPickRule(val weight: Int, val probability: Double) {

    MOVABLE_OUT_OF_START(5, 1.0),
    MOVABLE_AWAY_FROM_FIRST_FIELD(4, 1.0),
    MOVABLE_AND_GARAGE_ENTRANCE_POSSIBLE(3, 0.6),
    MOVABLE_AND_TARGET_IN_SIGHT(3, 0.4),
    MOVABLE(2, 0.6),
    MOVABLE_BUT_LANDS_AFTER_OPPONENT(1, 0.4),
    NOT_MOVABLE(0, 1.0);

}