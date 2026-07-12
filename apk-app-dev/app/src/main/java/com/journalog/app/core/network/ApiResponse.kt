package com.journalog.app.core.network

data class ApiResponse<T>(
    val ok: Boolean,
    val message: String?,
    val data: T?
)
