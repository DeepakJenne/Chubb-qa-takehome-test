package com.example.demo.support;

import com.example.demo.TestcontainersConfiguration;
import com.example.demo.domain.claim.Claim;
import com.example.demo.domain.claim.ClaimRepository;
import com.example.demo.domain.claim.ClaimStatus;
import com.example.demo.fixtures.UserTestBuilder;
import com.example.demo.user.domain.User;
import com.example.demo.user.domain.UserRepository;
import com.example.demo.user.domain.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Base for API integration tests: the real application against Testcontainers (Postgres, Kafka,
 * Redis, Keycloak) driven through MockMvc.
 *
 * <p>Events are published directly ({@code app.events.cdc-enabled=false}), so tests can observe
 * them on Kafka without Debezium. Tagged "integration" like the existing integration tests: run
 * with {@code mvn test -Dgroups=integration -Dsurefire.excludedGroups=}.
 */
@Tag("integration")
@SpringBootTest(properties = "app.events.cdc-enabled=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class AbstractApiIntegrationTest {

    protected static final String DEFAULT_LOCATION = "12 Harbour Road, Singapore";
    protected static final String DEFAULT_DESCRIPTION = "Water damage to the basement after a pipe burst";
    protected static final BigDecimal DEFAULT_AMOUNT = new BigDecimal("1500.00");

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected ClaimRepository claimRepository;
    @Autowired
    private KafkaContainer kafkaContainer;

    private ClaimEventsProbe eventsProbe;

    /** Call before the action under test; the probe only sees messages published after this. */
    protected ClaimEventsProbe startRecordingEvents() {
        eventsProbe = ClaimEventsProbe.startAt(kafkaContainer.getBootstrapServers());
        return eventsProbe;
    }

    @AfterEach
    void stopRecordingEvents() {
        if (eventsProbe != null) {
            eventsProbe.close();
            eventsProbe = null;
        }
    }

    protected User seedAdmin() {
        return seedUser(UserRole.ADMIN);
    }

    protected User seedClaimant() {
        return seedUser(UserRole.CLAIMANT);
    }

    protected User seedUser(UserRole role) {
        User user = UserTestBuilder.aUser()
                .withUserId(UUID.randomUUID())
                .withName("QA " + role.name())
                .withEmail("qa-" + UUID.randomUUID() + "@test.local")
                .withRole(role)
                .build();
        return userRepository.save(user);
    }

    /** Persists a claim directly in the given status, bypassing domain rules and events. */
    protected Claim seedClaim(UUID ownerId, ClaimStatus status) {
        Instant now = Instant.now();
        Claim claim = Claim.reconstitute(UUID.randomUUID(), ownerId, LocalDate.now().minusDays(1),
                DEFAULT_LOCATION, DEFAULT_DESCRIPTION, DEFAULT_AMOUNT, status, now, now);
        return claimRepository.save(claim);
    }

    protected static UUID idOf(User user) {
        return user.getUserId().value();
    }
}
