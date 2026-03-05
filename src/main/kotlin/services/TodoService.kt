package org.delcom.services

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.delcom.data.AppException
import org.delcom.data.DataResponse
import org.delcom.data.TodoRequest
import org.delcom.entities.Todo
import org.delcom.helpers.ServiceHelper
import org.delcom.helpers.ValidatorHelper
import org.delcom.repositories.ITodoRepository
import org.delcom.repositories.IUserRepository
import java.io.File
import java.util.*

// ── Serializable response wrappers ────────────────────────────────────────────

@Serializable
data class StatsData(
    val total: Long,
    val done: Long,
    val pending: Long
)

@Serializable
data class StatsResponse(
    val stats: StatsData
)

@Serializable
data class PaginationData(
    val currentPage: Int,
    val perPage: Int,
    val total: Long,
    val totalPages: Int,
    val hasNextPage: Boolean,
    val hasPrevPage: Boolean
)

@Serializable
data class TodosResponse(
    val todos: List<Todo>,
    val pagination: PaginationData
)

@Serializable
data class TodoResponse(
    val todo: Todo
)

@Serializable
data class TodoAddResponse(
    val todoId: String
)

// ─────────────────────────────────────────────────────────────────────────────

class TodoService(
    private val userRepo: IUserRepository,
    private val todoRepo: ITodoRepository
) {

    // Mengambil statistik todo saya
    suspend fun getStats(call: ApplicationCall) {
        val user = ServiceHelper.getAuthUser(call, userRepo)

        val (total, done, pending) = todoRepo.getStats(user.id)

        val response = DataResponse(
            status = "success",
            message = "Berhasil mengambil statistik todo",
            data = StatsResponse(
                stats = StatsData(total = total, done = done, pending = pending)
            )
        )
        call.respond(response)
    }

    // Mengambil semua daftar todo saya
    suspend fun getAll(call: ApplicationCall) {
        val user = ServiceHelper.getAuthUser(call, userRepo)

        val search = call.request.queryParameters["search"] ?: ""
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val perPage = call.request.queryParameters["perPage"]?.toIntOrNull() ?: 10
        val isDoneParam = call.request.queryParameters["isDone"]
        val urgency = call.request.queryParameters["urgency"]

        val isDone: Boolean? = when (isDoneParam?.lowercase()) {
            "true" -> true
            "false" -> false
            else -> null
        }

        val (todos, total) = todoRepo.getAll(
            userId = user.id,
            search = search,
            page = page,
            perPage = perPage,
            isDone = isDone,
            urgency = urgency
        )

        val totalPages = if (total == 0L) 1 else Math.ceil(total.toDouble() / perPage).toInt()

        val response = DataResponse(
            status = "success",
            message = "Berhasil mengambil daftar todo saya",
            data = TodosResponse(
                todos = todos,
                pagination = PaginationData(
                    currentPage = page,
                    perPage = perPage,
                    total = total,
                    totalPages = totalPages,
                    hasNextPage = page < totalPages,
                    hasPrevPage = page > 1
                )
            )
        )
        call.respond(response)
    }

    // Mengambil daftar todo saya dengan id
    suspend fun getById(call: ApplicationCall) {
        val todoId = call.parameters["id"]
            ?: throw AppException(400, "Data todo tidak valid!")

        val user = ServiceHelper.getAuthUser(call, userRepo)

        val todo = todoRepo.getById(todoId)
        if (todo == null || todo.userId != user.id) {
            throw AppException(404, "Data todo tidak tersedia!")
        }

        val response = DataResponse(
            status = "success",
            message = "Berhasil mengambil data todo",
            data = TodoResponse(todo = todo)
        )
        call.respond(response)
    }

    // Ubah cover todo
    suspend fun putCover(call: ApplicationCall) {
        val todoId = call.parameters["id"]
            ?: throw AppException(400, "Data todo tidak valid!")

        val user = ServiceHelper.getAuthUser(call, userRepo)

        val request = TodoRequest()
        request.userId = user.id

        val multipartData = call.receiveMultipart(formFieldLimit = 1024 * 1024 * 5)
        multipartData.forEachPart { part ->
            when (part) {
                is PartData.FileItem -> {
                    val ext = part.originalFileName
                        ?.substringAfterLast('.', "")
                        ?.let { if (it.isNotEmpty()) ".$it" else "" }
                        ?: ""

                    val fileName = UUID.randomUUID().toString() + ext
                    val filePath = "uploads/todos/$fileName"

                    withContext(Dispatchers.IO) {
                        val file = File(filePath)
                        file.parentFile.mkdirs()
                        part.provider().copyAndClose(file.writeChannel())
                        request.cover = filePath
                    }
                }

                else -> {}
            }

            part.dispose()
        }

        if (request.cover == null) {
            throw AppException(404, "Cover todo tidak tersedia!")
        }

        val newFile = File(request.cover!!)
        if (!newFile.exists()) {
            throw AppException(404, "Cover todo gagal diunggah!")
        }

        val oldTodo = todoRepo.getById(todoId)
        if (oldTodo == null || oldTodo.userId != user.id) {
            throw AppException(404, "Data todo tidak tersedia!")
        }

        request.title = oldTodo.title
        request.description = oldTodo.description
        request.isDone = oldTodo.isDone
        request.urgency = oldTodo.urgency

        val isUpdated = todoRepo.update(user.id, todoId, request.toEntity())
        if (!isUpdated) {
            throw AppException(400, "Gagal memperbarui cover todo!")
        }

        if (oldTodo.cover != null) {
            val oldFile = File(oldTodo.cover!!)
            if (oldFile.exists()) oldFile.delete()
        }

        val response = DataResponse(
            status = "success",
            message = "Berhasil mengubah cover todo",
            data = null as String?
        )
        call.respond(response)
    }

    // Menambahkan data todo
    suspend fun post(call: ApplicationCall) {
        val user = ServiceHelper.getAuthUser(call, userRepo)

        val request = call.receive<TodoRequest>()
        request.userId = user.id

        // Validasi urgency
        if (request.urgency !in listOf("low", "medium", "high")) {
            request.urgency = "medium"
        }

        val validator = ValidatorHelper(request.toMap())
        validator.required("title", "Judul todo tidak boleh kosong")
        validator.required("description", "Deskripsi tidak boleh kosong")
        validator.validate()

        val todoId = todoRepo.create(request.toEntity())

        val response = DataResponse(
            status = "success",
            message = "Berhasil menambahkan data todo",
            data = TodoAddResponse(todoId = todoId)
        )
        call.respond(response)
    }

    // Mengubah data todo
    suspend fun put(call: ApplicationCall) {
        val todoId = call.parameters["id"]
            ?: throw AppException(400, "Data todo tidak valid!")

        val user = ServiceHelper.getAuthUser(call, userRepo)

        val request = call.receive<TodoRequest>()
        request.userId = user.id

        // Validasi urgency
        if (request.urgency !in listOf("low", "medium", "high")) {
            request.urgency = "medium"
        }

        val validator = ValidatorHelper(request.toMap())
        validator.required("title", "Judul todo tidak boleh kosong")
        validator.required("description", "Deskripsi tidak boleh kosong")
        validator.required("isDone", "Status selesai tidak boleh kosong")
        validator.validate()

        val oldTodo = todoRepo.getById(todoId)
        if (oldTodo == null || oldTodo.userId != user.id) {
            throw AppException(404, "Data todo tidak tersedia!")
        }
        request.cover = oldTodo.cover

        val isUpdated = todoRepo.update(user.id, todoId, request.toEntity())
        if (!isUpdated) {
            throw AppException(400, "Gagal memperbarui data todo!")
        }

        val response = DataResponse(
            status = "success",
            message = "Berhasil mengubah data todo",
            data = null as String?
        )
        call.respond(response)
    }

    // Menghapus data todo
    suspend fun delete(call: ApplicationCall) {
        val todoId = call.parameters["id"]
            ?: throw AppException(400, "Data todo tidak valid!")

        val user = ServiceHelper.getAuthUser(call, userRepo)

        val oldTodo = todoRepo.getById(todoId)
        if (oldTodo == null || oldTodo.userId != user.id) {
            throw AppException(404, "Data todo tidak tersedia!")
        }

        val isDeleted = todoRepo.delete(user.id, todoId)
        if (!isDeleted) {
            throw AppException(400, "Gagal menghapus data todo!")
        }

        if (oldTodo.cover != null) {
            val oldFile = File(oldTodo.cover!!)
            if (oldFile.exists()) oldFile.delete()
        }

        val response = DataResponse(
            status = "success",
            message = "Berhasil menghapus data todo",
            data = null as String?
        )
        call.respond(response)
    }

    // Mengambil gambar todo
    suspend fun getCover(call: ApplicationCall) {
        val todoId = call.parameters["id"]
            ?: throw AppException(400, "Data todo tidak valid!")

        val todo = todoRepo.getById(todoId)
            ?: return call.respond(HttpStatusCode.NotFound)

        if (todo.cover == null) {
            throw AppException(404, "Todo belum memiliki cover")
        }

        val file = File(todo.cover!!)
        if (!file.exists()) {
            throw AppException(404, "Cover todo tidak tersedia")
        }

        call.respondFile(file)
    }
}