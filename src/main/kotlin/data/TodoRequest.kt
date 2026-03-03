package org.delcom.data

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.delcom.entities.Todo
import java.util.UUID

@Serializable
data class TodoRequest(
    var userId: String = "",
    var title: String = "",
    var description: String = "",
    var cover: String? = null,
    var isDone: Boolean = false,
    var urgency: String = "medium",
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "title" to title,
            "description" to description,
            "cover" to cover,
            "isDone" to isDone,
            "urgency" to urgency,
        )
    }

    fun toEntity(): Todo {
        return Todo(
            userId = userId,
            title = title,
            description = description,
            cover = cover,
            isDone = isDone,
            urgency = urgency,
            updatedAt = Clock.System.now()
        )
    }
}
