/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.model;

import io.strimzi.api.kafka.model.common.Condition;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.kafka.KafkaSpec;
import io.strimzi.api.kafka.model.kafka.KafkaSpecBuilder;
import io.strimzi.api.kafka.model.kafka.KafkaStatus;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListenerBuilder;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerType;
import io.strimzi.operator.common.model.InvalidResourceException;
import io.strimzi.test.annotations.ParallelSuite;
import io.strimzi.test.annotations.ParallelTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ParallelSuite
public class KRaftUtilsTest {
    @ParallelTest
    public void testValidKafka() {
        KafkaSpec spec = new KafkaSpecBuilder()
                .withNewKafka()
                    .withListeners(new GenericKafkaListenerBuilder()
                            .withName("listener")
                            .withPort(9092)
                            .withTls(true)
                            .withType(KafkaListenerType.INTERNAL)
                            .build())
                    .withNewKafkaAuthorizationOpa()
                        .withUrl("http://opa:8080")
                    .endKafkaAuthorizationOpa()
                .endKafka()
                .build();

        assertDoesNotThrow(() -> KRaftUtils.validateKafkaCrForKRaft(spec));
    }

    @ParallelTest
    public void testInvalidKafka() {
        InvalidResourceException ex = assertThrows(InvalidResourceException.class, () -> KRaftUtils.validateKafkaCrForKRaft(null));
        assertThat(ex.getMessage(), is("Kafka configuration is not valid: [The .spec section of the Kafka custom resource is missing]"));
    }

    @ParallelTest
    public void testKRaftMetadataVersionValidation()    {
        // Valid values
        assertDoesNotThrow(() -> KRaftUtils.validateMetadataVersion("3.6"));
        assertDoesNotThrow(() -> KRaftUtils.validateMetadataVersion("3.6-IV2"));

        // Minimum supported versions
        assertDoesNotThrow(() -> KRaftUtils.validateMetadataVersion("3.3"));
        assertDoesNotThrow(() -> KRaftUtils.validateMetadataVersion("3.3-IV0"));

        // Invalid Values
        InvalidResourceException e = assertThrows(InvalidResourceException.class, () -> KRaftUtils.validateMetadataVersion("3.6-IV9"));
        assertThat(e.getMessage(), containsString("Metadata version 3.6-IV9 is invalid"));

        e = assertThrows(InvalidResourceException.class, () -> KRaftUtils.validateMetadataVersion("3"));
        assertThat(e.getMessage(), containsString("Metadata version 3 is invalid"));

        e = assertThrows(InvalidResourceException.class, () -> KRaftUtils.validateMetadataVersion("3.2"));
        assertThat(e.getMessage(), containsString("The oldest supported metadata version is 3.3-IV0"));

        e = assertThrows(InvalidResourceException.class, () -> KRaftUtils.validateMetadataVersion("3.2-IV0"));
        assertThat(e.getMessage(), containsString("The oldest supported metadata version is 3.3-IV0"));
    }

    @ParallelTest
    public void testKRaftWarningsForZookeeperFields() {
        Kafka kafka = new KafkaBuilder()
                .withNewSpec()
                    .withNewZookeeper()
                        .withReplicas(3)
                        .withNewEphemeralStorage()
                        .endEphemeralStorage()
                    .endZookeeper()
                    .withNewKafka()
                        .withReplicas(3)
                        .withListeners(new GenericKafkaListenerBuilder()
                                .withName("listener")
                                .withPort(9092)
                                .withTls(true)
                                .withType(KafkaListenerType.INTERNAL)
                                .build())
                        .withNewEphemeralStorage()
                        .endEphemeralStorage()
                        .withNewKafkaAuthorizationOpa()
                            .withUrl("http://opa:8080")
                        .endKafkaAuthorizationOpa()
                    .endKafka()
                .endSpec()
                .build();

        KafkaStatus status = new KafkaStatus();
        KRaftUtils.kraftWarnings(kafka, status);

        assertThat(status.getConditions().size(), is(3));

        Condition condition = status.getConditions().stream().filter(c -> "UnusedZooKeeperConfiguration".equals(c.getReason())).findFirst().orElseThrow();
        assertThat(condition.getMessage(), is("The .spec.zookeeper section in the Kafka custom resource is ignored in KRaft mode and should be removed from the custom resource."));
        assertThat(condition.getType(), is("Warning"));
        assertThat(condition.getStatus(), is("True"));

        condition = status.getConditions().stream().filter(c -> "UnusedReplicasConfiguration".equals(c.getReason())).findFirst().orElseThrow();
        assertThat(condition.getMessage(), is("The .spec.kafka.replicas property in the Kafka custom resource is ignored when node pools are used and should be removed from the custom resource."));
        assertThat(condition.getType(), is("Warning"));
        assertThat(condition.getStatus(), is("True"));

        condition = status.getConditions().stream().filter(c -> "UnusedStorageConfiguration".equals(c.getReason())).findFirst().orElseThrow();
        assertThat(condition.getMessage(), is("The .spec.kafka.storage section in the Kafka custom resource is ignored when node pools are used and should be removed from the custom resource."));
        assertThat(condition.getType(), is("Warning"));
        assertThat(condition.getStatus(), is("True"));
    }
}
