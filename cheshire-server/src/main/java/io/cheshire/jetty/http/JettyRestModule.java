/*-
 * #%L
 * Cheshire :: Servers
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.jetty.http;

import io.cheshire.core.config.CheshireConfig;
import io.cheshire.core.server.CheshireDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;

import java.util.Objects;

@Slf4j
public final class JettyRestModule {

    private final CheshireDispatcher dispatcher;
    private final CheshireConfig.Exposure exposure;

    public JettyRestModule(CheshireConfig.Exposure exposure, CheshireDispatcher dispatcher) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "Dispatcher cannot be null");
        this.exposure = Objects.requireNonNull(exposure, "Exposure cannot be null");
    }

    /**
     * Creates and configures the ServletContextHandler for REST. This matches your original logic but keeps it
     * isolated.
     */
    public ServletContextHandler createHandler() {

        String baseUrl = exposure.getConfig().get("path") instanceof String path ? path : "/api/v1";

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        RestRequestHandler handler = new RestRequestHandler(dispatcher::dispatch);
        RestAdapterServlet servlet = new RestAdapterServlet(handler);
        context.addServlet(new ServletHolder(servlet), baseUrl + "/*");

        log.debug("REST Module context initialized for {}/*", baseUrl);
        return context;
    }
}
