package dev.jperkins.example.model;

import jakarta.inject.Singleton;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.annotation.JsonbTypeAdapter;

/**
 * The priority for a task.
 *
 * @author <a href="mailto:james@jperkins.dev">James R. Perkins</a>
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
    @Singleton
    public static class OrdinalTypeAdaptor implements JsonbAdapter<Priority, Integer> {
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
