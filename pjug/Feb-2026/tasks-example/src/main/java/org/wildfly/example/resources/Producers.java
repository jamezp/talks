/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.example.resources;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;

/**
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@ApplicationScoped
public class Producers {

    @Inject
    private Sse sse;

    /**
     * Creates the broadcaster used for the application via injection.
     *
     * @return the broadcaster to use
     */
    @Produces
    @ApplicationScoped
    public SseBroadcaster broadcaster() {
        return sse.newBroadcaster();
    }

    /**
     * Close the broadcaster when we dispose the instance.
     *
     * @param broadcaster the broadcaster to close
     */
    void closeBroadcaster(@Disposes final SseBroadcaster broadcaster) {
        broadcaster.close(true);
    }
}
