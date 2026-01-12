package io.cheshire.query.engine.calcite;

import io.cheshire.core.config.CheshireConfig;
import io.cheshire.spi.query.engine.ConfigAdapter;
import io.cheshire.spi.query.engine.QueryEngineFactory;
import io.cheshire.spi.query.exception.QueryEngineException;

public class CalciteQueryEngineFactory implements QueryEngineFactory<CalciteQueryEngineConfig, CalciteQueryEngine, CheshireConfig.QueryEngine> {

    public CalciteQueryEngineFactory() {
        // For ServiceLoader
    }

    @Override
    public CalciteQueryEngine create(CalciteQueryEngineConfig config) throws QueryEngineException {
        try {
            return new CalciteQueryEngine(config);
        } catch (IllegalArgumentException e) {
            throw new QueryEngineException("Failed to create CalciteQueryEngine", e);
        }
    }

    @Override
    public ConfigAdapter<CheshireConfig.QueryEngine> adapter() {
        return (name, queryDef) -> CalciteQueryEngineConfig.from(name, queryDef);
    }

    @Override
    public Class<CalciteQueryEngineConfig> configClass() {
        return CalciteQueryEngineConfig.class;
    }

    @Override
    public Class<CalciteQueryEngine> queryEngineClass() {
        return CalciteQueryEngine.class;
    }

    @Override
    public void validate(CalciteQueryEngineConfig config) throws QueryEngineException {
        QueryEngineFactory.super.validate(config);
    }
}

