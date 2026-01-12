package io.cheshire.core.manager;

/**
 * Enumeration for initialization phases.
 */
public enum InitializationPhase {
    PRE_INIT(0),
    BOOTSTRAP(10),
    SOURCE_PROVIDERS(20),
    QUERY_ENGINES(30),
    CAPABILITIES(40),
    PIPELINES(50),
    POST_INIT(100);

    private final int order;

    InitializationPhase(int order) {
        this.order = order;
    }

    public int order() {
        return order;
    }
}
