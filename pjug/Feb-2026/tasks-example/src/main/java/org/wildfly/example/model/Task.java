/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.example.model;

import java.time.Instant;
import java.util.Objects;

import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.ws.rs.FormParam;

import org.wildfly.example.resources.TaskListener;

/**
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Entity
@NamedQuery(name = "findAll", query = "SELECT t FROM Task t ORDER BY t.priority, t.added ASC, t.summary ASC")
@NamedQuery(name = "findCompleted", query = "SELECT t FROM Task t WHERE t.completed = :completed ORDER BY t.priority, t.added ASC, t.summary ASC")
@EntityListeners(TaskListener.class)
@JsonbDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(updatable = false, nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Instant added;

    @Column(insertable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Instant updated;

    @FormParam("completed")
    private boolean completed;

    @Column(nullable = false)
    @FormParam("summary")
    private String summary;

    @Column
    @FormParam("description")
    private String description;

    @Column
    @Enumerated
    @FormParam("priority")
    private Priority priority;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Instant getAdded() {
        return added;
    }

    public void setAdded(final Instant added) {
        this.added = added;
    }

    public Instant getUpdated() {
        return updated;
    }

    public void setUpdated(final Instant updated) {
        this.updated = updated;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(final boolean completed) {
        this.completed = completed;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(final String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(final Priority priority) {
        this.priority = priority;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof final Task other)) {
            return false;
        }
        return Objects.equals(getId(), other.getId());
    }

    @Override
    public String toString() {
        return "Task[id=" + getId() + ", completed=" + isCompleted() + ", priority=" + getPriority() + ", summary="
                + getSummary()
                + ", description=" + getDescription() + ", added=" + getAdded() + ", updated=" + getUpdated() + "]";
    }
}
