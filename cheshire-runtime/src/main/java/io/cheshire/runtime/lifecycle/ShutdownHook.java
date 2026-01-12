package io.cheshire.runtime.lifecycle;

import lombok.extern.slf4j.Slf4j;

/**
 * Minimal shutdown hook placeholder.
 */
@Slf4j
public class ShutdownHook extends Thread {

    @Override
    public void run() {
        log.info("Cheshire framework is shutting down...");
    }
}
