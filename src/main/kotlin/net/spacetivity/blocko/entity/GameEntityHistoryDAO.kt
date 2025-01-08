package net.spacetivity.blocko.entity

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object GameEntityHistoryDAO : Table("game_entity_history") {
    val uuid: Column<String> = varchar("uuid", 36)
    val selectedEntityType: Column<GameEntityType> = enumeration("selectedEntityType")

    override val primaryKey: PrimaryKey = PrimaryKey(uuid)
}