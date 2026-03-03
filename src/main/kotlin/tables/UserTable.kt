package org.delcom.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object UserTable : UUIDTable("users") {
    val name = varchar("name", 100)
    val username = varchar("username", 50)
    val password = varchar("password", 255)
    val photo = varchar("photo", 255).nullable()
    val about = text("about").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
