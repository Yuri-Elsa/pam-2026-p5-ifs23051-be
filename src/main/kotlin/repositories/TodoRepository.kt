package org.delcom.repositories

import org.delcom.dao.TodoDAO
import org.delcom.entities.Todo
import org.delcom.helpers.suspendTransaction
import org.delcom.helpers.todoDAOToModel
import org.delcom.tables.TodoTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import java.util.*

class TodoRepository : ITodoRepository {

    override suspend fun getAll(
        userId: String,
        search: String,
        page: Int,
        perPage: Int,
        isDone: Boolean?,
        urgency: String?
    ): Pair<List<Todo>, Long> = suspendTransaction {
        val offset = ((page - 1) * perPage).toLong()

        val query = TodoTable.selectAll()
            .where { TodoTable.userId eq UUID.fromString(userId) }

        if (search.isNotBlank()) {
            val keyword = "%${search.lowercase()}%"
            query.andWhere { TodoTable.title.lowerCase() like keyword }
        }

        if (isDone != null) {
            query.andWhere { TodoTable.isDone eq isDone }
        }

        if (!urgency.isNullOrBlank()) {
            query.andWhere { TodoTable.urgency eq urgency }
        }

        val total = query.count()

        val todos = TodoDAO.wrapRows(
            query.orderBy(TodoTable.createdAt to SortOrder.DESC)
                .limit(perPage)
                .offset(offset)
        ).map(::todoDAOToModel)

        Pair(todos, total)
    }

    override suspend fun getStats(userId: String): Triple<Long, Long, Long> = suspendTransaction {
        val total = TodoTable.selectAll()
            .where { TodoTable.userId eq UUID.fromString(userId) }
            .count()

        val done = TodoTable.selectAll()
            .where { (TodoTable.userId eq UUID.fromString(userId)) and (TodoTable.isDone eq true) }
            .count()

        val pending = total - done
        Triple(total, done, pending)
    }

    override suspend fun getById(todoId: String): Todo? = suspendTransaction {
        TodoDAO
            .find {
                (TodoTable.id eq UUID.fromString(todoId))
            }
            .limit(1)
            .map(::todoDAOToModel)
            .firstOrNull()
    }

    override suspend fun create(todo: Todo): String = suspendTransaction {
        val todoDAO = TodoDAO.new {
            userId = UUID.fromString(todo.userId)
            title = todo.title
            description = todo.description
            cover = todo.cover
            isDone = todo.isDone
            urgency = todo.urgency
            createdAt = todo.createdAt
            updatedAt = todo.updatedAt
        }

        todoDAO.id.value.toString()
    }

    override suspend fun update(userId: String, todoId: String, newTodo: Todo): Boolean = suspendTransaction {
        val todoDAO = TodoDAO
            .find {
                (TodoTable.id eq UUID.fromString(todoId)) and
                        (TodoTable.userId eq UUID.fromString(userId))
            }
            .limit(1)
            .firstOrNull()

        if (todoDAO != null) {
            todoDAO.title = newTodo.title
            todoDAO.description = newTodo.description
            todoDAO.cover = newTodo.cover
            todoDAO.isDone = newTodo.isDone
            todoDAO.urgency = newTodo.urgency
            todoDAO.updatedAt = newTodo.updatedAt
            true
        } else {
            false
        }
    }

    override suspend fun delete(userId: String, todoId: String): Boolean = suspendTransaction {
        val rowsDeleted = TodoTable.deleteWhere {
            (TodoTable.id eq UUID.fromString(todoId)) and
                    (TodoTable.userId eq UUID.fromString(userId))
        }
        rowsDeleted >= 1
    }
}
