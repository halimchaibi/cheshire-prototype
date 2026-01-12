package io.cheshire.spi.query.engine;

@FunctionalInterface
public interface ConfigAdapter<T> {
    QueryEngineConfig adapt(String name, T queryEngineConfig);
}