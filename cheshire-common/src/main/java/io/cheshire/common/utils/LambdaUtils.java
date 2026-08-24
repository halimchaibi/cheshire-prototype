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

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.*;
import java.util.stream.Stream;

public class LambdaUtils {

  public static <T> Consumer<T> consumer(ThrowingConsumer<T, Exception> action) {
    return t -> {
      try {
        action.accept(t);
      } catch (Exception e) {
        throwAsUnchecked(e);
      }
    };
  }

  public static <K, V> BiConsumer<K, V> biConsumer(ThrowingBiConsumer<K, V, Exception> action) {
    return (k, v) -> {
      try {
        action.accept(k, v);
      } catch (Exception e) {
        throwAsUnchecked(e);
      }
    };
  }

  // The "Sneaky Throw" trick - allows propagating checked exceptions
  // without wrapping them in RuntimeException
  @SuppressWarnings("unchecked")
  private static <E extends Throwable> void throwAsUnchecked(Throwable exception) throws E {
    throw (E) exception;
  }

  @FunctionalInterface
  public interface ThrowingConsumer<T, E extends Exception> {
    void accept(T t) throws E;
  }

  @FunctionalInterface
  public interface ThrowingBiConsumer<K, V, E extends Exception> {
    void accept(K k, V v) throws E;
  }

  @FunctionalInterface
  public interface CheckedSupplier<T> {
    T get() throws Exception;
  }

  @FunctionalInterface
  public interface CheckedFunction<T, R> {
    R apply(T t) throws Exception;
  }

  @FunctionalInterface
  public interface CheckedConsumer<T> {
    void accept(T t) throws Exception;
  }

  @FunctionalInterface
  public interface CheckedRunnable {
    void run() throws Exception;
  }

  public sealed interface Try<T> {

    static <T> Try<T> of(CheckedSupplier<T> supplier) {
      try {
        return new Success<>(supplier.get());
      } catch (Exception e) {
        return new Failure<>(e);
      }
    }

    static <T> Try<T> success(T value) {
      return new Success<>(value);
    }

    static <T> Try<T> failure(Exception exception) {
      return new Failure<>(exception);
    }

    static Try<Void> run(CheckedRunnable runnable) {
      try {
        runnable.run();
        return new Success<>(null);
      } catch (Exception e) {
        return new Failure<>(e);
      }
    }

    record Success<T>(T value) implements Try<T> {}

    record Failure<T>(Exception exception) implements Try<T> {}

    default <U> Try<U> map(Function<? super T, ? extends U> mapper) {
      return switch (this) {
        case Success(var v) -> {
          try {
            yield new Success<>(mapper.apply(v));
          } catch (Exception e) {
            yield new Failure<>(e);
          }
        }
        case Failure(var e) -> new Failure<>(e);
      };
    }

    default <U> Try<U> flatMap(Function<? super T, Try<U>> mapper) {
      return switch (this) {
        case Success(var v) -> {
          try {
            yield mapper.apply(v);
          } catch (Exception e) {
            yield new Failure<>(e);
          }
        }
        case Failure(var e) -> new Failure<>(e);
      };
    }

    default <U> Try<U> mapTry(CheckedFunction<? super T, ? extends U> mapper) {
      return switch (this) {
        case Success(var v) -> Try.of(() -> mapper.apply(v));
        case Failure(var e) -> new Failure<>(e);
      };
    }

    default Try<T> recover(Function<? super Exception, ? extends T> recovery) {
      return switch (this) {
        case Success<T> s -> s;
        case Failure(var e) -> {
          try {
            yield new Success<>(recovery.apply(e));
          } catch (Exception ex) {
            yield new Failure<>(ex);
          }
        }
      };
    }

    default Try<T> recoverWith(Function<? super Exception, Try<T>> recovery) {
      return switch (this) {
        case Success<T> s -> s;
        case Failure(var e) -> {
          try {
            yield recovery.apply(e);
          } catch (Exception ex) {
            yield new Failure<>(ex);
          }
        }
      };
    }

    default Try<T> recoverTry(CheckedFunction<? super Exception, ? extends T> recovery) {
      return switch (this) {
        case Success<T> s -> s;
        case Failure(var e) -> Try.of(() -> recovery.apply(e));
      };
    }

