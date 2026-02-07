/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.example.resources;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;

import org.jboss.logging.Logger;
import org.wildfly.example.model.Task;

/**
 * A listener for changes of task entities. Once an add, update or delete has been made a notification is sent to
 * registered subscribers.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@ApplicationScoped
@Path("/subscribe")
public class TaskListener {
    private static final Logger LOGGER = Logger.getLogger(TaskListener.class);

    @Inject
    private Sse sse;

    @Inject
    private SseBroadcaster broadcaster;

    /**
     * Subscribes a client to the events of an entity.
     *
     * @param sink the event sink to register
     */
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void subscribe(@Context final SseEventSink sink) {
        broadcaster.register(sink);
    }

    /**
     * Sends a notification to the subscribed clients that an entity as been added.
     *
     * @param task the task that has been added
     */
    @PostPersist
    public void notifyAdded(final Task task) {
        notifyClient(task, "task.persist.added");
    }

    /**
     * Sends a notification to the subscribed clients that an entity as been updated.
     *
     * @param task the task that has been updated
     */
    @PostUpdate
    public void notifyUpdated(final Task task) {
        notifyClient(task, "task.persist.updated");
    }

    /**
     * Sends a notification to the subscribed clients that an entity as been deleted.
     *
     * @param task the task that has been deleted
     */
    @PostRemove
    public void notifyRemoved(final Task task) {
        notifyClient(task, "task.persist.removed");
    }

    private void notifyClient(final Task task, final String name) {
        broadcaster.broadcast(sse.newEventBuilder()
                .name(name)
                .data(task)
                .mediaType(MediaType.APPLICATION_JSON_TYPE).build())
                .whenComplete((value, error) -> {
                    if (error != null) {
                        LOGGER.errorf(error, "Failed to notify clients of event %s: %s", name, value);
                    }
                });
    }
}
