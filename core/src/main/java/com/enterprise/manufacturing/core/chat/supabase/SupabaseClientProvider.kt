package com.enterprise.manufacturing.core.chat.supabase

import com.enterprise.manufacturing.core.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Один клиент Supabase (PostgREST) на процесс для чата и синхронизации пользователей. */
@Singleton
class SupabaseClientProvider @Inject constructor() {

    private val clientLazy =
        kotlin.lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            val url = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
            val key = BuildConfig.SUPABASE_ANON_KEY.trim()
            if (url.isEmpty() || key.isEmpty()) {
                null
            } else {
                createSupabaseClient(
                    supabaseUrl = url,
                    supabaseKey = key,
                ) {
                    defaultSerializer =
                        KotlinXSerializer(
                            Json {
                                ignoreUnknownKeys = true
                                isLenient = true
                                encodeDefaults = true
                            },
                        )
                    install(Postgrest)
                }
            }
        }

    fun clientOrNull(): SupabaseClient? = clientLazy.value
}
