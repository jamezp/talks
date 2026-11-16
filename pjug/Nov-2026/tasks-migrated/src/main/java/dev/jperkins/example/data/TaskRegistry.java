package dev.jperkins.example.data;

import java.time.Instant;
import java.util.Collection;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;

import dev.jperkins.example.model.Task;

/**
 * @author <a href="mailto:james@jperkins.dev">James R. Perkins</a>
 */
@RequestScoped
@Transactional
public class TaskRegistry {
    @Inject
    private EntityManager em;

    /**
     * Returns all available tasks.
     *
     * @return all available tasks
     */
    public Collection<Task> getTasks() {
        return em.createNamedQuery("findAll", Task.class).getResultList();
    }

    /**
     * Returns all available tasks.
     *
     * @return all available tasks
     */
    public Collection<Task> getTasks(final boolean completed) {
        return em.createNamedQuery("findCompleted", Task.class).setParameter("completed", completed).getResultList();
    }

    public Task getTaskById(final long id) {
        return em.find(Task.class, id);
    }

    /**
     * Adds a task to the repository.
     *
     * @param task the task to add
     *
     * @return the task
     */
    public Task add(@NotNull final Task task) {
        em.persist(task);
        return task;
    }

    /**
     * Adds a task to the repository.
     *
     * @param task the task to add
     *
     * @return the task
     */
    public Task updateOrAdd(@NotNull final Task task) {
        if (task.getId() == null) {
            return add(task);
        }
        final Task taskToUpdate = em.find(Task.class, task.getId());
        if (taskToUpdate != null) {
            taskToUpdate.setCompleted(task.isCompleted());
            taskToUpdate.setDescription(task.getDescription());
            taskToUpdate.setSummary(task.getSummary());
            taskToUpdate.setUpdated(Instant.now());
            taskToUpdate.setPriority(task.getPriority());
            em.merge(taskToUpdate);
            return null;
        }
        return add(task);
    }

    /**
     * Updates the completed status of a task.
     *
     * @param id   the task ID
     * @param task the partial task
     *
     * @return the updated task
     */
    public Task updateCompleted(final long id, final Task task) {
        final Task taskToUpdate = em.find(Task.class, id);
        if (taskToUpdate != null) {
            taskToUpdate.setCompleted(task.isCompleted());
            taskToUpdate.setUpdated(Instant.now());
            em.merge(taskToUpdate);
            return taskToUpdate;
        }
        throw new IllegalArgumentException("Task with id %d does not exist".formatted(id));
    }

    public Task remove(final long id) {
        final Task task = em.find(Task.class, id);
        if (task != null) {
            em.remove(task);
            return task;
        }
        return null;
    }
}
