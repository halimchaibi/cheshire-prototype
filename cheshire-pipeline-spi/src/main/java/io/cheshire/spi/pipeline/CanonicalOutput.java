/*-
 * #%L
 * Cheshire :: Pipeline :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.pipeline;

//TODO: refactcor to handle bigger outputs better

/**
 * public sealed interface CanonicalOutput permits MaterializedOutput, OffHeapOutput, StreamingOutput {
 * <p>
 * Map<String, Object> getMetadata(); OutputType getType();
 * <p>
 * enum OutputType { MATERIALIZED, STREAMING, OFF_HEAP } }
 * <p>
 * ========================================== Current supported implementations
 * <p>
 * public record MaterializedOutput( Map<String, Object> data, Map<String, Object> metadata ) implements
 * CanonicalOutput<MaterializedOutput> { }
 * <p>
 * <p>
 * ==========================================
 * <p>
 * <p>
 * public record OffHeapOutput( OffHeapHandle handle, Map<String, Object> metadata ) implements CanonicalOutput { }
 * <p>
 * <p>
 * public interface OffHeapHandle extends AutoCloseable {
 * <p>
 * long sizeBytes();
 * <p>
 * <p>
 * Provides a zero-copy view suitable for transport. No allocation, no deserialization.
 * <p>
 * Iterator<ByteBuffer> buffers(); Explicit manager control. Must be idempotent.
 *
 * @Override void close(); }
 *           <p>
 *           ==========================================
 *           <p>
 *           public record StreamingOutput( Iterator<Map<String, Object>> dataStream, Map<String, Object> metadata )
 *           implements CanonicalOutput<StreamingOutput> { }
 */
public non-sealed interface CanonicalOutput<S extends CanonicalOutput<S>> extends Canonical<S> {}
