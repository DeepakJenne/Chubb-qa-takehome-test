package com.example.demo.api;

import com.example.demo.support.AbstractApiIntegrationTest;
import com.example.demo.support.SharedCases;
import com.example.demo.support.SharedCases.ValidationCase;
import com.example.demo.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.example.demo.support.TestAuth.asClaimant;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Test cases C-01 to C-07 (claim creation validation and ownership). */
class ClaimCreationValidationIntegrationTest extends AbstractApiIntegrationTest {

    private static final String CLAIMS_URL = "/api/claims";

    static Stream<ValidationCase> boundaryCases() {
        return SharedCases.validationCases().stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("boundaryCases")
    @DisplayName("C-01..C-05: boundary values are accepted or rejected as specified")
    void boundaryValueIsHandledAsSpecified(ValidationCase testCase) throws Exception {
        User claimant = seedClaimant();
        Map<String, Object> request = validRequest();
        request.put(testCase.field(), testCase.value());

        ResultMatcher expected = testCase.valid() ? status().isCreated() : status().isBadRequest();

        mockMvc.perform(post(CLAIMS_URL)
                        .with(asClaimant(idOf(claimant)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(expected);
    }

    @ParameterizedTest(name = "missing {0}")
    @ValueSource(strings = {
            "incidentDate", "incidentLocation", "description", "claimAmount"})
    @DisplayName("C-06: a missing required field is rejected with 400")
    void missingRequiredFieldIsRejected(String field) throws Exception {
        User claimant = seedClaimant();
        Map<String, Object> request = validRequest();
        request.remove(field);

        mockMvc.perform(post(CLAIMS_URL)
                        .with(asClaimant(idOf(claimant)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("C-07: a new claim is SUBMITTED and owned by the caller, whatever the body says")
    void newClaimIsSubmittedAndOwnedByCaller() throws Exception {
        User claimant = seedClaimant();
        Map<String, Object> request = validRequest();
        request.put("userId", UUID.randomUUID().toString()); // must be ignored

        mockMvc.perform(post(CLAIMS_URL)
                        .with(asClaimant(idOf(claimant)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.userId").value(idOf(claimant).toString()));
    }

    private Map<String, Object> validRequest() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("incidentDate", LocalDate.now().minusDays(1).toString());
        request.put("incidentLocation", DEFAULT_LOCATION);
        request.put("description", DEFAULT_DESCRIPTION);
        request.put("claimAmount", DEFAULT_AMOUNT.doubleValue());
        return request;
    }
}
