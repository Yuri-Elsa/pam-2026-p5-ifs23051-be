package org.delcom.helpers

import org.mindrot.jbcrypt.BCrypt

fun parseMessageToMap(rawMessage: String): Map<String, List<String>> {
    return rawMessage.split("|").mapNotNull { part ->
        val split = part.split(":", limit = 2)
        if (split.size == 2) {
            val key = split[0].trim()
            val value = split[1].trim()
            key to listOf(value)
        } else null
    }.toMap()
}

fun hashPassword(password: String): String {
    return BCrypt.hashpw(password, BCrypt.gensalt(12))
}

fun verifyPassword(password: String, hashed: String): Boolean {
    return BCrypt.checkpw(password, hashed)
}