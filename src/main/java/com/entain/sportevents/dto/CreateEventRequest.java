package com.entain.sportevents.dto;

import com.entain.sportevents.validation.ValidSportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request body for creating a new sport event.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventRequest {

    @NotBlank(message = "Event name must not be blank")
    private String name;

    @NotBlank(message = "Sport type must not be blank")
    @ValidSportType
    private String sport;

    @NotNull(message = "Start time must not be null")
    private LocalDateTime startTime;
}
