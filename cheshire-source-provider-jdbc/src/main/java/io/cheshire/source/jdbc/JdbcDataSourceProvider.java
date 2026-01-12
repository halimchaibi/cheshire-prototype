package io.cheshire.source.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.cheshire.spi.source.SourceProvider;
import io.cheshire.spi.source.SourceProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.sql.Date;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JDBC-based implementation of {@link SourceProvider} with connection pooling via HikariCP.
 * <p>
 * <strong>Purpose:</strong>
 * <p>
 * Provides access to relational databases via JDBC, handling connection pooling,
 * query execution, parameter binding, and result set extraction.
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *   <li><strong>Connection Pooling:</strong> Uses HikariCP for efficient connection reuse</li>
 *   <li><strong>Named Parameters:</strong> Supports `:paramName` syntax converted to JDBC `?` placeholders</li>
 *   <li><strong>Type Conversion:</strong> Automatic conversion of Java time types to SQL types</li>
 *   <li><strong>Result Extraction:</strong> Converts ResultSet to List&lt;Map&lt;String, Object&gt;&gt;</li>
 *   <li><strong>Update Operations:</strong> Returns affected row count for DML statements</li>
 * </ul>
 * <p>
 * <strong>Connection Pool Configuration:</strong>
 * <pre>{@code
 * // Optional pool settings (with defaults):
 * maximumPoolSize: 10
 * minimumIdle: 1
 * connectionTimeout: 30000 (30 seconds)
 * idleTimeout: 600000 (10 minutes)
 * maxLifetime: 1800000 (30 minutes)
 * }</pre>
 * <p>
 * <strong>Thread Safety:</strong>
 * <p>
 * This implementation is fully thread-safe. The connection pool handles concurrent access.
 *
 * @see SqlQuery
 * @see SqlQueryResult
 * @see JdbcDataSourceConfig
 * @since 1.1.0
 */
public final class JdbcDataSourceProvider implements SourceProvider<SqlQuery, SqlQueryResult> {

    private static final Logger log = LoggerFactory.getLogger(JdbcDataSourceProvider.class);
    private static final Pattern NAMED_PARAM_PATTERN = Pattern.compile("(?<!:):([a-zA-Z_][a-zA-Z0-9_]*)");

    //TODO: Make these configurable via JdbcDataSourceConfig
    private static final int DEFAULT_MAX_POOL_SIZE = 10;
    private static final int DEFAULT_MIN_IDLE = 1;
    private static final long DEFAULT_CONNECTION_TIMEOUT = 30_000; // 30 seconds
    private static final long DEFAULT_IDLE_TIMEOUT = 600_000; // 10 minutes
    private static final long DEFAULT_MAX_LIFETIME = 1_800_000; // 30 minutes
    private static final long LEAK_DETECTION_THRESHOLD_MS = 30_000; // 30 seconds

    private final JdbcDataSourceConfig config;
    private final Map<String, Object> poolConfig;
    private final AtomicBoolean isOpen = new AtomicBoolean(false);
    private volatile HikariDataSource dataSource;

    public JdbcDataSourceProvider(JdbcDataSourceConfig config) {
        this.config = Objects.requireNonNull(config);
        this.poolConfig = (Map<String, Object>)this.config.additionalProperties().get("pool");
    }

    @Override
    public SqlQueryResult execute(SqlQuery query) throws SourceProviderException {
        ensureOpen();

        String sql = Objects.requireNonNull(query.sql(), "SQL is null").trim();
        ParsedQuery parsed = parseSql(sql);

        if (log.isDebugEnabled()) {
            log.debug("Executing SQL: {}", parsed.sql());
            log.debug("Parameters: {}", query.params());
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(parsed.sql())) {

            bindParameters(pstmt, parsed.paramMap(), query.params());

            boolean isResultSet = pstmt.execute();

            if (isResultSet) {
                try (ResultSet rs = pstmt.getResultSet()) {
                    List<Map<String, Object>> results = extractResults(rs);
                    return SqlQueryResult.of(results);
                }
            } else {
                int count = pstmt.getUpdateCount();
                log.debug("Update query affected {} rows", count);
                return SqlQueryResult.of(List.of(Map.of("updated", count)));
            }
        } catch (SQLException e) {
            throw new SourceProviderException("Database error: " + e.getMessage(), e);
        }
    }

