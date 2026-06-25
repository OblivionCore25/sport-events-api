package com.entain.sportevents.exception;

import java.util.UUID;

/**
 * Thrown when a sport event with the given ID does not exist in the repository.
 */
public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(UUID id) {
        super("Sport event not found with id: " + id);
    }
}
