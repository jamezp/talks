package dev.jperkins.example;

import java.lang.reflect.Method;
import java.time.Instant;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;

import dev.jperkins.example.data.TaskRegistry;
import dev.jperkins.example.model.Priority;
import dev.jperkins.example.model.Task;

/**
 * Tests that the {@link TaskRegistry} works as expected. This test executes within the container and requires tests
 * operation in a specific order.
 *
 * @author <a href="mailto:james@jperkins.dev">James R. Perkins</a>
 */
@QuarkusTest
@RequestScoped
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TaskRegistryTest {

    @Inject
    private TaskRegistry taskRegistry;

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
