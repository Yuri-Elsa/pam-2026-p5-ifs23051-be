package org.delcom.dao

import org.delcom.tables.TodoTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import java.util.UUID


class TodoDAO(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, TodoDAO>(TodoTable)

    var userId by TodoTable.userId
    var title by TodoTable.title
    var description by TodoTable.description
    var cover by TodoTable.cover
    var isDone by TodoTable.isDone
    var urgency by TodoTable.urgency
    var createdAt by TodoTable.createdAt
    var updatedAt by TodoTable.updatedAt
}
