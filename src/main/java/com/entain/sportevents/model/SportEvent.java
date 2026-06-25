package com.entain.sportevents.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain model representing a sport event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportEvent {

    private UUID id;

    /** Human-readable name of the event (e.g. "Champions League Final"). */
    private String name;

    /**
     * Sport type — validated against the configured list in {@code application.yml}.
     * Stored as an uppercase String for flexibility without code changes.
     */
    private String sport;

    /** Current lifecycle status of the event. */
    private EventStatus status;

    /** Scheduled start time of the event. */
    private LocalDateTime startTime;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
