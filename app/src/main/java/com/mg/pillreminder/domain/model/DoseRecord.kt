package com.mg.pillreminder.domain.model

import java.time.Instant

data class DoseRecord(
    val id: Long,
    val medicineId: Long,
    val takenAt: Instant,
)
