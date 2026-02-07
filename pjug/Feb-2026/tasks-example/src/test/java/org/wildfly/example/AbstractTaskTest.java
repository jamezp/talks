/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.example;

import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.wildfly.example.model.Task;

/**
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
abstract class AbstractTaskTest {

    static void tasksArqEqual(final Task task1, final Task task2, final boolean checkUpdated) {
        Assertions.assertNotNull(task1, "The first task was null and should not have been.");
        Assertions.assertNotNull(task2, "The second task was null and should not have been.");
        assertPropertiesEqual("id's", Task::getId, task1, task2);
        assertPropertiesEqual("summaries", Task::getSummary, task1, task2);
        assertPropertiesEqual("descriptions", Task::getDescription, task1, task2);
        assertPropertiesEqual("added dates", Task::getAdded, task1, task2);
        assertPropertiesEqual("completed indicators", Task::isCompleted, task1, task2);
        assertPropertiesEqual("priorities", Task::getPriority, task1, task2);
        if (checkUpdated) {
            assertPropertiesEqual("updated dates", Task::getUpdated, task1, task2);
        }
    }

    private static <T> void assertPropertiesEqual(final String field, final Function<Task, T> valueGetter, final Task task1,
            final Task task2) {
        Assertions.assertEquals(valueGetter.apply(task1), valueGetter.apply(task2),
                () -> String.format("The %s do not match in the tasks.%nExpected: %s%nFound   : %s", field, task1, task2));
    }
}
