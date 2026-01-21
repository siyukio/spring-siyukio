package io.github.siyukio.tools.entity;

/**
 * @author Bugee
 */
public interface EntityConstants {

    String INDEX_SUFFIX = "idx";

    String UNIQUE_INDEX_SUFFIX = "uidx";

    /**
     * Column name for storing record-specific encryption salt.
     * <p>
     * Used as the second half of the encryption key combined with system key.
     */
    String SALT_COLUMN = "salt";

    String ERROR_FILED_UNSUPPORTED_FORMAT = "Entity: '%s' unsupported field type: '%s'.";

    String ERROR_KEY_IS_NULL_FORMAT = "Entity: '%s' must not be null";

    String ERROR_COLUMNS_IS_EMPTY_FORMAT = "Entity: '%s' must not be empty";

    /**
     * Error message format for missing salt column in encrypted entity.
     * <p>
     * Thrown when an entity has encrypted fields but no salt column defined.
     */
    String ERROR_SALT_COLUMN_MISSING_FORMAT = "Entity: '%s' has encrypted fields but missing salt column '%s'.";

    /**
     * Error message format for missing encryption key.
     * <p>
     * Thrown when encryption is required but the encryption key is not configured.
     */
    String ERROR_ENCRYPTION_KEY_MISSING = "Encryption key is not configured. Please set the encryption key in data source properties.";

    /**
     * Error message format for missing createdAtTs field in partitioned entity.
     * <p>
     * Thrown when an entity is configured with partitioning but lacks the required timestamp field.
     */
    String ERROR_PARTITION_TIMESTAMP_FIELD_MISSING_FORMAT = "Entity: '%s' with partition '%s' must include 'createdAtTs' field.";

    /**
     * Field name for creation timestamp (milliseconds).
     */
    String CREATED_AT_TS_FIELD = "createdAtTs";

    /**
     * Field name for creation date-time.
     */
    String CREATED_AT_FIELD = "createdAt";

    /**
     * Field name for update timestamp (milliseconds).
     */
    String UPDATED_AT_TS_FIELD = "updatedAtTs";

    /**
     * Field name for update date-time.
     */
    String UPDATED_AT_FIELD = "updatedAt";
}
