package com.campus.todo.ui.screens.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.todo.data.db.entity.SourceKind
import com.campus.todo.data.repo.TodoRepository
import kotlinx.coroutines.launch

class AddCandidateViewModel(
    private val repo: TodoRepository
) : ViewModel() {

    /** 仅写入原文并解析为候选（旧流程：生成候选按钮） */
    fun submit(raw: String, source: SourceKind, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val ids = repo.ingestRawTextBatch(raw, source)
            onCreated(ids.firstOrNull() ?: -1L)
        }
    }

    /**
     * 本地规则结构化：写入数据库并返回候选 id，直接进入详情页确认。
     */
    fun structureAndOpenDetail(
        raw: String,
        source: SourceKind,
        onCreated: (Long) -> Unit,
        onErr: (String) -> Unit
    ) {
        viewModelScope.launch {
            val t = raw.trim()
            if (t.isBlank()) {
                onErr("请先输入或粘贴通知内容")
                return@launch
            }
            val id = repo.insertStructuredNoticeCandidate(t, source)
            if (id <= 0L) {
                onErr("未能创建候选项")
                return@launch
            }
            onCreated(id)
        }
    }
}



