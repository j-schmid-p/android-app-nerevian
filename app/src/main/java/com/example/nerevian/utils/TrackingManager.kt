package com.example.nerevian.utils

/**
 * Manages the relationship between Tracking Steps and Offer Statuses.
 * Since the API requires separate calls, this manager decides if a status update is needed.
 */
object TrackingManager {

    // Status Constants based on provided IDs
    const val STATUS_PENDING = 1
    const val STATUS_ACCEPTED = 2
    const val STATUS_REJECTED = 3
    const val STATUS_SHIPPED = 4
    const val STATUS_DELAYED = 5
    const val STATUS_FINALIZED = 6
    const val STATUS_IN_TRANSIT = 7
    const val STATUS_OUT_FOR_DELIVERY = 8

    /**
     * Returns the appropriate Offer Status ID for a given Tracking Step ID.
     * Returns -1 if no status change is needed or if the step doesn't trigger a change.
     */
    fun getStatusForStepId(stepId: Int): Int {
        return when (stepId) {
            1, 2 -> STATUS_SHIPPED
            3, 4, 5, 6, 7 -> STATUS_IN_TRANSIT
            8 -> STATUS_OUT_FOR_DELIVERY
            9 -> STATUS_FINALIZED
            else -> -1
        }
    }
}
