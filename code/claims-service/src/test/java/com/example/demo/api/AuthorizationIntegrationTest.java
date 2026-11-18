package com.example.demo.api;

import com.example.demo.domain.claim.Claim;
import com.example.demo.domain.claim.ClaimStatus;
import com.example.demo.support.AbstractApiIntegrationTest;
import com.example.demo.user.domain.User;
import com.example.demo.user.domain.UserId;
import com.example.demo.user.domain.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.example.demo.support.TestAuth.asClaimant;
import static com.example.demo.support.TestAuth.withoutRoles;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test cases B-01 to B-08 (authorization). Cases marked HYPOTHESIS come from code review and may
 * fail: a failure is the defect evidence, so keep the assertion strict.
 */
class AuthorizationIntegrationTest extends AbstractApiIntegrationTest {

    private static final int CLIENT_ERROR_MIN = 400;
    private static final int CLIENT_ERROR_MAX = 499;
    private static final String USERS_URL = "/api/users";

    static Stream<Arguments> adminOnlyEndpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/api/admin/claims"),
                Arguments.of(HttpMethod.GET, "/api/admin/dashboard/stats"),
                Arguments.of(HttpMethod.POST, "/api/admin/dashboard/flush-cache"),
                Arguments.of(HttpMethod.GET, USERS_URL));
    }

    @Test
    @DisplayName("B-01: a claimant cannot read another user's claim")
    void claimantCannotReadAnotherUsersClaim() throws Exception {
        User owner = seedClaimant();
        User intruder = seedClaimant();
        Claim claim = seedClaim(idOf(owner), ClaimStatus.SUBMITTED);

        mockMvc.perform(get("/api/claims/{claimId}", claim.getClaimId()).with(asClaimant(idOf(intruder))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("B-02: a claimant's list contains only their own claims")
    void claimantListContainsOnlyOwnClaims() throws Exception {
        User claimant = seedClaimant();
        User other = seedClaimant();
        Claim first = seedClaim(idOf(claimant), ClaimStatus.SUBMITTED);
        Claim second = seedClaim(idOf(claimant), ClaimStatus.APPROVED);
        seedClaim(idOf(other), ClaimStatus.SUBMITTED);

        mockMvc.perform(get("/api/claims").with(asClaimant(idOf(claimant))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].claimId", containsInAnyOrder(
                        first.getClaimId().toString(), second.getClaimId().toString())));
    }

    @ParameterizedTest(name = "claimant gets 403 on {0} {1}")
    @MethodSource("adminOnlyEndpoints")
    @DisplayName("B-03: a claimant is forbidden from admin-only endpoints")
    void claimantIsForbiddenFromAdminEndpoints(HttpMethod method, String path) throws Exception {
        User claimant = seedClaimant();

        mockMvc.perform(request(method, path).with(asClaimant(idOf(claimant))))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "token without roles gets 403 on {0} {1}")
    @MethodSource("adminOnlyEndpoints")
    @DisplayName("B-05: a valid token with no realm roles is forbidden from admin-only endpoints")
    void tokenWithoutRolesIsForbiddenFromAdminEndpoints(HttpMethod method, String path) throws Exception {
        mockMvc.perform(request(method, path).with(withoutRoles(UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("B-04: a request without a token is unauthorised")
    void requestWithoutTokenIsUnauthorised() throws Exception {
        mockMvc.perform(get("/api/claims")).andExpect(status().isUnauthorized());
    }

    /** HYPOTHESIS: POST /api/users has no role check and accepts the role from the body. */
    @Test
    @DisplayName("B-06: a claimant cannot create a user with the ADMIN role")
    void claimantCannotCreateAdminUser() throws Exception {
        User claimant = seedClaimant();
        UUID newUserId = UUID.randomUUID();

        int httpStatus = mockMvc.perform(request(HttpMethod.POST, USERS_URL)
                        .with(asClaimant(idOf(claimant)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserBody(newUserId, uniqueEmail(), UserRole.ADMIN)))
                .andReturn().getResponse().getStatus();

        assertThat(httpStatus).as("HTTP status").isEqualTo(403);
        assertThat(userRepository.findById(UserId.of(newUserId))).as("admin user was persisted").isEmpty();
    }

    /** HYPOTHESIS: an existing user can be re-keyed to a new id by anyone who knows their email. */
    @Test
    @DisplayName("B-07: an existing user cannot be re-keyed by another caller")
    void existingUserCannotBeReKeyed() throws Exception {
        User victim = seedClaimant();
        User attacker = seedClaimant();
        UUID attackerChosenId = UUID.randomUUID();

        int httpStatus = mockMvc.perform(request(HttpMethod.POST, USERS_URL)
                        .with(asClaimant(idOf(attacker)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserBody(attackerChosenId, victim.getEmail().value(), UserRole.CLAIMANT)))
                .andReturn().getResponse().getStatus();

        assertThat(httpStatus).as("HTTP status").isBetween(CLIENT_ERROR_MIN, CLIENT_ERROR_MAX);
        assertThat(userRepository.findById(victim.getUserId()))
                .as("victim still reachable under the original id").isPresent();
    }

    /** HYPOTHESIS: GET /api/users/{id} has no ownership check. */
    @Test
    @DisplayName("B-08: a claimant cannot read another user's profile")
    void claimantCannotReadAnotherUsersProfile() throws Exception {
        User claimant = seedClaimant();
        User other = seedClaimant();

        mockMvc.perform(get(USERS_URL + "/{userId}", idOf(other)).with(asClaimant(idOf(claimant))))
                .andExpect(status().isForbidden());
    }

    private String createUserBody(UUID userId, String email, UserRole role) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "userId", userId.toString(),
                "email", email,
                "name", "QA Created User",
                "role", role.name()));
    }

    private static String uniqueEmail() {
        return "qa-" + UUID.randomUUID() + "@test.local";
    }
}
