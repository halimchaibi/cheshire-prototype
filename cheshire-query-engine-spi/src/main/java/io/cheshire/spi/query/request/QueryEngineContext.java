/*-
 * #%L
 * Cheshire :: Query Engine :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.query.request;

import io.cheshire.spi.source.SourceProvider;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public record QueryEngineContext(
    String sessionId,
    String userId,
    String traceId,
    Map<String, Object> securityContext,
    List<SourceProvider<?>> sources,
    ConcurrentMap<String, Object> attributes,
    Instant arrivalTime,
    Instant deadline) {

  public QueryEngineContext(
      String sessionId,
      String userId,
      String traceId,
      Map<String, Object> securityContext,
      List<SourceProvider<?>> sources,
      ConcurrentMap<String, Object> attributes,
      Instant arrivalTime,
      Instant deadline) {
    this.sessionId = sessionId;
    this.userId = userId;
    this.traceId = traceId;

    this.securityContext =
        securityContext != null
            ? Collections.unmodifiableMap(securityContext)
            : Collections.emptyMap();
    this.sources =
        sources != null ? Collections.unmodifiableList(sources) : Collections.emptyList();
    this.attributes = attributes != null ? attributes : new ConcurrentHashMap<>();
    this.arrivalTime = arrivalTime != null ? arrivalTime : Instant.now();

    this.deadline = deadline;
  }

  private QueryEngineContext() {
    this(null, null, null, null, null, null, null, null);
  }

  public static QueryEngineContext empty() {
    return new QueryEngineContext();
  }

  public Object putIfAbsent(String key, Object value) {
    if (key == null) {
      throw new IllegalArgumentException("Attribute key cannot be null");
    }
    return attributes.putIfAbsent(key, value);
  }

  @Override
  public ConcurrentMap<String, Object> attributes() {
    // TODO: This approach is an attempt of POSA-style controlled mutation ... may need to revisit
    // based on
    // performance profiling.
    return new ConcurrentHashMap<>(attributes);
  }
}
