/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.example;

import java.net.URI;
import java.util.Collection;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.wildfly.example.model.Task;
import org.wildfly.testing.junit.extension.annotation.GenerateDeployment;
import org.wildfly.testing.junit.extension.annotation.RequestPath;
import org.wildfly.testing.junit.extension.annotation.ServerResource;
import org.wildfly.testing.junit.extension.annotation.WildFlyTest;

/**
 * Tests the CRUD actions of the task resource. Tests here are ordered by the {@link Order} annotation. While it's not
 * ideal to require test order, for a CRUD resource the order is important.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@WildFlyTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CrudTaskResourceTest extends AbstractTaskTest {
    private static final Task DEFAULT_TASK = new Task();

    static {
        DEFAULT_TASK.setSummary("Test task summary");
        DEFAULT_TASK.setDescription("Test task description");
    }

    @ServerResource
    @RequestPath("api/task")
    private URI uri;

    @GenerateDeployment
    public static void createDeployment(final WebArchive deployment) {
        deployment.addPackages(true, "org.wildfly.example")
                .addAsResource("META-INF/persistence.xml")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    @Order(1)
    public void addTask() {
        try (
                Client client = ClientBuilder.newClient();
                Response createdResponse = client.target(UriBuilder.fromUri(uri)).request()
                        .post(Entity.json(DEFAULT_TASK))) {
            Assertions.assertEquals(Response.Status.CREATED, createdResponse.getStatusInfo(),
                    () -> String.format("Invalid status: %s", createdResponse.readEntity(String.class)));
            // We should have the location
            try (Response response = client.target(createdResponse.getLocation()).request().get()) {
                Assertions.assertEquals(Response.Status.OK, response.getStatusInfo(),
                        () -> String.format("Invalid status: %s - %s", createdResponse.readEntity(String.class),
                                createdResponse.getLocation()));
                final Task resolvedTask = response.readEntity(Task.class);
                // Set the ID and the added date
                DEFAULT_TASK.setId(resolvedTask.getId());
                DEFAULT_TASK.setAdded(resolvedTask.getAdded());
                tasksArqEqual(resolvedTask, false);
            }
        }
    }

    @Test
    @Order(2)
    public void singleTask() {
        try (
                Client client = ClientBuilder.newClient();
                Response response = client.target(UriBuilder.fromUri(uri))
                        .request(MediaType.APPLICATION_JSON)
                        .get()) {
            Assertions.assertEquals(Response.Status.OK, response.getStatusInfo(),
                    () -> String.format("Invalid status: %s", response.readEntity(String.class)));
            final Collection<Task> tasks = response.readEntity(new GenericType<>() {
            });
            Assertions.assertEquals(1, tasks.size(),
                    () -> String.format("Expected a single task, but got %s", tasks));
            tasksArqEqual(tasks.iterator().next(), false);
        }
    }

    @Test
    @Order(3)
    public void getTask() {
        try (
                Client client = ClientBuilder.newClient();
                Response response = client.target(UriBuilder.fromUri(uri).path("1"))
                        .request(MediaType.APPLICATION_JSON)
                        .get()) {
            Assertions.assertEquals(Response.Status.OK, response.getStatusInfo(),
                    () -> String.format("Invalid status: %s", response.readEntity(String.class)));
            final Task resolvedTask = response.readEntity(Task.class);
            tasksArqEqual(resolvedTask, false);
        }
    }

    @Test
    @Order(4)
    public void editTask() {
        DEFAULT_TASK.setSummary("Changed: " + DEFAULT_TASK.getSummary());
        DEFAULT_TASK.setDescription("Changed: " + DEFAULT_TASK.getDescription());
        try (
                Client client = ClientBuilder.newClient();
                Response createdResponse = client.target(UriBuilder.fromUri(uri))
                        .request(MediaType.APPLICATION_JSON)
                        .put(Entity.json(DEFAULT_TASK))) {
            Assertions.assertEquals(Response.Status.NO_CONTENT, createdResponse.getStatusInfo(),
                    () -> String.format("Invalid status: %s", createdResponse.readEntity(String.class)));
            // We should have the location
            try (Response response = client.target(createdResponse.getLocation()).request().get()) {
                Assertions.assertEquals(Response.Status.OK, response.getStatusInfo(),
                        () -> String.format("Invalid status: %s - %s", createdResponse.readEntity(String.class),
                                createdResponse.getLocation()));
                final Task resolvedTask = response.readEntity(Task.class);
                DEFAULT_TASK.setUpdated(resolvedTask.getUpdated());
                tasksArqEqual(resolvedTask, true);
            }
        }
    }

    @Test
    @Order(5)
    public void completeTask() {
        final Task newTask = new Task();
        newTask.setId(DEFAULT_TASK.getId());
        newTask.setCompleted(true);
        DEFAULT_TASK.setCompleted(true);
        try (
                Client client = ClientBuilder.newClient();
                Response response = client.target(UriBuilder.fromUri(uri).path(String.valueOf(DEFAULT_TASK.getId())))
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .method("PATCH", Entity.entity(newTask, "application/merge-patch+json"))) {
            Assertions.assertEquals(Response.Status.OK, response.getStatusInfo(),
                    () -> String.format("Invalid status: %s", response.readEntity(String.class)));
            final Task resolvedTask = response.readEntity(Task.class);
            DEFAULT_TASK.setUpdated(resolvedTask.getUpdated());
            tasksArqEqual(resolvedTask, true);
        }
    }

    @Test
    @Order(5)
    public void removeCompletedTask() {
        final Task newTask = new Task();
        newTask.setId(DEFAULT_TASK.getId());
        newTask.setCompleted(false);
        DEFAULT_TASK.setCompleted(false);
        try (
                Client client = ClientBuilder.newClient();
                Response response = client.target(UriBuilder.fromUri(uri).path(String.valueOf(DEFAULT_TASK.getId())))
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .method("PATCH", Entity.entity(newTask, "application/merge-patch+json"))) {
            Assertions.assertEquals(Response.Status.OK, response.getStatusInfo(),
                    () -> String.format("Invalid status: %s", response.readEntity(String.class)));
            final Task resolvedTask = response.readEntity(Task.class);
            DEFAULT_TASK.setUpdated(resolvedTask.getUpdated());
            tasksArqEqual(resolvedTask, true);
        }
    }

    @Test
    @Order(7)
    public void deleteTask() {
        try (
                Client client = ClientBuilder.newClient();
                Response response = client.target(UriBuilder.fromUri(uri).path("1"))
                        .request(MediaType.APPLICATION_JSON)
                        .delete()) {
            Assertions.assertEquals(Response.Status.OK, response.getStatusInfo(),
                    () -> String.format("Invalid status: %s", response.readEntity(String.class)));
            final Task resolvedTask = response.readEntity(Task.class);
            tasksArqEqual(resolvedTask, false);
        }
    }

    @Test
    @Order(8)
    public void emptyTasks() {
        try (
                Client client = ClientBuilder.newClient();
                Response response = client.target(UriBuilder.fromUri(uri))
                        .request(MediaType.APPLICATION_JSON)
                        .get()) {
            Assertions.assertEquals(Response.Status.OK, response.getStatusInfo(),
                    () -> String.format("Invalid status: %s", response.readEntity(String.class)));
            final Collection<Task> tasks = response.readEntity(new GenericType<>() {
            });
            Assertions.assertTrue(tasks.isEmpty(),
                    () -> String.format("Expected an empty set of tasks, but got %s", tasks));
        }
    }

    private static void tasksArqEqual(final Task secondary, final boolean checkUpdated) {
        tasksArqEqual(DEFAULT_TASK, secondary, checkUpdated);
    }

}
