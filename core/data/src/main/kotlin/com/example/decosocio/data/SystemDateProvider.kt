package com.example.decosocio.data

import com.example.decosocio.domain.repository.DateProvider
import kotlinx.datetime.LocalDate
import java.time.ZoneId

/** Dates in Portugal's time zone, where memberships and withdrawal windows are counted. */
class SystemDateProvider(private val zone: ZoneId = ZoneId.of("Europe/Lisbon")) : DateProvider {
    override fun today(): LocalDate {
        val now = java.time.LocalDate.now(zone)
        return LocalDate(now.year, now.monthValue, now.dayOfMonth)
    }

    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}
