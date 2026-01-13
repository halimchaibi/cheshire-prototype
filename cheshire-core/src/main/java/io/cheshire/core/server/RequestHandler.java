/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core.server;

/**
 * <h1>RequestHandler</h1> *
 * <p>
 * A <b>Functional Interface</b> representing the core execution unit for a specific request. It defines the contract
 * for taking a validated input and producing a processed output within the Cheshire ecosystem.
 * </p>
 * * *
 * <h3>Architectural Role</h3>
 * <p>
 * The {@code RequestHandler} is typically invoked by the {@code CheshireDispatcher} after the {@code ProtocolAdapter}
 * has successfully mapped an incoming message into the internal domain model. It serves as the primary extension point
 * for developers to inject custom business logic or tool behaviors.
 * </p>
 * *
 * <h3>Design Benefits</h3>
 * <ul>
 * <li><b>Functional Versatility:</b> Can be implemented via standard classes, anonymous inner classes, or modern Java
 * <b>Lambdas</b>.</li>
 * <li><b>Type Safety:</b> Uses generics to ensure that the input ({@code I}) and output ({@code O}) remain consistent
 * throughout the execution chain.</li>
 * <li><b>Checked Error Boundary:</b> Forces the implementation to handle or propagate logic failures through the
 * specialized {@link RequestHandlerException}.</li>
 * </ul>
 * *
 * <h3>Usage Examples</h3>
 *
 * <pre>{@code
 * // Implementation using a Lambda expression
 * RequestHandler<String, Integer> wordCounter = (request) -> {
 * if (request == null) throw new RequestHandlerException("Input cannot be null");
 * return request.length();
 * };
 * * // Implementation using a Method Reference
 * RequestHandler<RequestEnvelope, ResponseEntity> handler = myService::process;
 * }</pre>
 *
 * * @param <I> The type of the input request.
 *
 * @param <O>
 *            The type of the produced response.
 * @author Cheshire Framework
 * @since 1.0.0
 */
@FunctionalInterface
public interface RequestHandler<I, O> {

    /**
     * Executes the business logic associated with the request.
     *
     * @param request
     *            The inbound data or envelope to be processed.
     * @return The result of the processing logic.
     * @throws RequestHandlerException
     *             if the logic encounters a functional or operational failure (e.g., database timeout, validation
     *             error).
     */
    O handle(I request) throws RequestHandlerException;
}
