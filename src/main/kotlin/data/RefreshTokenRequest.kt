package org.delcom.data

import kotlinx.serialization.Serializable
import org.delcom.entities.RefreshToken

@Serializable
data class RefreshTokenRequest(
    var userId: String = "",
    var refreshToken: String = "",
    var authToken: String = "",
){
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "refreshToken" to refreshToken,
            "authToken" to authToken,
        )
    }

    fun toEntity(): RefreshToken {
        return RefreshToken(
            userId = userId,
            refreshToken = refreshToken,
            authToken = authToken,
        )
    }

}