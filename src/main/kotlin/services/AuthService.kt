package org.delcom.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.delcom.data.AppException
import org.delcom.data.AuthRequest
import org.delcom.data.DataResponse
import org.delcom.data.RefreshTokenRequest
import org.delcom.entities.RefreshToken
import org.delcom.helpers.JWTConstants
import org.delcom.helpers.ValidatorHelper
import org.delcom.helpers.hashPassword
import org.delcom.helpers.verifyPassword
import org.delcom.repositories.IRefreshTokenRepository
import org.delcom.repositories.IUserRepository
import java.util.*

class AuthService(
    private val jwtSecret: String,
    private val userRepository: IUserRepository,
    private val refreshTokenRepository: IRefreshTokenRepository,
) {
    private fun generateAuthToken(userId: String): String {
        if (jwtSecret.isBlank()) {
            throw AppException(500, "JWT secret tidak dikonfigurasi!")
        }
        return JWT.create()
            .withAudience(JWTConstants.AUDIENCE)
            .withIssuer(JWTConstants.ISSUER)
            .withClaim("userId", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + 60 * 60 * 1000)) // 1 Jam
            .sign(Algorithm.HMAC256(jwtSecret))
    }

    // register
    suspend fun postRegister(call: ApplicationCall) {
        val request = call.receive<AuthRequest>()

        val validator = ValidatorHelper(request.toMap())
        validator.required("name", "Nama tidak boleh kosong")
        validator.required("username", "Username tidak boleh kosong")
        validator.required("password", "Password tidak boleh kosong")
        validator.validate()

        val existUser = userRepository.getByUsername(request.username)
        if (existUser != null) {
            throw AppException(
                409,
                "Akun dengan username ini sudah terdaftar!"
            )
        }

        request.password = hashPassword(request.password)
        val userId = userRepository.create(request.toEntity())

        val response = DataResponse(
            "success",
            "Berhasil melakukan pendaftaran",
            mapOf(Pair("userId", userId))
        )
        call.respond(response)
    }

    // Login
    suspend fun postLogin(call: ApplicationCall) {
        val request = call.receive<AuthRequest>()

        val validator = ValidatorHelper(request.toMap())
        validator.required("username", "Username tidak boleh kosong")
        validator.required("password", "Password tidak boleh kosong")
        validator.validate()

        val existUser = userRepository.getByUsername(request.username)
            ?: throw AppException(404, "Kredensial yang digunakan tidak valid!")

        val validPassword = try {
            verifyPassword(request.password, existUser.password)
        } catch (e: Exception) {
            throw AppException(500, "Gagal memverifikasi kata sandi: ${e.message}")
        }

        if (!validPassword) {
            throw AppException(404, "Kredensial yang digunakan tidak valid!")
        }

        val authToken = try {
            generateAuthToken(existUser.id)
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw AppException(500, "Gagal membuat token: ${e.message}")
        }

        // Hapus semua token lama
        refreshTokenRepository.deleteByUserId(existUser.id)

        val strRefreshToken = UUID.randomUUID().toString()
        refreshTokenRepository.create(
            RefreshToken(
                userId = existUser.id,
                authToken = authToken,
                refreshToken = strRefreshToken
            )
        )

        val response = DataResponse(
            "success",
            "Berhasil melakukan login",
            mapOf(
                Pair("authToken", authToken),
                Pair("refreshToken", strRefreshToken)
            )
        )
        call.respond(response)
    }

    // Refresh Token
    suspend fun postRefreshToken(call: ApplicationCall) {
        val request = call.receive<RefreshTokenRequest>()

        val validator = ValidatorHelper(request.toMap())
        validator.required("refreshToken", "Refresh Token tidak boleh kosong")
        validator.required("authToken", "Auth Token tidak boleh kosong")
        validator.validate()

        val existRefreshToken = refreshTokenRepository.getByToken(
            refreshToken = request.refreshToken,
            authToken = request.authToken
        )

        // Hapus token lama
        refreshTokenRepository.delete(request.authToken)

        if (existRefreshToken == null) {
            throw AppException(401, "Token tidak valid!")
        }

        val userId = existRefreshToken.userId
        val user = userRepository.getById(userId)
            ?: throw AppException(404, "User tidak valid!")

        val authToken = try {
            generateAuthToken(userId)
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw AppException(500, "Gagal membuat token: ${e.message}")
        }

        val strRefreshToken = UUID.randomUUID().toString()
        refreshTokenRepository.create(
            RefreshToken(
                userId = user.id,
                authToken = authToken,
                refreshToken = strRefreshToken
            )
        )

        val response = DataResponse(
            "success",
            "Berhasil melakukan refresh token",
            mapOf(
                Pair("authToken", authToken),
                Pair("refreshToken", strRefreshToken)
            )
        )
        call.respond(response)
    }

    // Logout
    suspend fun postLogout(call: ApplicationCall) {
        val request = call.receive<RefreshTokenRequest>()

        val validator = ValidatorHelper(request.toMap())
        validator.required("authToken", "Auth Token tidak boleh kosong")
        validator.validate()

        try {
            val decodedJWT = JWT.require(Algorithm.HMAC256(jwtSecret))
                .build()
                .verify(request.authToken)

            val userId = decodedJWT
                .getClaim("userId")
                .asString() ?: throw AppException(401, "Token tidak valid")

            refreshTokenRepository.deleteByUserId(userId)
        } catch (e: AppException) {
            throw e
        } catch (e: JWTVerificationException) {
            // Token expired atau invalid — tetap hapus dari DB jika ada
        } catch (e: Exception) {
            // Abaikan error verifikasi lainnya
        }

        // Selalu hapus authToken dari DB
        refreshTokenRepository.delete(request.authToken)

        val response = DataResponse(
            "success",
            "Berhasil logout",
            null,
        )
        call.respond(response)
    }
}