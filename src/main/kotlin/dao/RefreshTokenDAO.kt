package org.delcom.dao

import org.delcom.tables.RefreshTokenTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import java.util.UUID

class RefreshTokenDAO(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, RefreshTokenDAO>(RefreshTokenTable)

    var userId by RefreshTokenTable.userId
    var refreshToken by RefreshTokenTable.refreshToken
    var authToken by RefreshTokenTable.authToken
    var createdAt by RefreshTokenTable.createdAt
}