/*-
 * #%L
 * Cheshire :: Common Utils
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.common.utils;

import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceUtils {

  private static final Logger log = LoggerFactory.getLogger(ServiceUtils.class);

  /** Loads the first available implementation of a service. */
  public static <T> Optional<T> loadFirst(Class<T> serviceClass) {
    ServiceLoader<T> loader = ServiceLoader.load(serviceClass, serviceClass.getClassLoader());

    return StreamSupport.stream(loader.spliterator(), false).findFirst();
  }

  /** Discovers all implementations of a service and maps them by their Class Name. */
  public static <T> Map<String, T> loadAll(Class<T> serviceClass) {
    ServiceLoader<T> loader = ServiceLoader.load(serviceClass, serviceClass.getClassLoader());

    return loader.stream()
        .map(ServiceLoader.Provider::get)
        .peek(f -> log.info("Found {}: {}", serviceClass.getSimpleName(), f.getClass().getName()))
        .collect(Collectors.toMap(f -> f.getClass().getName(), f -> f, (existing, _) -> existing));
  }
}
