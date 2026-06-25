package com.entain.sportevents.dto;

import com.entain.sportevents.model.EventStatus;
import com.entain.sportevents.model.SportEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbound DTO returned to API consumers for any sport event response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    private UUID id;
    private String name;
    private String sport;
    private EventStatus status;
    private LocalDateTime startTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Maps a {@link SportEvent} domain model to an {@link EventResponse} DTO.
     *
     * @param event the domain model
     * @return the outbound response representation
     */
    public static EventResponse from(SportEvent event) {
        return EventResponse.builder()
                .id(event.getId())
                .name(event.getName())
                .sport(event.getSport().toUpperCase())
                .status(event.getStatus())
                .startTime(event.getStartTime())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
