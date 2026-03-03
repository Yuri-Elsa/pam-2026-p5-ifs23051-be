package org.delcom.data

import kotlinx.serialization.Serializable

@Serializable
open class AppException(
    val code: Int,
    override val message: String
) : Exception(message)