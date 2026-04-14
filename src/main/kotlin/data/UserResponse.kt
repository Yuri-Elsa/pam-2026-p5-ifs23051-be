package org.delcom.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    var id: String = "",
    var name: String = "",
    var username: String = "",
    var about: String? = null,
    var photo: String? = null,       // path lokal, e.g. "uploads/users/xxx.jpg"
    var urlPhoto: String? = null,    // URL publik, e.g. "/images/users/{id}"
    var createdAt: Instant = Clock.System.now(),
    var updatedAt: Instant = Clock.System.now(),
)