package com.enterprise.manufacturing.sync.work

object EnterpriseSyncNames {
    /** Ручной / стартовый one-shot sync (unique work). */
    const val IMMEDIATE = "enterprise_sync_immediate"

    /** Фоновый периодический sync (unique periodic work). */
    const val PERIODIC = "enterprise_sync_periodic"
}
