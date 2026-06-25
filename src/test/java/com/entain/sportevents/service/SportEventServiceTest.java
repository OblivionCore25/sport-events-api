package com.entain.sportevents.service;

import com.entain.sportevents.dto.CreateEventRequest;
import com.entain.sportevents.dto.EventResponse;
import com.entain.sportevents.exception.EventNotFoundException;
import com.entain.sportevents.exception.InvalidStatusTransitionException;
import com.entain.sportevents.model.EventStatus;
import com.entain.sportevents.model.SportEvent;
import com.entain.sportevents.repository.InMemoryEventRepository;
import com.entain.sportevents.sse.SseEmitterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SportEventService}.
 * All dependencies are mocked — no Spring context is loaded.
 */
@ExtendWith(MockitoExtension.class)
class SportEventServiceTest {

    @Mock
    private InMemoryEventRepository repository;

    @Mock
    private SseEmitterRegistry sseEmitterRegistry;

    @InjectMocks
    private SportEventService service;

    private UUID eventId;
    private SportEvent inactiveEvent;
    private SportEvent activeEvent;
    private SportEvent finishedEvent;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();

        inactiveEvent = SportEvent.builder()
                .id(eventId)
                .name("Test Match")
                .sport("FOOTBALL")
                .status(EventStatus.INACTIVE)
                .startTime(LocalDateTime.now().plusHours(2))   // future
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        activeEvent = SportEvent.builder()
                .id(eventId)
                .name("Test Match")
                .sport("FOOTBALL")
                .status(EventStatus.ACTIVE)
                .startTime(LocalDateTime.now().plusHours(2))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        finishedEvent = SportEvent.builder()
                .id(eventId)
                .name("Test Match")
                .sport("FOOTBALL")
                .status(EventStatus.FINISHED)
                .startTime(LocalDateTime.now().minusHours(1))  // past
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    // Create
    // =========================================================================

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("should create event with INACTIVE status")
        void shouldCreateEventAsInactive() {
            CreateEventRequest request = new CreateEventRequest(
                    "Premier League Final", "FOOTBALL", LocalDateTime.now().plusDays(1));

            when(repository.save(any(SportEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            EventResponse response = service.create(request);

            assertThat(response.getStatus()).isEqualTo(EventStatus.INACTIVE);
            assertThat(response.getName()).isEqualTo("Premier League Final");
            assertThat(response.getSport()).isEqualTo("FOOTBALL");
            assertThat(response.getId()).isNotNull();
        }

        @Test
        @DisplayName("should normalise sport type to uppercase")
        void shouldNormaliseSportTypeToUppercase() {
            CreateEventRequest request = new CreateEventRequest(
                    "Hockey Cup", "hockey", LocalDateTime.now().plusDays(1));

            when(repository.save(any(SportEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            EventResponse response = service.create(request);

            assertThat(response.getSport()).isEqualTo("HOCKEY");
        }
    }

    // =========================================================================
    // Read
    // =========================================================================

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("should return event when found")
        void shouldReturnEventWhenFound() {
            when(repository.findById(eventId)).thenReturn(Optional.of(inactiveEvent));

            EventResponse response = service.findById(eventId);

            assertThat(response.getId()).isEqualTo(eventId);
        }

        @Test
        @DisplayName("should throw EventNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(repository.findById(eventId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(eventId))
                    .isInstanceOf(EventNotFoundException.class)
                    .hasMessageContaining(eventId.toString());
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("should delegate filter parameters to repository")
        void shouldDelegateFiltersToRepository() {
            when(repository.findByFilters(EventStatus.ACTIVE, "FOOTBALL"))
                    .thenReturn(List.of(activeEvent));

            List<EventResponse> results = service.findAll(EventStatus.ACTIVE, "FOOTBALL");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getStatus()).isEqualTo(EventStatus.ACTIVE);
        }
    }

    // =========================================================================
    // Status transitions
    // =========================================================================

    @Nested
    @DisplayName("changeStatus() — allowed transitions")
    class AllowedTransitionTests {

        @Test
        @DisplayName("INACTIVE → ACTIVE: allowed when startTime is in the future")
        void inactiveToActiveWithFutureStartTime() {
            when(repository.findById(eventId)).thenReturn(Optional.of(inactiveEvent));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            EventResponse response = service.changeStatus(eventId, EventStatus.ACTIVE);

            assertThat(response.getStatus()).isEqualTo(EventStatus.ACTIVE);
            verify(sseEmitterRegistry).broadcast(any());
        }

        @Test
        @DisplayName("ACTIVE → FINISHED: always allowed")
        void activeToFinished() {
            when(repository.findById(eventId)).thenReturn(Optional.of(activeEvent));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            EventResponse response = service.changeStatus(eventId, EventStatus.FINISHED);

            assertThat(response.getStatus()).isEqualTo(EventStatus.FINISHED);
            verify(sseEmitterRegistry).broadcast(any());
        }
    }

    @Nested
    @DisplayName("changeStatus() — forbidden transitions")
    class ForbiddenTransitionTests {

        @Test
        @DisplayName("INACTIVE → ACTIVE: forbidden when startTime is in the past")
        void inactiveToActiveWithPastStartTime() {
            SportEvent pastEvent = SportEvent.builder()
                    .id(eventId)
                    .name("Past Event")
                    .sport("FOOTBALL")
                    .status(EventStatus.INACTIVE)
                    .startTime(LocalDateTime.now().minusHours(1))  // past!
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(repository.findById(eventId)).thenReturn(Optional.of(pastEvent));

            assertThatThrownBy(() -> service.changeStatus(eventId, EventStatus.ACTIVE))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("start time");

            verify(sseEmitterRegistry, never()).broadcast(any());
        }

        @Test
        @DisplayName("INACTIVE → FINISHED: forbidden")
        void inactiveToFinished() {
            when(repository.findById(eventId)).thenReturn(Optional.of(inactiveEvent));

            assertThatThrownBy(() -> service.changeStatus(eventId, EventStatus.FINISHED))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("INACTIVE");

            verify(sseEmitterRegistry, never()).broadcast(any());
        }

        @Test
        @DisplayName("FINISHED → ACTIVE: forbidden (terminal state)")
        void finishedToActive() {
            when(repository.findById(eventId)).thenReturn(Optional.of(finishedEvent));

            assertThatThrownBy(() -> service.changeStatus(eventId, EventStatus.ACTIVE))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("Finished");

            verify(sseEmitterRegistry, never()).broadcast(any());
        }

        @Test
        @DisplayName("FINISHED → INACTIVE: forbidden (terminal state)")
        void finishedToInactive() {
            when(repository.findById(eventId)).thenReturn(Optional.of(finishedEvent));

            assertThatThrownBy(() -> service.changeStatus(eventId, EventStatus.INACTIVE))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("Finished");

            verify(sseEmitterRegistry, never()).broadcast(any());
        }

        @Test
        @DisplayName("ACTIVE → INACTIVE: forbidden (no rollback)")
        void activeToInactive() {
            when(repository.findById(eventId)).thenReturn(Optional.of(activeEvent));

            assertThatThrownBy(() -> service.changeStatus(eventId, EventStatus.INACTIVE))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("revert");

            verify(sseEmitterRegistry, never()).broadcast(any());
        }

        @Test
        @DisplayName("Same status → same status: rejected as a no-op")
        void sameStatusTransition() {
            when(repository.findById(eventId)).thenReturn(Optional.of(inactiveEvent));

            assertThatThrownBy(() -> service.changeStatus(eventId, EventStatus.INACTIVE))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("already");

            verify(sseEmitterRegistry, never()).broadcast(any());
        }

        @Test
        @DisplayName("changeStatus: throws EventNotFoundException when event does not exist")
        void throwsWhenEventNotFound() {
            when(repository.findById(eventId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.changeStatus(eventId, EventStatus.ACTIVE))
                    .isInstanceOf(EventNotFoundException.class);
        }
    }
}
