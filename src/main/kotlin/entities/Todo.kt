package org.delcom.entities

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Todo(
    var id : String = UUID.randomUUID().toString(),
    var userId : String,
    var title: String,
    var description: String,
    var isDone: Boolean = false,
    var urgency: String = "medium",
    var cover: String? = null,

    @Contextual
    val createdAt: Instant = Clock.System.now(),
    @Contextual
    var updatedAt: Instant = Clock.System.now(),
)