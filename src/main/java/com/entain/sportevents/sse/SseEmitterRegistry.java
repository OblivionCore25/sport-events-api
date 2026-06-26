package com.entain.sportevents.sse;

import com.entain.sportevents.dto.EventResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry that manages active SSE connections and broadcasts event updates to
 * all subscribers.
 *
 * <p>
 * Uses a {@link CopyOnWriteArrayList} so that iterating for broadcast and
 * removing
 * completed/timed-out emitters are both thread-safe without external locking.
 */
@Component
public class SseEmitterRegistry {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterRegistry.class);

    // CopyOnWriteArrayList: thread-safe iteration and concurrent removal
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Registers a new SSE client and returns its emitter.
     * The emitter removes itself on completion, timeout or error.
     *
     * @return a fresh {@link SseEmitter} bound to this client
     */
    public SseEmitter register() {
        // Long.MAX_VALUE = no server-side timeout (client disconnects as needed)
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitters.add(emitter);
        log.debug("SSE client connected. Total subscribers: {}", emitters.size());

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.debug("SSE client disconnected (completion). Remaining: {}", emitters.size());
        });
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.debug("SSE client disconnected (timeout). Remaining: {}", emitters.size());
        });
        emitter.onError(e -> {
            emitters.remove(emitter);
            log.debug("SSE client disconnected (error: {}). Remaining: {}", e.getMessage(), emitters.size());
        });

        return emitter;
    }

    /**
     * Broadcasts an event update to all currently connected SSE clients.
     * Dead emitters (IOException) are collected and removed after the iteration.
     *
     * @param event the updated event to broadcast
     */
    public void broadcast(EventResponse event) {
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(
                        SseEmitter.event()
                                .name("event-update")
                                .data(objectMapper.writeValueAsString(event)));
            } catch (IOException e) {
                deadEmitters.add(emitter);
                log.debug("Removing dead SSE emitter: {}", e.getMessage());
            }
        }

        emitters.removeAll(deadEmitters);
        log.debug("Broadcasted update for event {}. Active subscribers: {}", event.getId(), emitters.size());
    }

    /**
     * Returns the number of currently active SSE subscribers (useful for
     * monitoring).
     */
    public int subscriberCount() {
        return emitters.size();
    }
}
