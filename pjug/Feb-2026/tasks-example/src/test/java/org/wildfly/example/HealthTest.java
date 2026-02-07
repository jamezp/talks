/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.example;

import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wildfly.example.resources.HealthResource;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;

/**
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap(HealthTest.TestApplication.class)
public class HealthTest {

    @Test
    public void testHealth(@RequestPath("test/status") final WebTarget target) {
        Assertions.assertEquals("UP", target.request().get().readEntity(String.class));
    }

    @ApplicationPath("/test")
    public static class TestApplication extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            return Set.of(HealthResource.class);
        }
    }
}
