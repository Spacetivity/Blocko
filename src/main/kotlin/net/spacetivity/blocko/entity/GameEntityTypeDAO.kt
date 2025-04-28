package net.spacetivity.blocko.entity

import org.jetbrains.exposed.sql.Table

object GameEntityTypeDAO : Table("unlocked_entity_types") {
    val uuid = varchar("uuid", 36)
    val entityTypeName = varchar("entityTypeName", 20)
}