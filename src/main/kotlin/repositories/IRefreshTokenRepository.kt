package org.delcom.repositories

import org.delcom.entities.RefreshToken

interface  IRefreshTokenRepository {
    suspend fun getByToken(refreshToken: String, authToken: String): RefreshToken?
    suspend fun create(newRefreshToken: RefreshToken) : String
    suspend fun delete(authToken: String): Boolean
    suspend fun deleteByUserId(userId: String): Boolean
}