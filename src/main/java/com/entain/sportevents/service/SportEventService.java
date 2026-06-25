package com.entain.sportevents.service;

import com.entain.sportevents.dto.CreateEventRequest;
import com.entain.sportevents.dto.EventResponse;
import com.entain.sportevents.exception.EventNotFoundException;
import com.entain.sportevents.exception.InvalidStatusTransitionException;
import com.entain.sportevents.model.EventStatus;
import com.entain.sportevents.model.SportEvent;
import com.entain.sportevents.repository.InMemoryEventRepository;
import com.entain.sportevents.sse.SseEmitterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core business logic for sport event management.
 *
 * <p>Status transition rules enforced here:
 * <ul>
 *   <li>INACTIVE → ACTIVE: allowed only if {@code startTime} is not in the past</li>
 *   <li>ACTIVE → FINISHED: always allowed</li>
 *   <li>INACTIVE → FINISHED: forbidden</li>
 *   <li>FINISHED → any: forbidden (terminal state)</li>
 *   <li>ACTIVE → INACTIVE: forbidden (no rollback)</li>
 *   <li>Same status → same status: rejected (no-op guard)</li>
 * </ul>
 */
@Service
public class SportEventService {

    private final InMemoryEventRepository repository;
    private final SseEmitterRegistry sseEmitterRegistry;

    @Autowired
    public SportEventService(InMemoryEventRepository repository,
                             SseEmitterRegistry sseEmitterRegistry) {
        this.repository = repository;
        this.sseEmitterRegistry = sseEmitterRegistry;
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    /**
     * Creates a new sport event with an INACTIVE status.
     *
     * @param request the creation request payload
     * @return the created event as a response DTO
     */
    public EventResponse create(CreateEventRequest request) {
        LocalDateTime now = LocalDateTime.now();
        SportEvent event = SportEvent.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .sport(request.getSport().toUpperCase())
                .status(EventStatus.INACTIVE)
                .startTime(request.getStartTime())
                .createdAt(now)
                .updatedAt(now)
                .build();

        repository.save(event);
        return EventResponse.from(event);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /**
     * Returns all events, optionally filtered by status and/or sport type.
     *
     * @param status filter by event status, or {@code null} for all statuses
     * @param sport  filter by sport type (case-insensitive), or {@code null} for all sports
     * @return list of matching events as response DTOs
     */
    public List<EventResponse> findAll(EventStatus status, String sport) {
        return repository.findByFilters(status, sport)
                .stream()
                .map(EventResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Returns a single event by its ID.
     *
     * @param id the event UUID
     * @return the event as a response DTO
     * @throws EventNotFoundException if no event with the given ID exists
     */
    public EventResponse findById(UUID id) {
        return repository.findById(id)
                .map(EventResponse::from)
                .orElseThrow(() -> new EventNotFoundException(id));
    }

    // -------------------------------------------------------------------------
    // Update status
    // -------------------------------------------------------------------------

    /**
     * Changes the status of a sport event, enforcing all lifecycle transition rules.
     * Broadcasts the updated event to all SSE subscribers after a successful change.
     *
     * @param id        the event UUID
     * @param newStatus the desired target status
     * @return the updated event as a response DTO
     * @throws EventNotFoundException           if no event with the given ID exists
     * @throws InvalidStatusTransitionException if the requested transition is not allowed
     */
    public EventResponse changeStatus(UUID id, EventStatus newStatus) {
        SportEvent event = repository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        validateTransition(event, newStatus);

        event.setStatus(newStatus);
        event.setUpdatedAt(LocalDateTime.now());
        repository.save(event);

        EventResponse response = EventResponse.from(event);
        // Push update to all connected SSE clients
        sseEmitterRegistry.broadcast(response);
        return response;
    }

    // -------------------------------------------------------------------------
    // Internal transition guard
    // -------------------------------------------------------------------------

    /**
     * Enforces all event lifecycle transition rules.
     *
     * @param event     the current event
     * @param newStatus the requested target status
     * @throws InvalidStatusTransitionException if the transition is not permitted
     */
    void validateTransition(SportEvent event, EventStatus newStatus) {
        EventStatus current = event.getStatus();

        // Guard: no-op
        if (current == newStatus) {
            throw new InvalidStatusTransitionException(
                    "Event is already in status " + current + ". No change applied.");
        }

        // FINISHED is a terminal state — no exits
        if (current == EventStatus.FINISHED) {
            throw new InvalidStatusTransitionException(
                    "Finished events cannot change status. Current status: FINISHED.");
        }

        // INACTIVE → FINISHED is not an allowed path
        if (current == EventStatus.INACTIVE && newStatus == EventStatus.FINISHED) {
            throw new InvalidStatusTransitionException(
                    "Cannot transition directly from INACTIVE to FINISHED. " +
                    "Activate the event first.");
        }

        // ACTIVE cannot revert to INACTIVE
        if (current == EventStatus.ACTIVE && newStatus == EventStatus.INACTIVE) {
            throw new InvalidStatusTransitionException(
                    "Cannot revert an ACTIVE event back to INACTIVE.");
        }

        // INACTIVE → ACTIVE: start_time must not be in the past
        if (current == EventStatus.INACTIVE && newStatus == EventStatus.ACTIVE) {
            if (event.getStartTime().isBefore(LocalDateTime.now())) {
                throw new InvalidStatusTransitionException(
                        "Cannot activate event: start time (" + event.getStartTime() +
                        ") is in the past.");
            }
        }
    }
}
