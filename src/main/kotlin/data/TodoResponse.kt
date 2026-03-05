package org.delcom.data

import kotlinx.serialization.Serializable
import org.delcom.entities.Todo

@Serializable
data class PaginationResponse(
    val currentPage: Int,
    val perPage: Int,
    val total: Long,
    val totalPages: Int,
    val hasNextPage: Boolean,
    val hasPrevPage: Boolean
)

@Serializable
data class TodoListResponse(
    val todos: List<Todo>,
    val pagination: PaginationResponse
)

@Serializable
data class TodoItemResponse(
    val todo: Todo
)

@Serializable
data class TodoIdResponse(
    val todoId: String
)