/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.example;

import java.lang.reflect.Method;
import java.time.Instant;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.container.annotation.ArquillianTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.wildfly.example.data.TaskRegistry;
import org.wildfly.example.model.Priority;
import org.wildfly.example.model.Task;
import org.wildfly.example.resources.Producers;
import org.wildfly.example.resources.TaskListener;

/**
 * Tests that the {@link TaskRegistry} works as expected. This test executes within the container and requires tests
 * operation in a specific order.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@ArquillianTest
@RequestScoped
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TaskRegistryIT {

    @Inject
    private TaskRegistry taskRegistry;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                // Note for this test we don't use the REST endpoints so we don't need the REST resources
                .addClasses(TaskRegistry.class,
                        Priority.class,
                        Task.class,
                        Producers.class,
                        TaskListener.class)
                .addAsResource("META-INF/persistence.xml")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    @Order(1)
    public void addTask(final TestInfo testInfo) {
        final Task task = new Task();
        task.setAdded(Instant.now());
        task.setDescription("This is a test task from " + testInfo.getTestMethod()
                .map(Method::getName)
                .orElse("<unknown>"));
        task.setPriority(Priority.IMPORTANT);
        task.setSummary("Test summary");
        final var addedTask = taskRegistry.add(task);
        Assertions.assertEquals(task, addedTask);
    }

    @Test
    @Order(2)
    public void queryTasks() {
        final var tasks = taskRegistry.getTasks();
        Assertions.assertEquals(1, tasks.size());
        // The task should have an id of 1
        Assertions.assertEquals(1L, tasks.iterator().next().getId(),
                () -> String.format("Failed to find task with id %s in %s", 1L, tasks));
    }

    @Test
    @Order(3)
    public void updateTask(final TestInfo testInfo) {
        final var task = taskRegistry.getTaskById(1L);
        Assertions.assertNotNull(task);
        // Update the task
        task.setUpdated(Instant.now());
        task.setDescription(
                "This is an updated test task from " + testInfo.getTestMethod()
                        .map(Method::getName)
                        .orElse("<unknown>"));
        var updatedTask = taskRegistry.updateOrAdd(task);
        Assertions.assertNull(updatedTask, "An updated task should return null");

        final var newTask = new Task();
        newTask.setAdded(Instant.now());
        newTask.setDescription("This is a test task from " + testInfo.getTestMethod()
                .map(Method::getName)
                .orElse("<unknown>"));
        newTask.setPriority(Priority.IMPORTANT);
        newTask.setSummary("Test new task summary");
        updatedTask = taskRegistry.updateOrAdd(newTask);
        Assertions.assertEquals(newTask, updatedTask);
    }

    @Test
    @Order(4)
    public void completeTask() {
        final var task = taskRegistry.getTaskById(2L);
        Assertions.assertNotNull(task);
        final var patchedTask = new Task();
        patchedTask.setCompleted(true);
        final var completedTask = taskRegistry.updateCompleted(2L, patchedTask);
        Assertions.assertTrue(completedTask.isCompleted());
    }

    @Test
    @Order(5)
    public void completedTasks() {
        final var completedTasks = taskRegistry.getTasks(true);
        Assertions.assertEquals(1, completedTasks.size(),
                () -> String.format("Expected 1 completed tasks in %s", completedTasks));
        final var openTasks = taskRegistry.getTasks(false);
        Assertions.assertEquals(1, openTasks.size(), () -> String.format("Expected 1 open tasks in %s", openTasks));
    }

    @Test
    @Order(6)
    public void deleteTask() {
        final var removedTask = taskRegistry.remove(2L);
        Assertions.assertNotNull(removedTask);
        final var completedTasks = taskRegistry.getTasks(true);
        Assertions.assertEquals(0, completedTasks.size(),
                () -> String.format("Expected 0 completed tasks in %s", completedTasks));
        final var openTasks = taskRegistry.getTasks(false);
        Assertions.assertEquals(1, openTasks.size(), () -> String.format("Expected 1 open tasks in %s", openTasks));
    }
}
