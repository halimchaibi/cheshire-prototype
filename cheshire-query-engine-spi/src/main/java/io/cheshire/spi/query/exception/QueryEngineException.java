package io.cheshire.spi.query.exception;

public class QueryEngineException extends Exception {

    public QueryEngineException(String message) {
        super(message);
    }

    public QueryEngineException(String message, Throwable cause) {
        super(message, cause);
    }

}
