package com.example.demo.support;

import com.example.demo.domain.claim.ClaimStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the cross-layer test contracts from {@code qa/shared} so API tests and UI tests
 * are driven by the same data.
 */
public final class SharedCases {

    private static final String SHARED_DIRECTORY = "qa/shared";
    private static final String STATUS_TRANSITIONS_FILE = "status-transitions.json";
    private static final String VALIDATION_CASES_FILE = "validation-cases.json";
    private static final List<String> VALIDATED_FIELDS =
            List.of("claimAmount", "description", "incidentLocation", "incidentDate");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SharedCases() {
    }

    public record Transition(ClaimStatus from, ClaimStatus to) {
        @Override
        public String toString() {
            return from + " -> " + to;
        }
    }

    public record ValidationCase(String field, String name, Object value, boolean valid) {
        @Override
        public String toString() {
            return field + ": " + name;
        }
    }

    public static List<Transition> validTransitions() {
        JsonNode contract = load(STATUS_TRANSITIONS_FILE);
        List<Transition> transitions = new ArrayList<>();
        for (JsonNode source : contract.get("statuses")) {
            for (JsonNode target : contract.get("allowed").get(source.asText())) {
                transitions.add(new Transition(
                        ClaimStatus.valueOf(source.asText()), ClaimStatus.valueOf(target.asText())));
            }
        }
        return transitions;
    }

    /** Every ordered pair of statuses (including self-transitions) that is not allowed. */
    public static List<Transition> invalidTransitions() {
        List<Transition> allowed = validTransitions();
        List<Transition> invalid = new ArrayList<>();
        for (ClaimStatus from : ClaimStatus.values()) {
            for (ClaimStatus to : ClaimStatus.values()) {
                Transition candidate = new Transition(from, to);
                if (!allowed.contains(candidate)) {
                    invalid.add(candidate);
                }
            }
        }
        return invalid;
    }

    public static List<ValidationCase> validationCases() {
        JsonNode contract = load(VALIDATION_CASES_FILE);
        List<ValidationCase> cases = new ArrayList<>();
        for (String field : VALIDATED_FIELDS) {
            for (JsonNode node : contract.get(field)) {
                cases.add(new ValidationCase(
                        field, node.get("name").asText(), valueFor(field, node), node.get("valid").asBoolean()));
            }
        }
        return cases;
    }

    private static Object valueFor(String field, JsonNode node) {
        return switch (field) {
            case "claimAmount" -> node.get("value").asDouble();
            case "incidentDate" -> LocalDate.now().plusDays(node.get("daysFromToday").asLong()).toString();
            default -> " ".repeat(node.path("leadingSpaces").asInt(0)) + "a".repeat(node.get("length").asInt());
        };
    }

    private static JsonNode load(String fileName) {
        try {
            return MAPPER.readTree(Files.readString(locate(fileName)));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read shared test data " + fileName, exception);
        }
    }

    /** Walks up from the working directory so the tests run from the module or the repo root. */
    private static Path locate(String fileName) {
        for (Path directory = Path.of("").toAbsolutePath(); directory != null; directory = directory.getParent()) {
            Path candidate = directory.resolve(SHARED_DIRECTORY).resolve(fileName);
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(SHARED_DIRECTORY + "/" + fileName + " not found above " + Path.of("").toAbsolutePath());
    }
}
