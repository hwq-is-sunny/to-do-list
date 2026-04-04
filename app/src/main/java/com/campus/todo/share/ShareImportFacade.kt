package com.campus.todo.share

/**
 * 第二轮：在此接入 [Intent.ACTION_SEND] 的真实解析与路由。
 * 当前仅占位，供 [ShareImportActivity] 调用。
 */
fun interface ShareImportFacade {
    suspend fun ingestPlainTextForInbox(text: String): Long
}
