/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.optimizer;

import io.cheshire.spi.query.request.LogicalQuery;
import io.cheshire.spi.query.request.QueryEngineContext;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

/** Engine-level runtime context for a single query execution. Immutable except for cancel flag. */
public final class QueryRuntimeContext {

  private final Map<String, Object> parameters;
  private final TimeZone timeZone;
  private final Locale locale;
  private final AtomicBoolean cancelFlag;

  private final String userId;
  private final String traceId;

  private QueryRuntimeContext(Builder builder) {
    this.parameters = builder.parameters != null ? Map.copyOf(builder.parameters) : Map.of();
    this.timeZone = builder.timeZone != null ? builder.timeZone : TimeZone.getDefault();
    this.locale = builder.locale != null ? builder.locale : Locale.getDefault();
    this.cancelFlag = builder.cancelFlag != null ? builder.cancelFlag : new AtomicBoolean(false);
    this.userId = builder.userId;
    this.traceId = builder.traceId;
  }

  public static Builder fromQuery(LogicalQuery query, QueryEngineContext ctx) {
    Builder builder = new Builder();
    builder.parameters = query != null ? query.parameters() : Map.of();
    if (ctx != null) {
      builder.userId = ctx.userId();
      builder.traceId = ctx.traceId();
      // TODO: review QueryEngineContext and what needs to be added to it
      //      builder.timeZone = ctx.timeZone() != null ? ctx.timeZone() : TimeZone.getDefault();
      //      builder.locale = ctx.locale() != null ? ctx.locale() : Locale.getDefault();
    }
    return builder;
  }

  public static class Builder {
    private Map<String, Object> parameters;
    private TimeZone timeZone;
    private Locale locale;
    private AtomicBoolean cancelFlag;
    private String userId;
    private String traceId;

    // Optional overrides
    public Builder withParameters(Map<String, Object> parameters) {
      this.parameters = parameters;
      return this;
    }

    public Builder withTimeZone(TimeZone timeZone) {
      this.timeZone = timeZone;
      return this;
    }

    public Builder withLocale(Locale locale) {
      this.locale = locale;
      return this;
    }

    public Builder withCancelFlag(AtomicBoolean cancelFlag) {
      this.cancelFlag = cancelFlag;
      return this;
    }

    public Builder withUserId(String userId) {
      this.userId = userId;
      return this;
    }

    public Builder withTraceId(String traceId) {
      this.traceId = traceId;
      return this;
    }

    public QueryRuntimeContext build() {
      return new QueryRuntimeContext(this);
    }
  }

  // -------------------------
  // Accessors
  // -------------------------
  public boolean hasParameter(String name) {
    return parameters.containsKey(name);
  }

  public Object parameter(String name) {
    return parameters.get(name);
  }

  public Map<String, Object> parameters() {
    return parameters;
  }

  public TimeZone timeZone() {
    return timeZone;
  }

  public Locale locale() {
    return locale;
  }

  public AtomicBoolean cancelFlag() {
    return cancelFlag;
  }

  public String userId() {
    return userId;
  }

  public String traceId() {
    return traceId;
  }
}
