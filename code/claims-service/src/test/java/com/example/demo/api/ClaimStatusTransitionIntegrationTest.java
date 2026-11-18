package com.example.demo.api;

import com.example.demo.domain.claim.Claim;
import com.example.demo.domain.claim.ClaimStatus;
import com.example.demo.support.AbstractApiIntegrationTest;
import com.example.demo.support.ClaimEventsProbe;
import com.example.demo.support.ClaimEventsProbe.ClaimEvent;
import com.example.demo.support.SharedCases;
import com.example.demo.support.SharedCases.Transition;
import com.example.demo.user.domain.User;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.example.demo.support.TestAuth.asAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Test cases A-01 to A-05 (status transitions and their error mapping). */
class ClaimStatusTransitionIntegrationTest extends AbstractApiIntegrationTest {

    private static final String STATUS_URL = "/api/admin/claims/{claimId}/status";
    private static final Duration EVENT_WINDOW = Duration.ofSeconds(3);
    private static final String STATUS_CHANGED_EVENT = "claim-status-changed";

    static Stream<Transition> validTransitions() {
        return SharedCases.validTransitions().stream();
    }

    static Stream<Transition> invalidTransitions() {
        return SharedCases.invalidTransitions().stream();
    }

    @ParameterizedTest(name = "{0} is accepted")
    @MethodSource("validTransitions")
    @DisplayName("A-01: admin can perform every allowed status transition")
    void adminCanPerformAllowedTransition(Transition transition) throws Exception {
        User admin = seedAdmin();
        Claim claim = seedClaim(idOf(seedClaimant()), transition.from());

        mockMvc.perform(patch(STATUS_URL, claim.getClaimId())
                        .with(asAdmin(idOf(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody(transition.to())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(transition.to().name()));

        assertThat(claimRepository.findById(claim.getClaimId()).orElseThrow().getStatus())
                .isEqualTo(transition.to());
    }

    /**
     * HYPOTHESIS: the invalid-transition exception has no handler, so this probably answers 500.
     * OpenAPI documents 400. Keep this assertion strict; if it fails, that is the defect evidence.
     */
    @ParameterizedTest(name = "{0} is rejected with 400")
    @MethodSource("invalidTransitions")
    @DisplayName("A-02: every disallowed status transition is rejected with 400 and changes nothing")
    void disallowedTransitionIsRejected(Transition transition) throws Exception {
        User admin = seedAdmin();
        Claim claim = seedClaim(idOf(seedClaimant()), transition.from());

        int httpStatus = patchAndGetStatus(idOf(admin), claim.getClaimId(), transition.to());

        assertThat(httpStatus)
                .as("HTTP status for %s", transition)
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(claimRepository.findById(claim.getClaimId()).orElseThrow().getStatus())
                .as("stored status after rejected transition")
                .isEqualTo(transition.from());
    }

    @Test
    @DisplayName("A-03: a rejected transition publishes no event")
    void rejectedTransitionPublishesNoEvent() throws Exception {
        User admin = seedAdmin();
        Claim claim = seedClaim(idOf(seedClaimant()), ClaimStatus.CLOSED);
        ClaimEventsProbe probe = startRecordingEvents();

        patchAndGetStatus(idOf(admin), claim.getClaimId(), ClaimStatus.SUBMITTED);

        assertThat(probe.collectFor(claim.getClaimId(), EVENT_WINDOW)).isEmpty();
    }

    @Test
    @DisplayName("A-04: an allowed transition publishes exactly one status-changed event")
    void allowedTransitionPublishesExactlyOneEvent() throws Exception {
        User admin = seedAdmin();
        Claim claim = seedClaim(idOf(seedClaimant()), ClaimStatus.SUBMITTED);
        ClaimEventsProbe probe = startRecordingEvents();

        mockMvc.perform(patch(STATUS_URL, claim.getClaimId())
                        .with(asAdmin(idOf(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody(ClaimStatus.UNDER_REVIEW)))
                .andExpect(status().isOk());

        List<ClaimEvent> events = probe.collectFor(claim.getClaimId(), EVENT_WINDOW);
        assertThat(events).hasSize(1);
        ClaimEvent event = events.get(0);
        assertThat(event.eventType()).isEqualTo(STATUS_CHANGED_EVENT);
        assertThat(event.key()).isEqualTo(claim.getClaimId().toString());
        assertThat(event.payload().path("oldStatus").asText()).isEqualTo(ClaimStatus.SUBMITTED.name());
        assertThat(event.payload().path("newStatus").asText()).isEqualTo(ClaimStatus.UNDER_REVIEW.name());
    }

    @Test
    @DisplayName("A-05: updating an unknown claim returns 404")
    void unknownClaimReturnsNotFound() throws Exception {
        User admin = seedAdmin();

        mockMvc.perform(patch(STATUS_URL, UUID.randomUUID())
                        .with(asAdmin(idOf(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody(ClaimStatus.UNDER_REVIEW)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    @DisplayName("A-06: an unknown status value is a client error, not a server error")
    void unknownStatusValueIsClientError() throws Exception {
        User admin = seedAdmin();
        Claim claim = seedClaim(idOf(seedClaimant()), ClaimStatus.SUBMITTED);

        mockMvc.perform(patch(STATUS_URL, claim.getClaimId())
                        .with(asAdmin(idOf(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"NOT_A_STATUS\"}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * MockMvc rethrows exceptions that no handler maps, where a real server would answer 500.
     * Translating that here lets the assertion read as an HTTP status either way.
     */
    private int patchAndGetStatus(UUID adminId, UUID claimId, ClaimStatus target) throws Exception {
        try {
            return mockMvc.perform(patch(STATUS_URL, claimId)
                            .with(asAdmin(adminId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(statusBody(target)))
                    .andReturn().getResponse().getStatus();
        } catch (ServletException unmappedException) {
            return HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
    }

    private String statusBody(ClaimStatus status) throws Exception {
        return objectMapper.writeValueAsString(Map.of("newStatus", status.name()));
    }
}
