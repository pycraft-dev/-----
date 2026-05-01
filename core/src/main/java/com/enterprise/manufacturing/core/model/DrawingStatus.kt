package com.enterprise.manufacturing.core.model



/**

 * Жизненный цикл версии чертежа на устройстве (до синхронизации с сервером).

 */

enum class DrawingStatus {

    DRAFT,

    ON_APPROVAL,

    APPROVED,

}

