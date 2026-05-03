package com.enterprise.manufacturing.core.chat.supabase

import java.util.Locale

/** Стабильный ключ диалога по двум логинам (как на сервере). */
fun dmConversationKey(loginA: String, loginB: String): String =
    listOf(loginA.lowercase(Locale.US), loginB.lowercase(Locale.US))
        .sorted()
        .joinToString("#")
