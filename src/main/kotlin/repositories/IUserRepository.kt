package org.delcom.repositories

import org.delcom.entities.User

interface IUserRepository {
    suspend fun getById(userId: String): User?
    suspend fun getByUsername(username: String): User?
    suspend fun create(user: User): String
    suspend fun update(id: String, newUser: User): Boolean
    suspend fun updateAbout(id: String, about: String): Boolean
    suspend fun delete(id: String): Boolean
}
