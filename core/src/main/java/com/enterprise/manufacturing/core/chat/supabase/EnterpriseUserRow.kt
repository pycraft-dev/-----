package com.enterprise.manufacturing.core.chat.supabase

import com.enterprise.manufacturing.core.db.entity.UserEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EnterpriseUserRow(
    val login: String,
    @SerialName("password_hash") val passwordHash: String,
    @SerialName("full_name") val fullName: String,
    val position: String = "",
    @SerialName("group_key") val groupKey: String,
    val role: String,
)

fun UserEntity.toEnterpriseUserRow(): EnterpriseUserRow =
    EnterpriseUserRow(
        login = login,
        passwordHash = passwordHash,
        fullName = fullName,
        position = position,
        groupKey = groupKey,
        role = role,
    )

fun EnterpriseUserRow.toUserEntity(localId: Long = 0L): UserEntity =
    UserEntity(
        id = localId,
        login = login,
        passwordHash = passwordHash,
        fullName = fullName,
        position = position,
        groupKey = groupKey,
        role = role,
    )