    default T get() {
      return switch (this) {
        case Success(var v) -> v;
        case Failure(var e) -> throw new RuntimeException("Try.get() called on Failure", e);
      };
    }

    default T getOrThrow() throws Exception {
      return switch (this) {
        case Success(var v) -> v;
        case Failure(var e) -> throw e;
      };
    }

    default T orElse(T defaultValue) {
      return switch (this) {
        case Success(var v) -> v;
        case Failure(var e) -> defaultValue;
      };
    }

    default T orElseGet(Supplier<? extends T> supplier) {
      return switch (this) {
        case Success(var v) -> v;
        case Failure(var e) -> supplier.get();
      };
    }

    default T orElseThrow(Function<? super Exception, ? extends RuntimeException> exceptionMapper) {
      return switch (this) {
        case Success(var v) -> v;
        case Failure(var e) -> throw exceptionMapper.apply(e);
      };
    }

    default <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
      return switch (this) {
        case Success(var v) -> v;
        case Failure(var e) -> throw exceptionSupplier.get();
      };
    }

    // Side effects
    default Try<T> onSuccess(Consumer<? super T> action) {
      if (this instanceof Success(var v)) {
        action.accept(v);
      }
      return this;
    }

    default Try<T> onFailure(Consumer<? super Exception> action) {
      if (this instanceof Failure(var e)) {
        action.accept(e);
      }
      return this;
    }

    default Try<T> peek(Consumer<? super T> action) {
      return onSuccess(action);
    }

    default Try<T> peekTry(CheckedConsumer<? super T> action) {
      return switch (this) {
        case Success(var v) -> {
          try {
            action.accept(v);
            yield this;
          } catch (Exception e) {
            yield new Failure<>(e);
          }
        }
        case Failure<T> f -> f;
      };
    }

    default boolean isSuccess() {
      return this instanceof Success;
    }

    default boolean isFailure() {
      return this instanceof Failure;
    }

    default Try<T> filter(Predicate<? super T> predicate) {
      return switch (this) {
        case Success(var v) ->
            predicate.test(v)
                ? this
                : new Failure<>(new NoSuchElementException("Predicate does not hold for " + v));
        case Failure<T> f -> f;
      };
    }

    default Try<T> filterTry(CheckedFunction<? super T, Boolean> predicate) {
      return switch (this) {
        case Success(var v) -> {
          try {
            yield predicate.apply(v)
                ? this
                : new Failure<>(new NoSuchElementException("Predicate does not hold for " + v));
          } catch (Exception e) {
            yield new Failure<>(e);
          }
        }
        case Failure<T> f -> f;
      };
    }

    default Optional<T> toOptional() {
      return switch (this) {
        case Success(var v) -> Optional.ofNullable(v);
        case Failure(var e) -> Optional.empty();
      };
    }

    default Optional<Exception> toFailureOptional() {
      return switch (this) {
        case Success(var v) -> Optional.empty();
        case Failure(var e) -> Optional.of(e);
      };
    }

    default Stream<T> stream() {
      return switch (this) {
        case Success(var v) -> Stream.of(v);
        case Failure(var e) -> Stream.empty();
      };
    }

    default <U> U fold(
        Function<? super Exception, ? extends U> failureMapper,
        Function<? super T, ? extends U> successMapper) {
      return switch (this) {
        case Success(var v) -> successMapper.apply(v);
        case Failure(var e) -> failureMapper.apply(e);
      };
    }

    default <U> Try<U> andThen(CheckedSupplier<U> supplier) {
      return switch (this) {
        case Success(var v) -> Try.of(supplier);
        case Failure(var e) -> new Failure<>(e);
      };
    }

    default <U, R> Try<R> zip(
        Try<U> other, BiFunction<? super T, ? super U, ? extends R> combiner) {
      return switch (this) {
        case Success(var v1) ->
            switch (other) {
              case Success(var v2) -> {
                try {
                  yield new Success<>(combiner.apply(v1, v2));
                } catch (Exception e) {
                  yield new Failure<>(e);
                }
              }
              case Failure(var e2) -> new Failure<>(e2);
            };
        case Failure(var e1) -> new Failure<>(e1);
      };
    }
  }
}
