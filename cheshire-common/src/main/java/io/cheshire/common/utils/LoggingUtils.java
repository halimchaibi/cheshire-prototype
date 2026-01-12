package io.cheshire.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimal logging utility.
 */
public class LoggingUtils {

    /**
     * Creates a logger for the given class.
     *
     * @param clazz the clazz
     * @return the logger
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
}