    private void bindParameters(PreparedStatement pstmt, Map<String, List<Integer>> paramMap, Map<String, Object> params) throws SQLException {
        if (params == null) return;

        for (var entry : paramMap.entrySet()) {
            String paramName = entry.getKey();
            Object value = params.get(paramName);

            switch (value) {
                case null -> {
                    for (int index : entry.getValue()) {
                        pstmt.setNull(index, Types.OTHER);
                    }
                    continue;
                }

                case java.time.LocalDateTime localDateTime -> value = Timestamp.valueOf(localDateTime);
                case java.time.LocalDate localDate -> value = Date.valueOf(localDate);
                case OffsetDateTime odt -> value = Timestamp.from(odt.toInstant());
                default -> {
                    log.debug("No binding found for value type: {}", value);
                }
            }

            for (int index : entry.getValue()) {
                pstmt.setObject(index, value);
            }

            log.debug("Bound parameter '{}' = '{}' at positions {}", paramName, value, entry.getValue());

        }
    }

    private List<Map<String, Object>> extractResults(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        List<Map<String, Object>> results = new ArrayList<>();

        String[] columnNames = new String[cols];
        for (int i = 1; i <= cols; i++) {
            columnNames[i - 1] = meta.getColumnLabel(i);
        }
        rs.getFetchSize();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 0; i < cols; i++) {
                Object value = rs.getObject(i + 1);
                if (value instanceof Timestamp) {
                    value = ((Timestamp) value).toLocalDateTime();
                } else if (value instanceof Date) {
                    value = ((Date) value).toLocalDate();
                } else if (value instanceof java.sql.Time) {
                    value = ((java.sql.Time) value).toLocalTime();
                }
                row.put(columnNames[i], value);
            }
            results.add(row);
        }
        return results;
    }

    private ParsedQuery parseSql(String sql) {
        StringBuilder sb = new StringBuilder();
        Map<String, List<Integer>> paramMap = new HashMap<>();
        Matcher m = NAMED_PARAM_PATTERN.matcher(sql);
        int index = 1, last = 0;

        while (m.find()) {
            sb.append(sql, last, m.start()).append("?");
            String paramName = m.group(1);
            paramMap.computeIfAbsent(paramName, k -> new ArrayList<>()).add(index++);
            last = m.end();
        }
        sb.append(sql.substring(last));

        return new ParsedQuery(sb.toString(), Map.copyOf(paramMap));
    }

    private HikariDataSource createDataSource() throws SourceProviderException {
        String jdbcUrl = config.require("jdbcUrl");
        String driverClassName = config.require("driverClassName");
        String username = config.get("username");
        String password = config.get("password");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setDriverClassName(driverClassName);

        if (username != null) {
            hikariConfig.setUsername(username);
        }
        if (password != null) {
            hikariConfig.setPassword(password);
        }

        hikariConfig.setMaximumPoolSize(
                getFromPoolConfig("maximumPoolSize", DEFAULT_MAX_POOL_SIZE, v -> Integer.parseInt(v.toString()))
        );

        hikariConfig.setMinimumIdle(
                getFromPoolConfig("minimumIdle", DEFAULT_MIN_IDLE, v -> Integer.parseInt(v.toString()))
        );

        hikariConfig.setConnectionTimeout(
                getFromPoolConfig("connectionTimeout", DEFAULT_CONNECTION_TIMEOUT, v ->  Long.parseLong(v.toString()))
        );

        hikariConfig.setIdleTimeout(
                getFromPoolConfig("idleTimeout", DEFAULT_IDLE_TIMEOUT, v ->  Long.parseLong(v.toString()))
        );

        hikariConfig.setPoolName(
                getFromPoolConfig("poolName", config.name() + "-pool", Object::toString)
        );

        hikariConfig.setLeakDetectionThreshold(LEAK_DETECTION_THRESHOLD_MS);

         hikariConfig.setMaxLifetime(
                getFromPoolConfig("maxLifetime", DEFAULT_MAX_LIFETIME, v ->  Long.parseLong(v.toString()))
        );

        try {
            return new HikariDataSource(hikariConfig);
        } catch (Exception e) {
            throw new SourceProviderException("Failed to create connection pool: " + e.getMessage(), e);
        }
    }

    /**
     * Helper to check the map for a key, otherwise return the default.
     */
    private <T> T getFromPoolConfig(String key, T defaultValue, java.util.function.Function<Object, T> parser) {
        if (poolConfig == null || !poolConfig.containsKey(key)) {
            return defaultValue;
        }
        Object value = poolConfig.get(key);
        if (value == null) return defaultValue;
        if (defaultValue.getClass().isInstance(value)) {
            return (T) value;
        }
        try {
            return parser.apply(value);
        } catch (Exception e) {
            log.warn("Failed to parse pool config '{}': {}. Falling back to default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    private void ensureOpen() throws SourceProviderException {
        if (!isOpen.get()) {
            synchronized (this) {
                if (!isOpen.get()) {
                    open();
                }
            }
        }
    }

    @Override
    public void open() throws SourceProviderException {
        if (isOpen.get()) return;

        log.info("Opening JdbcDataSourceProvider '{}' with connection pool", config.name());
        long startTime = System.currentTimeMillis();

        try {
            dataSource = createDataSource();

            isOpen.set(true);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Source '{}' validated and opened with connection pool in {}ms", config.name(), duration);
            log.info("Pool stats - Max: {}, MinIdle: {}",
                    dataSource.getMaximumPoolSize(),
                    dataSource.getMinimumIdle());
        } catch (Exception e) {
            if (dataSource != null) {
                dataSource.close();
                dataSource = null;
            }
            throw new SourceProviderException("Unexpected error during source initialization", e);
        }
    }

    @Override
    public boolean isOpen() {
        return isOpen.get() && dataSource != null && !dataSource.isClosed();
    }

    @Override
    public JdbcDataSourceConfig config() {
        return config;
    }

    @Override
    public void close() {
        if (isOpen.compareAndSet(true, false)) {
            log.info("Closing JdbcDataSourceProvider '{}' and shutting down connection pool...", config.name());

            if (dataSource != null) {
                try {
                    log.info("Pool stats before shutdown - Active: {}, Idle: {}, Total: {}, Waiting: {}",
                            dataSource.getHikariPoolMXBean().getActiveConnections(),
                            dataSource.getHikariPoolMXBean().getIdleConnections(),
                            dataSource.getHikariPoolMXBean().getTotalConnections(),
                            dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());

                    dataSource.close();
                    log.info("Connection pool shut down successfully");
                } catch (Exception e) {
                    log.error("Error closing connection pool", e);
                } finally {
                    dataSource = null;
                }
            }

            log.info("JdbcDataSourceProvider '{}' is now closed.", config.name());
        } else {
            log.debug("JdbcDataSourceProvider '{}' was already closed.", config.name());
        }
    }

    /**
     * Executes a batch operation using a single connection from the pool.
     * This is more efficient than executing queries individually.
     *
     * @param query The SQL query template
     * @param batchParams List of parameter maps for each batch execution
     * @return Result containing total affected rows and batch results
     * @throws SourceProviderException if execution fails
     */
    public SqlQueryResult executeBatch(SqlQuery query, List<Map<String, Object>> batchParams) throws SourceProviderException {
        ensureOpen();

        String sql = Objects.requireNonNull(query.sql(), "SQL is null").trim();
        ParsedQuery parsed = parseSql(sql);

        if (log.isDebugEnabled()) {
            log.debug("Executing batch SQL: {}", parsed.sql());
            log.debug("Batch size: {}", batchParams.size());
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(parsed.sql())) {

            for (Map<String, Object> params : batchParams) {
                bindParameters(pstmt, parsed.paramMap(), params);
                pstmt.addBatch();
            }

            int[] results = pstmt.executeBatch();
            int totalAffected = Arrays.stream(results).sum();

            log.debug("Batch execution affected {} total rows", totalAffected);
            return SqlQueryResult.of(List.of(Map.of(
                    "totalAffected", totalAffected,
                    "batchResults", results
            )));
        } catch (SQLException e) {
            throw new SourceProviderException("Database error during batch execution: " + e.getMessage(), e);
        }
    }

    /**
     * Returns connection pool statistics for monitoring.
     *
     * @return Map containing pool statistics
     */
    public Map<String, Object> getPoolStats() {
        if (dataSource == null || !isOpen.get()) {
            return Map.of("status", "closed");
        }

        try {
            return Map.of(
                    "active", dataSource.getHikariPoolMXBean().getActiveConnections(),
                    "idle", dataSource.getHikariPoolMXBean().getIdleConnections(),
                    "total", dataSource.getHikariPoolMXBean().getTotalConnections(),
                    "waiting", dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection(),
                    "maxPoolSize", dataSource.getMaximumPoolSize(),
                    "minIdle", dataSource.getMinimumIdle()
            );
        } catch (Exception e) {
            log.error("Error retrieving pool stats", e);
            return Map.of("error", e.getMessage());
        }
    }

    private record ParsedQuery(String sql, Map<String, List<Integer>> paramMap) {
    }
}
