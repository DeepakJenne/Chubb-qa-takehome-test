package com.example.demo.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Records messages published to {@code claim-events} from the moment it is started, so a test can
 * assert what a use case emitted (or that it emitted nothing).
 *
 * <p>Filtering by claim id keeps tests independent of each other on a shared topic.
 */
public final class ClaimEventsProbe implements AutoCloseable {

    public static final String TOPIC = "claim-events";
    private static final Duration POLL_INTERVAL = Duration.ofMillis(200);
    private static final Duration ADMIN_TIMEOUT = Duration.ofSeconds(10);

    private final KafkaConsumer<String, String> consumer;
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<ClaimEvent> received = new ArrayList<>();

    private ClaimEventsProbe(KafkaConsumer<String, String> consumer) {
        this.consumer = consumer;
    }

    /** One message from the topic, parsed from the {eventType, eventId, payload} envelope. */
    public record ClaimEvent(String key, JsonNode envelope) {
        public String eventType() {
            return envelope.path("eventType").asText();
        }

        public JsonNode payload() {
            return envelope.path("payload");
        }

        public String claimId() {
            return payload().path("claimId").asText();
        }
    }

    public static ClaimEventsProbe startAt(String bootstrapServers) {
        ensureTopicExists(bootstrapServers);
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "qa-probe-" + UUID.randomUUID());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
        List<TopicPartition> partitions = consumer.partitionsFor(TOPIC, ADMIN_TIMEOUT).stream()
                .map(info -> new TopicPartition(info.topic(), info.partition()))
                .toList();
        consumer.assign(partitions);
        consumer.seekToEnd(partitions);
        partitions.forEach(consumer::position); // forces the seek to resolve before the test acts
        return new ClaimEventsProbe(consumer);
    }

    /** Returns as soon as {@code expectedCount} events for the claim arrived, or when the timeout ends. */
    public List<ClaimEvent> awaitEvents(UUID claimId, int expectedCount, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (eventsFor(claimId).size() < expectedCount && Instant.now().isBefore(deadline)) {
            drain();
        }
        return eventsFor(claimId);
    }

    /** Listens for the whole window; use it to prove "exactly N" or "none". */
    public List<ClaimEvent> collectFor(UUID claimId, Duration window) {
        Instant deadline = Instant.now().plus(window);
        while (Instant.now().isBefore(deadline)) {
            drain();
        }
        return eventsFor(claimId);
    }

    private List<ClaimEvent> eventsFor(UUID claimId) {
        return received.stream().filter(event -> claimId.toString().equals(event.claimId())).toList();
    }

    private void drain() {
        for (ConsumerRecord<String, String> record : consumer.poll(POLL_INTERVAL)) {
            received.add(new ClaimEvent(record.key(), parse(record.value())));
        }
    }

    private JsonNode parse(String json) {
        try {
            return mapper.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Non-JSON message on " + TOPIC + ": " + json, exception);
        }
    }

    private static void ensureTopicExists(String bootstrapServers) {
        try (AdminClient admin = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {
            admin.createTopics(List.of(new NewTopic(TOPIC, 1, (short) 1)))
                    .all().get(ADMIN_TIMEOUT.toSeconds(), java.util.concurrent.TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            if (!(exception.getCause() instanceof TopicExistsException)) {
                throw new IllegalStateException("Cannot create topic " + TOPIC, exception);
            }
        } catch (InterruptedException | java.util.concurrent.TimeoutException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Cannot create topic " + TOPIC, exception);
        }
    }

    @Override
    public void close() {
        consumer.close();
    }
}
