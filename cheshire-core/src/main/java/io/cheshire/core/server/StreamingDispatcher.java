/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core.server;

import io.cheshire.core.CheshireSession;
import io.cheshire.core.server.protocol.RequestEnvelope;

public record StreamingDispatcher(
        CheshireSession session) implements CheshireDispatcher {

    // TODO: Needs return a published
    @Override
    public ResponseEntity dispatch(RequestEnvelope envelope) {
        return null;
    }
}
