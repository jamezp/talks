/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.example;

import java.net.URI;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.sse.SseEventSource;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wildfly.example.model.Priority;
import org.wildfly.example.model.Task;
import org.wildfly.testing.junit.extension.annotation.GenerateDeployment;
import org.wildfly.testing.junit.extension.annotation.ServerResource;
import org.wildfly.testing.junit.extension.annotation.WildFlyTest;

/**
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@WildFlyTest
public class SseTaskResourceTest extends AbstractTaskTest {
    private static final BlockingDeque<Task> EVENTS_QUEUE = new LinkedBlockingDeque<>();
    private static SseEventSource EVENT_SOURCE;
    private static Client CLIENT;

    @ServerResource
    private URI uri;

    @GenerateDeployment
    public static void createDeployment(final WebArchive deployment) {
        deployment.addPackages(true, "org.wildfly.example")
                .addAsResource("META-INF/persistence.xml")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @BeforeEach
    public void startEventSource() {
        if (CLIENT == null) {
            CLIENT = ClientBuilder.newClient();
        }
        if (EVENT_SOURCE == null) {
            EVENT_SOURCE = SseEventSource.target(CLIENT.target(UriBuilder.fromUri(uri))
                    .path("/api/subscribe"))
                    .build();

            // Register event consumers
            EVENT_SOURCE.register((event) -> {
                if (!event.isEmpty()) {
                    final var task = event.readData(Task.class, MediaType.APPLICATION_JSON_TYPE);
                    EVENTS_QUEUE.add(task);
                }
            }, (error) -> Assertions.fail("Failed to receive event: " + error));
            // Start listening for events
            EVENT_SOURCE.open();
        }
    }

    @AfterAll
    public static void shutdownEventSource() {
        if (EVENT_SOURCE != null) {
            EVENT_SOURCE.close();
        }
        if (CLIENT != null) {
            CLIENT.close();
        }
    }

    @Test
    public void addTask() throws Exception {
        final Task toAdd = new Task();
        toAdd.setSummary("This is a test task");
        toAdd.setDescription("This is the description for the test task");
        toAdd.setPriority(Priority.IMPORTANT);
        try (
                Response createdResponse = CLIENT.target(UriBuilder.fromUri(uri).path("api/task/")).request()
                        .post(Entity.json(toAdd))) {
            Assertions.assertEquals(Response.Status.CREATED, createdResponse.getStatusInfo(),
                    () -> String.format("Invalid status: %s", createdResponse.readEntity(String.class)));
            // We should have the location
            try (Response response = CLIENT.target(createdResponse.getLocation()).request().get()) {
                Assertions.assertEquals(Response.Status.OK, response.getStatusInfo(),
                        () -> String.format("Invalid status: %s - %s", createdResponse.readEntity(String.class),
                                createdResponse.getLocation()));
                final Task resolvedTask = response.readEntity(Task.class);
                // Wait for the event to be fired
                final Task evenTask = EVENTS_QUEUE.poll(5L, TimeUnit.SECONDS);
                Assertions.assertNotNull(evenTask,
                        () -> String.format("Failed to get a server-sent event within 5 seconds. Events: %s", EVENTS_QUEUE));
                tasksArqEqual(resolvedTask, evenTask, false);
            }
        }
    }

    @Test
    public void addAndRemoveTask() throws Exception {
        final Task toAdd = new Task();
        toAdd.setSummary("This is a test task which will be removed");
        toAdd.setDescription("This is the description for the test task");
        toAdd.setPriority(Priority.LOW);
        try (
                Response createdResponse = CLIENT.target(UriBuilder.fromUri(uri).path("api/task/")).request()
                        .post(Entity.json(toAdd))) {
            Assertions.assertEquals(Response.Status.CREATED, createdResponse.getStatusInfo(),
                    () -> String.format("Invalid status: %s", createdResponse.readEntity(String.class)));
            // We should have the location
            try (Response response = CLIENT.target(createdResponse.getLocation()).request().get()) {
                Assertions.assertEquals(Response.Status.OK, response.getStatusInfo(),
                        () -> String.format("Invalid status: %s - %s", createdResponse.readEntity(String.class),
                                createdResponse.getLocation()));
                final Task resolvedTask = response.readEntity(Task.class);
                // Wait for the event to be fired
                final Task evenTask = EVENTS_QUEUE.poll(5L, TimeUnit.SECONDS);
                Assertions.assertNotNull(evenTask,
                        () -> String.format("Failed to get a server-sent event within 5 seconds. Events: %s", EVENTS_QUEUE));
                tasksArqEqual(resolvedTask, evenTask, false);
            }
        }
    }
}
