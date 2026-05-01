package com.enterprise.manufacturing.core.db.entity



import androidx.room.Entity

import androidx.room.Index

import androidx.room.PrimaryKey



@Entity(

    tableName = "drawing_revisions",

    indices = [

        Index(value = ["seriesId"]),

        Index(value = ["seriesId", "version"], unique = true),

    ],

)

data class DrawingRevisionEntity(

    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** Логическая серия чертежа (одна линия версий v1, v2…). */

    val seriesId: String,

    val version: Int,

    /** Человекочитаемое название серии. */

    val seriesTitle: String,

    /** Абсолютный путь к файлу в каталоге приложения. */

    val localFilePath: String,

    /** Расширение без точки, lowercase (pdf, dwg). */

    val extensionLower: String,

    val changeDescription: String,

    /** [com.enterprise.manufacturing.core.model.DrawingStatus] name */

    val status: String,

    val authorUserId: Long,

    val createdAtEpochMs: Long,

)

