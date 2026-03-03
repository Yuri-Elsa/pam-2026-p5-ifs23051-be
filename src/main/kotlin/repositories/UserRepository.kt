package org.delcom.repositories

import org.delcom.dao.UserDAO
import org.delcom.entities.User
import org.delcom.helpers.suspendTransaction
import org.delcom.helpers.userDAOToModel
import org.delcom.tables.UserTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.update
import java.util.*

class UserRepository : IUserRepository {
    override suspend fun getById(userId: String): User? = suspendTransaction {
        UserDAO
            .find { (UserTable.id eq UUID.fromString(userId)) }
            .limit(1)
            .map(::userDAOToModel)
            .firstOrNull()
    }

    override suspend fun getByUsername(username: String): User? = suspendTransaction {
        UserDAO
            .find { (UserTable.username eq username) }
            .limit(1)
            .map(::userDAOToModel)
            .firstOrNull()
    }

    override suspend fun create(user: User): String = suspendTransaction {
        val userDAO = UserDAO.new {
            name = user.name
            username = user.username
            password = user.password
            about = user.about
            createdAt = user.createdAt
            updatedAt = user.updatedAt
        }

        userDAO.id.value.toString()
    }

    override suspend fun update(id: String, newUser: User): Boolean = suspendTransaction {
        val userDAO = UserDAO
            .find { UserTable.id eq UUID.fromString(id) }
            .limit(1)
            .firstOrNull()

        if (userDAO != null) {
            userDAO.name = newUser.name
            userDAO.username = newUser.username
            userDAO.password = newUser.password
            userDAO.photo = newUser.photo
            userDAO.about = newUser.about
            userDAO.updatedAt = newUser.updatedAt
            true
        } else {
            false
        }
    }

    override suspend fun updateAbout(id: String, about: String): Boolean = suspendTransaction {
        val rows = UserTable.update({ UserTable.id eq UUID.fromString(id) }) {
            it[UserTable.about] = about
        }
        rows >= 1
    }

    override suspend fun delete(id: String): Boolean = suspendTransaction {
        val rowsDeleted = UserTable.deleteWhere {
            UserTable.id eq UUID.fromString(id)
        }
        rowsDeleted >= 1
    }
}
