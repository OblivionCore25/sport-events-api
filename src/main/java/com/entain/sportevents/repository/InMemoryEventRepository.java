package com.entain.sportevents.repository;

import com.entain.sportevents.model.EventStatus;
import com.entain.sportevents.model.SportEvent;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe in-memory storage for sport events backed by a {@link ConcurrentHashMap}.
 * No external database required — all data lives in the JVM heap.
 */
@Repository
public class InMemoryEventRepository {

    private final ConcurrentHashMap<UUID, SportEvent> store = new ConcurrentHashMap<>();

    /**
     * Persists (inserts or updates) a sport event.
     *
     * @param event the event to save
     * @return the saved event
     */
    public SportEvent save(SportEvent event) {
        store.put(event.getId(), event);
        return event;
    }

    /**
     * Retrieves a sport event by its unique identifier.
     *
     * @param id the event ID
     * @return an {@link Optional} containing the event, or empty if not found
     */
    public Optional<SportEvent> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    /**
     * Returns all stored events, optionally filtered by status and/or sport type.
     * Both filters are applied with AND semantics. Passing {@code null} for either
     * parameter means "no filter on that dimension".
     *
     * @param status the status to filter by, or {@code null} to skip
     * @param sport  the sport type to filter by (case-insensitive), or {@code null} to skip
     * @return matching events
     */
    public List<SportEvent> findByFilters(EventStatus status, String sport) {
        return store.values().stream()
                .filter(e -> status == null || e.getStatus() == status)
                .filter(e -> sport == null || e.getSport().equalsIgnoreCase(sport))
                .collect(Collectors.toList());
    }
}
