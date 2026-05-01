package com.enterprise.manufacturing.timesheet.data

data class TimesheetHistoryRow(
    val id: Long,
    val taskTitle: String,
    val note: String?,
    val categoryName: String?,
    val startEpochMs: Long,
    val endEpochMs: Long,
)
