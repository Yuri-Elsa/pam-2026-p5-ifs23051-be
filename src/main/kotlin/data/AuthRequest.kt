package org.delcom.data

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.delcom.entities.User

@Serializable
data class AuthRequest(
    var name: String = "",
    var username: String = "",
    var password: String = "",
    var newPassword: String = "",
    var about: String? = null,
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "username" to username,
            "password" to password,
            "newPassword" to newPassword,
            "about" to about,
        )
    }

    fun toEntity(): User {
        return User(
            name = name,
            username = username,
            password = password,
            about = about,
            updatedAt = Clock.System.now()
        )
    }
}
