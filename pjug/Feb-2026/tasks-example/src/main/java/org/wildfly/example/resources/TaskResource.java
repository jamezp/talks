/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.example.resources;

import java.time.Instant;
import java.util.Collection;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.wildfly.example.data.TaskRegistry;
import org.wildfly.example.model.Task;

/**
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Path("task")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TaskResource {

    @Inject
    private TaskRegistry taskRegistry;
    @Inject
    private UriInfo uriInfo;

    @GET
    public Collection<Task> allTasks() {
        return taskRegistry.getTasks();
    }

    @GET
    @Path("/completed/{completed}")
    public Collection<Task> getAllCompleted(@PathParam("completed") final boolean completed) {
        return taskRegistry.getTasks(completed);
    }

    @GET
    @Path("/{id}")
    public Task getTask(@PathParam("id") final long id) {
        return taskRegistry.getTaskById(id);
    }

    @POST
    public Response addTask(final Task task) {
        task.setAdded(Instant.now());
        final Task added = taskRegistry.add(task);
        return Response
                .created(uriInfo.getBaseUriBuilder().path("task/" + added.getId()).build())
                .build();
    }

    @PUT
    public Response editTask(final Task task) {
        task.setUpdated(Instant.now());
        final Task added = taskRegistry.updateOrAdd(task);
        if (added == null) {
            return Response.noContent().location(uriInfo.getBaseUriBuilder().path("task/" + task.getId()).build()).build();
        }
        return Response
                .created(uriInfo.getBaseUriBuilder().path("task/" + added.getId()).build())
                .build();
    }

    @DELETE
    @Path("/{id}")
    public Task delete(@PathParam("id") final long id) {
        return taskRegistry.remove(id);
    }

    @PATCH
    @Consumes("application/merge-patch+json")
    @Path("/{id}")
    public Task update(@PathParam("id") final long id, final Task task) {
        try {
            return taskRegistry.updateCompleted(id, task);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("No task with id " + id + " found");
        }
    }
}
