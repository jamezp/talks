/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.example.model;

import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.annotation.JsonbTypeAdapter;

/**
 * The priority for a task.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@JsonbTypeAdapter(Priority.OrdinalTypeAdaptor.class)
public enum Priority {
    URGENT,
    IMPORTANT,
    MEDIUM,
    LOW;

    /**
     * Used to for using the ordinal value of the enum for JSON serialization and deserialization.
     */
    protected static class OrdinalTypeAdaptor implements JsonbAdapter<Priority, Integer> {
        @Override
        public Integer adaptToJson(final Priority obj) {
            return obj == null ? 2 : obj.ordinal();
        }

        @Override
        public Priority adaptFromJson(final Integer obj) {
            if (obj == null) {
                return MEDIUM;
            }
            return switch (obj) {
                case 0 -> URGENT;
                case 1 -> IMPORTANT;
                case 3 -> LOW;
                default -> MEDIUM;
            };
        }
    }
}
