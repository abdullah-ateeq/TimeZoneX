package com.example.timezonex.domain.calculator

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class DstCalculator {

    /**
     * Determines whether Daylight Saving Time is currently active in the given zone at the given instant.
     * Relies strictly on the IANA ZoneRules database.
     */
    fun isDstActive(instant: Instant, zoneId: ZoneId): Boolean {
        return zoneId.rules.isDaylightSavings(instant)
    }

    fun isDstActive(zonedDateTime: ZonedDateTime): Boolean {
        return isDstActive(zonedDateTime.toInstant(), zonedDateTime.zone)
    }

    /**
     * Returns whether the given zone ever observes Daylight Saving Time transitions.
     */
    fun observesDst(zoneId: ZoneId, referenceInstant: Instant = Instant.now()): Boolean {
        val rules = zoneId.rules
        if (rules.isFixedOffset) return false
        return rules.nextTransition(referenceInstant) != null ||
            rules.previousTransition(referenceInstant) != null ||
            rules.transitionRules.isNotEmpty() ||
            rules.transitions.isNotEmpty()
    }

    fun getDstStatusLabel(isDst: Boolean): String {
        return if (isDst) "DST Active" else "Standard Time"
    }
}
