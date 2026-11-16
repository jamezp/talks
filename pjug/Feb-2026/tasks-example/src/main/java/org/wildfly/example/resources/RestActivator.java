package org.wildfly.example.resources;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Indicates that the deployment is a REST deployment.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@ApplicationPath("/api")
public class RestActivator extends Application {
}
