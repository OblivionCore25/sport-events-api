package com.entain.sportevents.model;

/**
 * Represents the lifecycle status of a sport event.
 * <p>
 * Allowed transitions:
 * <ul>
 *   <li>INACTIVE → ACTIVE (only when start_time is not in the past)</li>
 *   <li>ACTIVE → FINISHED</li>
 * </ul>
 * All other transitions are forbidden.
 */
public enum EventStatus {
    INACTIVE,
    ACTIVE,
    FINISHED
}
