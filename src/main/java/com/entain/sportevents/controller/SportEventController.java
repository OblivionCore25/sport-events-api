package com.entain.sportevents.controller;

import com.entain.sportevents.config.SportTypesConfig;
import com.entain.sportevents.dto.CreateEventRequest;
import com.entain.sportevents.dto.EventResponse;
import com.entain.sportevents.dto.UpdateStatusRequest;
import com.entain.sportevents.model.EventStatus;
import com.entain.sportevents.service.SportEventService;
import com.entain.sportevents.sse.SseEmitterRegistry;
import com.entain.sportevents.validation.ValidSportType;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the Sport Events API.
 *
 * <p>Base path: {@code /api/events}
 * <ul>
 *   <li>{@code POST   /api/events}             – Create a sport event</li>
 *   <li>{@code GET    /api/events}             – List events (optional filters)</li>
 *   <li>{@code GET    /api/events/{id}}        – Get event by ID</li>
 *   <li>{@code PATCH  /api/events/{id}/status} – Change event status</li>
 *   <li>{@code GET    /api/events/stream}      – SSE stream for live updates</li>
 *   <li>{@code GET    /api/sports}             – List configured sport types</li>
 * </ul>
 */
@Validated
@RestController
@RequestMapping("/api")
public class SportEventController {

    private final SportEventService sportEventService;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final SportTypesConfig sportTypesConfig;

    @Autowired
    public SportEventController(SportEventService sportEventService,
                                SseEmitterRegistry sseEmitterRegistry,
                                SportTypesConfig sportTypesConfig) {
        this.sportEventService = sportEventService;
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.sportTypesConfig = sportTypesConfig;
    }

    // -------------------------------------------------------------------------
    // 1. Create a sport event
    // -------------------------------------------------------------------------

    /**
     * Creates a new sport event. The event starts with INACTIVE status.
     *
     * @param request the creation payload
     * @return 201 Created with the newly created event
     */
    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse createEvent(@Valid @RequestBody CreateEventRequest request) {
        return sportEventService.create(request);
    }

    // -------------------------------------------------------------------------
    // 2. Get list of sport events with optional filters
    // -------------------------------------------------------------------------

    /**
     * Returns all sport events, optionally filtered by status and/or sport type.
     *
     * @param status optional status filter (INACTIVE, ACTIVE, FINISHED)
     * @param sport  optional sport type filter (case-insensitive, e.g. "football")
     * @return 200 OK with the list of matching events
     */
    @GetMapping("/events")
    public List<EventResponse> listEvents(
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) @ValidSportType(required = false) String sport) {
        return sportEventService.findAll(status, sport);
    }

    // -------------------------------------------------------------------------
    // 3. Get a sport event by ID
    // -------------------------------------------------------------------------

    /**
     * Returns a single sport event by its UUID.
     *
     * @param id the event UUID
     * @return 200 OK with the event, or 404 if not found
     */
    @GetMapping("/events/{id}")
    public EventResponse getEventById(@PathVariable UUID id) {
        return sportEventService.findById(id);
    }

    // -------------------------------------------------------------------------
    // 4. Change sport event status
    // -------------------------------------------------------------------------

    /**
     * Changes the status of a sport event. All lifecycle transition rules are enforced.
     *
     * @param id      the event UUID
     * @param request the desired target status
     * @return 200 OK with the updated event, or 404/422 on error
     */
    @PatchMapping("/events/{id}/status")
    public EventResponse updateStatus(@PathVariable UUID id,
                                      @Valid @RequestBody UpdateStatusRequest request) {
        return sportEventService.changeStatus(id, request.getStatus());
    }

    // -------------------------------------------------------------------------
    // 5. SSE stream for real-time event updates
    // -------------------------------------------------------------------------

    /**
     * Opens a Server-Sent Events (SSE) stream. The client will receive a push notification
     * as an {@code event-update} event whenever any sport event's status changes.
     *
     * <p>Connect with: {@code curl -N http://localhost:8080/api/events/stream}
     *
     * @return an {@link SseEmitter} that stays open until the client disconnects
     */
    @GetMapping(value = "/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        return sseEmitterRegistry.register();
    }

    // -------------------------------------------------------------------------
    // Bonus: List configured sport types
    // -------------------------------------------------------------------------

    /**
     * Returns the list of valid sport types as configured in {@code application.yml}.
     * Useful for clients to discover available values without guessing.
     *
     * @return 200 OK with the list of sport type strings
     */
    @GetMapping("/sports")
    public List<String> listSportTypes() {
        return sportTypesConfig.getTypes();
    }
}
