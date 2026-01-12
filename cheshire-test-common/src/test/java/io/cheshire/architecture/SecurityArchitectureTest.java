package io.cheshire.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameContaining;
import static com.tngtech.archunit.lang.conditions.ArchConditions.beFinal;
import static com.tngtech.archunit.lang.conditions.ArchConditions.haveFullyQualifiedName;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Architecture tests for security-related patterns and practices.
 * <p>
 * <strong>Security Principles Enforced:</strong>
 * <ul>
 *   <li><strong>No hardcoded credentials:</strong> No password/key constants in code</li>
 *   <li><strong>Secure configuration:</strong> Sensitive data in config, not code</li>
 *   <li><strong>SQL injection prevention:</strong> No string concatenation for SQL</li>
 *   <li><strong>Immutable security contexts:</strong> Security objects immutable</li>
 * </ul>
 *
 * @since 1.0.0
 */
@AnalyzeClasses(
        packages = "io.cheshire",
        importOptions = {ImportOption.DoNotIncludeTests.class}
)
public class SecurityArchitectureTest {

    // ============================================
    // SQL Injection Prevention
    // ============================================

    /**
     * Ensures SQL queries use parameter binding, not string concatenation.
     * <p>
     * <strong>Security Risk:</strong> String concatenation in SQL leads to SQL injection:
     * <pre>{@code
     * // BAD - Vulnerable to SQL injection ✗
     * String sql = "SELECT * FROM users WHERE id = " + userId;
     *
     * // GOOD - Parameterized query ✓
     * String sql = "SELECT * FROM users WHERE id = :userId";
     * }</pre>
     * <p>
     * <strong>Cheshire Approach:</strong> All SQL generation uses DSL_QUERY templates
     * with automatic parameter binding via {@code SqlTemplateQueryBuilder}.
     */
    @ArchTest
    static final ArchRule query_Builders_Should_Not_Use_String_Concatenation =
            noClasses().that().resideInAPackage("..query..")
                    .should().dependOnClassesThat().haveFullyQualifiedName("java.lang.StringBuilder")
                    .orShould().dependOnClassesThat().haveFullyQualifiedName("java.lang.StringBuffer")
                    .because("Query builders should use parameter binding, not string concatenation, to prevent SQL injection");

    // ============================================
    // Credential Management
    // ============================================

    /**
     * Prevents hardcoded passwords in code.
     * <p>
     * <strong>Security Best Practice:</strong> Passwords, API keys, and tokens should
     * be externalized in configuration files or environment variables, never hardcoded.
     * <p>
     * <strong>Detection:</strong> Looks for fields with names suggesting credentials.
     */
    @ArchTest
    static final ArchRule no_Hardcoded_Passwords =
            fields().that().haveName("password")
                    .or().haveName("PASSWORD")
                    .or().haveName("apiKey")
                    .or().haveName("API_KEY")
                    .or().haveName("secret")
                    .or().haveName("SECRET")
                    .should().notBeStatic()
                    .orShould().notBeFinal()
                    .because("Credentials should not be hardcoded as static final fields");

    // ============================================
    // Configuration Security
    // ============================================

    /**
     * Ensures security-related configuration uses secure types.
     * <p>
     * <strong>Pattern:</strong> Security configs should be records for immutability.
     */
    @ArchTest
    static final ArchRule security_Config_Should_Be_Immutable =
            classes().that().resideInAPackage("..security..")
                    .and().haveSimpleNameEndingWith("Config")
                    .should().beRecords()
                    .because("Security configuration should be immutable records");

    // ============================================
    // Authentication & Authorization
    // ============================================

    /**
     * Ensures authentication/authorization classes are properly secured.
     * <p>
     * <strong>Pattern:</strong> Auth-related classes should be final to prevent
     * extension attacks.
     */
    @ArchTest
    static final ArchRule auth_Classes_Should_Be_Final =
            classes().that().resideInAPackage("..security..")
                    .and().haveSimpleNameContaining("Auth")
                    .and().areNotInterfaces()
                    .and().areNotEnums()
                    .should(beFinal())
                    .as("Authentication/Authorization classes should be final to prevent subclass attacks");

    // ============================================
    // Secure Data Handling
    // ============================================

    /**
     * Ensures sensitive data classes don't expose mutable collections.
     * <p>
     * <strong>Security Risk:</strong> Mutable collections allow external modification.
     * <p>
     * <strong>Best Practice:</strong> Use defensive copies or immutable collections.
     */
    @ArchTest
    static final ArchRule sensitive_Data_Should_Use_Immutable_Collections =
            noClasses()
                    .that().resideInAPackage("..security..")
                    .and().haveSimpleNameContaining("Credential")
                    .or().haveSimpleNameContaining("Token")
                    .should().accessClassesThat().haveFullyQualifiedName("java.util.ArrayList")
                    .orShould().accessClassesThat().haveFullyQualifiedName("java.util.HashMap")
                    .as("Sensitive data classes should use immutable collections for security");
}
