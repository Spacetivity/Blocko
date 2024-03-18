package net.spacetivity.ludo.entity

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object GameEntityTypeDAO : Table("unlocked_entity_types") {
    val uuid: Column<String> = varchar("uuid", 36)
    val entityTypeName: Column<String> = varchar("entityTypeName", 20)
}