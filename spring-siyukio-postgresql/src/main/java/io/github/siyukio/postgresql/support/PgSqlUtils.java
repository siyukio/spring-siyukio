package io.github.siyukio.postgresql.support;

/**
 * @author Bugee
 */
public class PgSqlUtils {

    public static String safeIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty.");
        }
        // Only letters, numbers, and underscores are allowed.
        if (!identifier.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("Invalid identifier: " + identifier);
        }
        // Add double quotes to PostgreSQL identifiers to prevent case sensitivity issues and SQL injection.
        return "\"" + identifier + "\"";
    }

    public static String createSchemaIfNotExists(String schemaName) {
        String safeSchema = safeIdentifier(schemaName);
        return "CREATE SCHEMA IF NOT EXISTS " + safeSchema;
    }
}
