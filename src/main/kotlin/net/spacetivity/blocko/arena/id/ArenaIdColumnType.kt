package net.spacetivity.blocko.arena.id

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table

class ArenaIdColumnType(private val length: Int) : ColumnType<ArenaId>() {
    override fun sqlType() = "VARCHAR($length)"

    override fun valueFromDB(value: Any): ArenaId = when (value) {
        is ArenaId -> value
        is String -> ArenaId(value)
        else -> error("Unexpected value for ArenaId: $value")
    }

    override fun notNullValueToDB(value: ArenaId): Any = value.value

    override fun nonNullValueToString(value: ArenaId): String = "'${value.value}'"
}

fun Table.arenaId(name: String, length: Int = 30): Column<ArenaId> = registerColumn(name, ArenaIdColumnType(length))