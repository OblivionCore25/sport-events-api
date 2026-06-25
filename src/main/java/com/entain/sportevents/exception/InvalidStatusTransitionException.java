package com.entain.sportevents.exception;

/**
 * Thrown when a requested status transition violates the event lifecycle rules.
 * Maps to HTTP 422 Unprocessable Entity.
 */
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
