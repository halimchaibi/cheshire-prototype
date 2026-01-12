package io.cheshire.spi.source;

@FunctionalInterface
public interface ConfigAdapter<T> {
    SourceConfig adapt(String name, T sourceDef);
}