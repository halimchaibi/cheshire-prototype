package io.cheshire.source.jdbc;

import io.cheshire.spi.source.SourceProvider;
import io.cheshire.spi.source.SourceProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JDBC data source provider implementation.
 *
 *
 *
 * <p>This provider supports thread-safe operations for both data retrieval and modification.
 * <p>
 * Key features:
 *
 * <ul>
 *
 * <li>Named parameter parsing and binding (e.g., :paramName)</li>
 *
 * <li>Case-insensitive result maps</li>
 *
 * <li>Automatic distinction between SELECT and UPDATE/INSERT/DELETE statements</li>
 *
 * <li>Connection management and validation</li>
 *
 * </ul>
 *
 * </p>
 *
 *
 *
 * <p><strong>Thread Safety:</strong> This implementation is thread-safe. Multiple threads
 * <p>
 * can safely execute queries concurrently. The {@link #open()} and {@link #close()} methods
 * <p>
 * are synchronized to ensure proper manager management.</p>
 *
 *
 *
 * <p><strong>Resource Management:</strong> This class implements {@link AutoCloseable}.
 * <p>
 * Always call {@link #close()} or use try-with-resources to ensure proper cleanup of
 * <p>
 * database connections.</p>
 *
 *
 *
 * <p><strong>Named Parameters:</strong> Supports named parameters in SQL queries using
 * <p>
 * the format {@code :paramName}. Parameters are bound by name, allowing the same parameter
 * <p>
 * to be used multiple times in a query.</p>
 *
 *
 *
 * <p><strong>Example Usage:</strong></p>
 *
 * <pre>{@code
 *
 * JdbcDataSourceConfig config = ...;
 *
 * JdbcDataSourceProviderBack provider = new JdbcDataSourceProviderBack(config);
 *
 * try {
 *
 * provider.open();
 *
 * List<Map<String, Object>> results = provider.execute(
 *
 * "SELECT * FROM users WHERE id = :id AND status = :status",
 *
 * Map.of("id", 123, "status", "active")
 *
 * );
 *
 * // Process results...
 *
 * } finally {
 *
 * provider.close();
 *
 * }
 *
 * }</pre>
 *
 * @author Cheshire Framework
 * @since 1.0.0
 */

public final class JdbcDataSourceProviderBack implements SourceProvider<SqlQuery, SqlQueryResult> {

    private static final Logger log = LoggerFactory.getLogger(JdbcDataSourceProviderBack.class);

    private static final Pattern NAMED_PARAM_PATTERN = Pattern.compile("(?<!:):([a-zA-Z_][a-zA-Z0-9_]*)");

    private final JdbcDataSourceConfig config;

    private volatile boolean open = false;

    /**
     * Creates a new JdbcDataSourceProviderBack with the specified configuration.
     *
     * @param config the JDBC data source configuration, must not be null
     * @throws NullPointerException if config is null
     */

    public JdbcDataSourceProviderBack(JdbcDataSourceConfig config) {

        this.config = Objects.requireNonNull(config, "config cannot be null");

        log.debug("Created JdbcDataSourceProviderBack for source '{}'", config.name());

    }

    @Override

    public synchronized void open() throws SourceProviderException {

        if (open) {

            log.debug("JdbcDataSourceProviderBack '{}' is already open", config.name());

            return;

        }

        log.info("Opening JdbcDataSourceProviderBack '{}'", config.name());

        long startTime = System.currentTimeMillis();

        try {

            String driverClassName = config.require("driverClassName");

            Class.forName(driverClassName);

            log.debug("Loaded JDBC driver: {}", driverClassName);

            try (Connection conn = getConnection()) {

                if (conn.isValid(5)) {

                    this.open = true;

                    long duration = System.currentTimeMillis() - startTime;

                    log.info("JdbcDataSourceProviderBack '{}' opened successfully in {}ms (url: {})", config.name(), duration, config.require("jdbcUrl"));

                } else {

                    throw new SourceProviderException(String.format("Connection validation failed for source '%s'", config.name()));

                }

            }

        } catch (ClassNotFoundException e) {

            String errorMsg = String.format("JDBC driver class not found for source '%s': %s", config.name(), e.getMessage());

            log.error(errorMsg, e);

            throw new SourceProviderException(errorMsg, e);

        } catch (SQLException e) {

            String errorMsg = String.format("Failed to establish connection for source '%s' (url: %s): %s", config.name(), config.require("jdbcUrl"), e.getMessage());

            log.error(errorMsg, e);

            throw new SourceProviderException(errorMsg, e);

        } catch (SourceProviderException e) {

// Re-throw as-is

            throw e;

        } catch (Exception e) {

            String errorMsg = String.format("Unexpected error opening JdbcDataSourceProviderBack '%s': %s", config.name(), e.getMessage());

            log.error(errorMsg, e);

            throw new SourceProviderException(errorMsg, e);

        }

    }

    @Override

    public boolean isOpen() {

        return open;

    }

    @Override

    public JdbcDataSourceConfig config() {

        return config;

    }

    /**
     * Executes a SQL statement and returns results.
     *
     *
     *
     * <p>Automatically distinguishes between SELECT queries (returns rows) and
     * <p>
     * UPDATE/INSERT/DELETE statements (returns update count).</p>
     *
     * @param query the SQL statement to execute
     * @return for SELECT: list of result rows; for UPDATE/INSERT/DELETE: list with update count
     * @throws SourceProviderException if execution fails or provider is not open
     */

    @Override

    public SqlQueryResult execute(SqlQuery query) throws SourceProviderException {

        if (query.sql() == null || query.sql().isBlank()) {

            throw new SourceProviderException("SQL statement cannot be null or empty");

        }

        ensureOpen();

        String trimmedSql = query.sql().trim();

        String upperSql = trimmedSql.toUpperCase();

// Determine query type and execute accordingly

        if (upperSql.startsWith("SELECT")) {

            log.debug("Executing SELECT query on source '{}': {}", config.name(), query.sql());

            return SqlQueryResult.of(query(trimmedSql, query.params()));

        } else if (upperSql.startsWith("INSERT") || upperSql.startsWith("UPDATE") || upperSql.startsWith("DELETE") || upperSql.startsWith("MERGE")) {

            log.debug("Executing DML statement on source '{}': {}", config.name(), query.sql());

            int updated = executeUpdate(trimmedSql, query.params());

            return SqlQueryResult.of(Collections.singletonList(Map.of("updated", updated)));

        } else {

// DDL or other statements

            log.debug("Executing DDL/other statement on source '{}': {}", config.name(), query.sql());

            int updated = executeUpdate(trimmedSql, query.params());

            return SqlQueryResult.of(Collections.singletonList(Map.of("updated", updated)));

        }

    }

    @Override

    public synchronized void close() throws SourceProviderException {

        if (!open) {

            log.debug("JdbcDataSourceProviderBack '{}' is already closed", config.name());

            return;

        }

        log.info("Closing JdbcDataSourceProviderBack '{}'", config.name());

        this.open = false;

        log.info("JdbcDataSourceProviderBack '{}' closed successfully", config.name());

    }

    /**
     * Executes a SELECT query and returns the results.
     *
     * @param sql    the SQL SELECT query
     * @param params named parameters for the query
     * @return list of result rows as maps
     * @throws SourceProviderException if query execution fails
     */

    private List<Map<String, Object>> query(String sql, Map<String, Object> params) throws SourceProviderException {

        ParsedQuery parsed = parseSql(sql);

        long startTime = System.currentTimeMillis();

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(parsed.sql)) {

            bindParameters(pstmt, parsed, params);

            try (ResultSet rs = pstmt.executeQuery()) {

                List<Map<String, Object>> results = extractResults(rs);

                long duration = System.currentTimeMillis() - startTime;

                log.debug("SELECT query executed on source '{}' in {}ms, returned {} rows", config.name(), duration, results.size());

                return results;

            }

        } catch (SQLException e) {

            String errorMsg = String.format("Failed to execute SELECT query on source '%s' for query '%s': %s", config.name(), sql, e.getMessage());

            log.error(errorMsg, e);

            throw new SourceProviderException(errorMsg, e);

        }

    }

    /**
     * Executes an INSERT, UPDATE, DELETE, or DDL statement.
     *
     * @param sql    the SQL statement to execute
     * @param params named parameters for the statement
     * @return the number of rows affected
     * @throws SourceProviderException if statement execution fails
     */

    private int executeUpdate(String sql, Map<String, Object> params) throws SourceProviderException {

        ParsedQuery parsed = parseSql(sql);

        long startTime = System.currentTimeMillis();

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(parsed.sql)) {

            bindParameters(pstmt, parsed, params);

            int updateCount = pstmt.executeUpdate();

            long duration = System.currentTimeMillis() - startTime;

            log.debug("DML/DDL statement executed on source '{}' in {}ms, affected {} rows", config.name(), duration, updateCount);

            return updateCount;

        } catch (SQLException e) {

            String errorMsg = String.format("Failed to execute DML/DDL statement on source '%s' for statement '%s': %s", config.name(), sql, e.getMessage());

            log.error(errorMsg, e);

            throw new SourceProviderException(errorMsg, e);

        }

    }

// --- Private Logic ---

    private Connection getConnection() throws SQLException, SourceProviderException {

        String jdbcUrl = config.require("jdbcUrl");

        String username = config.get("username");

        String password = config.get("password");

        log.trace("Getting connection for source '{}' (url: {})", config.name(), jdbcUrl);

        return DriverManager.getConnection(jdbcUrl, username, password);

    }

    /**
     * Ensures the provider is open and ready for operations.
     *
     * @throws SourceProviderException if the provider is not open
     */

    private void ensureOpen() throws SourceProviderException {

        if (!open) {

            log.debug("Opening JdbcDataSourceProviderBack '{}'", config.name());

            try {

                open();

            } catch (SourceProviderException e) {

                throw new SourceProviderException(String.format("Failed to open JdbcDataSourceProviderBack '%s': %s", config.name(), e.getMessage()), e);

            }

        }

    }

    private void bindParameters(PreparedStatement pstmt, ParsedQuery parsed, Map<String, Object> params) throws SQLException {

        if (params == null) {

            return;

        }

        for (Map.Entry<String, List<Integer>> entry : parsed.paramMap.entrySet()) {

            Object value = params.get(entry.getKey());

            for (int index : entry.getValue()) {

                pstmt.setObject(index, value);

            }

        }

    }

    private List<Map<String, Object>> extractResults(ResultSet rs) throws SQLException {

        ResultSetMetaData meta = rs.getMetaData();

        int columns = meta.getColumnCount();

        List<Map<String, Object>> results = new ArrayList<>();

        while (rs.next()) {

// TreeMap with CASE_INSENSITIVE_ORDER solves "ArtistId" vs "ARTISTID" issues

            Map<String, Object> row = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            for (int i = 1; i <= columns; i++) {

                row.put(meta.getColumnLabel(i), rs.getObject(i));

            }

            results.add(row);

        }

        return results;

    }

    private ParsedQuery parseSql(String sql) {

        StringBuilder sb = new StringBuilder();

        Map<String, List<Integer>> paramMap = new HashMap<>();

        Matcher m = NAMED_PARAM_PATTERN.matcher(sql);

        int index = 1;

        int last = 0;

        while (m.find()) {

            sb.append(sql, last, m.start()).append("?");

            paramMap.computeIfAbsent(m.group(1), k -> new ArrayList<>()).add(index++);

            last = m.end();

        }

        sb.append(sql.substring(last));

        return new ParsedQuery(sb.toString(), paramMap);

    }

    /**
     * Represents a parsed SQL query with parameter mappings.
     *
     *
     *
     * <p>After parsing, named parameters (e.g., :paramName) are replaced with
     * <p>
     * positional placeholders (?) and a mapping is created from parameter names
     * <p>
     * to their positions in the prepared statement.</p>
     *
     * @param sql      the SQL with named parameters replaced by ?
     * @param paramMap mapping from parameter names to their positions in the SQL
     */

    private record ParsedQuery(String sql, Map<String, List<Integer>> paramMap) {

        /**
         * Creates a new ParsedQuery.
         *
         * @param sql      the SQL with positional parameters
         * @param paramMap the parameter name to position mapping
         */

        public ParsedQuery {

            Objects.requireNonNull(sql, "SQL cannot be null");

            Objects.requireNonNull(paramMap, "Parameter map cannot be null");

        }

    }

}
