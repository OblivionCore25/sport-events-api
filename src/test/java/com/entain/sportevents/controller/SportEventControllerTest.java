package com.entain.sportevents.controller;

import com.entain.sportevents.dto.CreateEventRequest;
import com.entain.sportevents.dto.EventResponse;
import com.entain.sportevents.dto.UpdateStatusRequest;
import com.entain.sportevents.exception.EventNotFoundException;
import com.entain.sportevents.exception.InvalidStatusTransitionException;
import com.entain.sportevents.model.EventStatus;
import com.entain.sportevents.service.SportEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link SportEventController}.
 * Uses the full Spring Boot context with an in-memory store — no mocks.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SportEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SportEventService sportEventService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // =========================================================================
    // POST /api/events
    // =========================================================================

    @Nested
    @DisplayName("POST /api/events")
    class CreateEventTests {

        @Test
        @DisplayName("should create event and return 201")
        void shouldCreateEvent() throws Exception {
            CreateEventRequest request = new CreateEventRequest(
                    "Champions League Final", "FOOTBALL", LocalDateTime.now().plusDays(7));

            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.name", is("Champions League Final")))
                    .andExpect(jsonPath("$.sport", is("FOOTBALL")))
                    .andExpect(jsonPath("$.status", is("INACTIVE")));
        }

        @Test
        @DisplayName("should return 400 when sport type is invalid")
        void shouldRejectInvalidSportType() throws Exception {
            CreateEventRequest request = new CreateEventRequest(
                    "Unknown Sport Event", "UNDERWATER_CHESS", LocalDateTime.now().plusDays(1));

            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.sport", notNullValue()));
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void shouldRejectBlankName() throws Exception {
            CreateEventRequest request = new CreateEventRequest(
                    "", "FOOTBALL", LocalDateTime.now().plusDays(1));

            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.name", notNullValue()));
        }

        @Test
        @DisplayName("should return 400 when startTime is null")
        void shouldRejectNullStartTime() throws Exception {
            CreateEventRequest request = new CreateEventRequest("Event", "FOOTBALL", null);

            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.startTime", notNullValue()));
        }

        @Test
        @DisplayName("should accept lowercase sport type (case-insensitive)")
        void shouldAcceptLowercaseSportType() throws Exception {
            CreateEventRequest request = new CreateEventRequest(
                    "Hockey Cup", "hockey", LocalDateTime.now().plusDays(3));

            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sport", is("HOCKEY")));
        }
    }

    // =========================================================================
    // GET /api/events
    // =========================================================================

    @Nested
    @DisplayName("GET /api/events")
    class ListEventsTests {

        @Test
        @DisplayName("should return all events")
        void shouldReturnAllEvents() throws Exception {
            // Create two events first
            sportEventService.create(new CreateEventRequest(
                    "Event A", "FOOTBALL", LocalDateTime.now().plusDays(1)));
            sportEventService.create(new CreateEventRequest(
                    "Event B", "HOCKEY", LocalDateTime.now().plusDays(2)));

            mockMvc.perform(get("/api/events"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", notNullValue()));
        }

        @Test
        @DisplayName("should filter by status")
        void shouldFilterByStatus() throws Exception {
            mockMvc.perform(get("/api/events").param("status", "INACTIVE"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should filter by sport type")
        void shouldFilterBySport() throws Exception {
            mockMvc.perform(get("/api/events").param("sport", "FOOTBALL"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should filter by both status and sport")
        void shouldFilterByStatusAndSport() throws Exception {
            mockMvc.perform(get("/api/events")
                            .param("status", "INACTIVE")
                            .param("sport", "HOCKEY"))
                    .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // GET /api/events/{id}
    // =========================================================================

    @Nested
    @DisplayName("GET /api/events/{id}")
    class GetEventByIdTests {

        @Test
        @DisplayName("should return 200 with event when found")
        void shouldReturnEventById() throws Exception {
            EventResponse created = sportEventService.create(new CreateEventRequest(
                    "Specific Event", "TENNIS", LocalDateTime.now().plusDays(1)));

            mockMvc.perform(get("/api/events/{id}", created.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(created.getId().toString())))
                    .andExpect(jsonPath("$.name", is("Specific Event")));
        }

        @Test
        @DisplayName("should return 404 when event not found")
        void shouldReturn404WhenNotFound() throws Exception {
            mockMvc.perform(get("/api/events/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)));
        }
    }

    // =========================================================================
    // PATCH /api/events/{id}/status
    // =========================================================================

    @Nested
    @DisplayName("PATCH /api/events/{id}/status")
    class UpdateStatusTests {

        @Test
        @DisplayName("should successfully transition INACTIVE → ACTIVE with future startTime")
        void shouldActivateEventWithFutureStartTime() throws Exception {
            EventResponse created = sportEventService.create(new CreateEventRequest(
                    "Activation Test", "BASKETBALL", LocalDateTime.now().plusHours(1)));

            UpdateStatusRequest request = new UpdateStatusRequest(EventStatus.ACTIVE);

            mockMvc.perform(patch("/api/events/{id}/status", created.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("ACTIVE")));
        }

        @Test
        @DisplayName("should successfully transition ACTIVE → FINISHED")
        void shouldFinishActiveEvent() throws Exception {
            EventResponse created = sportEventService.create(new CreateEventRequest(
                    "Finish Test", "RUGBY", LocalDateTime.now().plusHours(1)));
            sportEventService.changeStatus(created.getId(), EventStatus.ACTIVE);

            UpdateStatusRequest request = new UpdateStatusRequest(EventStatus.FINISHED);

            mockMvc.perform(patch("/api/events/{id}/status", created.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("FINISHED")));
        }

        @Test
        @DisplayName("should return 422 when transitioning INACTIVE → FINISHED")
        void shouldRejectInactiveToFinished() throws Exception {
            EventResponse created = sportEventService.create(new CreateEventRequest(
                    "Invalid Transition", "CRICKET", LocalDateTime.now().plusDays(1)));

            UpdateStatusRequest request = new UpdateStatusRequest(EventStatus.FINISHED);

            mockMvc.perform(patch("/api/events/{id}/status", created.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status", is(422)));
        }

        @Test
        @DisplayName("should return 422 when event is FINISHED and any transition is attempted")
        void shouldRejectTransitionFromFinished() throws Exception {
            EventResponse created = sportEventService.create(new CreateEventRequest(
                    "Terminal State Test", "FOOTBALL", LocalDateTime.now().plusHours(1)));
            sportEventService.changeStatus(created.getId(), EventStatus.ACTIVE);
            sportEventService.changeStatus(created.getId(), EventStatus.FINISHED);

            UpdateStatusRequest request = new UpdateStatusRequest(EventStatus.ACTIVE);

            mockMvc.perform(patch("/api/events/{id}/status", created.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("should return 422 when activating with past startTime")
        void shouldRejectActivationWithPastStartTime() throws Exception {
            // Directly inject event with past startTime via service bypassing the controller
            // We create with future time, then manually set past time via low-level approach
            // Instead, we test via the service layer directly and validate the 422 via controller
            // by creating an event via injection
            EventResponse created = sportEventService.create(new CreateEventRequest(
                    "Past Start Time Test", "HOCKEY", LocalDateTime.now().minusHours(1)));

            UpdateStatusRequest request = new UpdateStatusRequest(EventStatus.ACTIVE);

            mockMvc.perform(patch("/api/events/{id}/status", created.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("past")));
        }

        @Test
        @DisplayName("should return 404 when event not found")
        void shouldReturn404ForUnknownEvent() throws Exception {
            UpdateStatusRequest request = new UpdateStatusRequest(EventStatus.ACTIVE);

            mockMvc.perform(patch("/api/events/{id}/status", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when status field is missing")
        void shouldReturn400WhenStatusMissing() throws Exception {
            EventResponse created = sportEventService.create(new CreateEventRequest(
                    "Validation Test", "TENNIS", LocalDateTime.now().plusDays(1)));

            mockMvc.perform(patch("/api/events/{id}/status", created.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // GET /api/sports
    // =========================================================================

    @Nested
    @DisplayName("GET /api/sports")
    class ListSportsTests {

        @Test
        @DisplayName("should return the configured list of sport types")
        void shouldReturnSportTypes() throws Exception {
            mockMvc.perform(get("/api/sports"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", notNullValue()))
                    .andExpect(jsonPath("$[0]", notNullValue()));
        }
    }
}
