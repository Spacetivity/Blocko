package net.spacetivity.ludo.database

data class DatabaseFile(val hostname: String, val port: Int, val database: String, val user: String, val password: String)