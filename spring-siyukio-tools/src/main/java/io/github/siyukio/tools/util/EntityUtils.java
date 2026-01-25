package io.github.siyukio.tools.util;

import io.github.siyukio.tools.entity.ColumnType;
import io.github.siyukio.tools.entity.EntityConstants;
import io.github.siyukio.tools.entity.definition.EntityDefinition;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

/**
 * @author Bugee
 */
public abstract class EntityUtils {

    public static String camelToSnake(String input) {
        StringBuilder result = new StringBuilder();
        char[] charArray = input.toCharArray();
        for (char c : charArray) {
            if (c >= 'A' && c <= 'Z') {
                c = (char) (c + 32);
                if (result.isEmpty()) {
                    result.append(c);
                } else {
                    result.append('_').append(c);
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static String removeSuffix(String input) {
        if (input.endsWith("Entity")) {
            input = input.substring(0, input.length() - "Entity".length());
        }
        return input;
    }

    public static String getTableName(Class<?> clazz) {
        String tableName = removeSuffix(clazz.getSimpleName());
        return camelToSnake(tableName);
    }

    public static String getKeyInfo(Class<?> clazz) {
        return removeSuffix(clazz.getSimpleName());
    }

    public static String snakeToCamel(String input) {
        if (input == null || input.isEmpty()) return input;

        StringBuilder result = new StringBuilder();
        boolean upperNext = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '_') {
                upperNext = true;
            } else {
                if (upperNext) {
                    result.append(Character.toUpperCase(c));
                    upperNext = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }

        return result.toString();
    }

    public static ColumnType getKeyType(RecordComponent recordComponent) {
        Class<?> type = recordComponent.getType();
        if (int.class == type || Integer.class == type) {
            return ColumnType.INT;
        } else if (long.class == type || Long.class == type) {
            return ColumnType.BIGINT;
        } else if (String.class == type) {
            return ColumnType.TEXT;
        } else {
            String error = String.format(EntityConstants.ERROR_FILED_UNSUPPORTED_FORMAT, "Key", recordComponent.getType());
            throw new IllegalArgumentException(error);
        }
    }

    public static boolean isCustomClass(Class<?> type) {
        // 1. JDK classes do not have a ClassLoader.
        if (type.getClassLoader() == null) {
            return false;
        }

        // 2. Filter common framework packages.
        String name = type.getName();
        if (name.startsWith("java.") || name.startsWith("javax.") ||
                name.startsWith("sun.") || name.startsWith("com.sun.") ||
                name.startsWith("org.springframework.") ||
                name.startsWith("org.apache.") ||
                name.startsWith("jakarta.") ||
                name.startsWith("com.fasterxml.")) {
            return false;
        }

        return true;
    }

    public static ColumnType getColumnType(RecordComponent recordComponent) {
        Class<?> type = recordComponent.getType();
        if (boolean.class == type || Boolean.class == type) {
            return ColumnType.BOOLEAN;
        } else if (int.class == type || Integer.class == type) {
            return ColumnType.INT;
        } else if (long.class == type || Long.class == type) {
            return ColumnType.BIGINT;
        } else if (double.class == type || Double.class == type) {
            return ColumnType.DOUBLE;
        } else if (LocalDateTime.class == type) {
            return ColumnType.DATETIME;
        } else if (String.class == type || type.isEnum()) {
            return ColumnType.TEXT;
        } else if (JSONArray.class == type) {
            return ColumnType.JSON_ARRAY;
        } else if (Collection.class.isAssignableFrom(type)) {
            return ColumnType.JSON_ARRAY;
        } else if (type.isArray()) {
            return ColumnType.JSON_ARRAY;
        } else if (JSONObject.class == type) {
            return ColumnType.JSON_OBJECT;
        } else if (isCustomClass(type)) {
            return ColumnType.JSON_OBJECT;
        } else {
            String error = String.format(EntityConstants.ERROR_FILED_UNSUPPORTED_FORMAT, "Column", recordComponent.getType());
            throw new IllegalArgumentException(error);
        }
    }

    public static void isSafe(String identifier) {
        // Only letters, numbers, and underscores are allowed.
        if (!identifier.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("Invalid entity identifier: " + identifier);
        }
    }

    private static LocalDateTime getPartitionEndDateTime(EntityDefinition.Partition partition, LocalDateTime startDateTime) {
        return switch (partition) {
            case YEAR -> startDateTime.plusYears(1);
            case MONTH -> startDateTime.plusMonths(1);
            case DAY -> startDateTime.plusDays(1);
            case HOUR -> startDateTime.plusHours(1);
            default -> throw new IllegalArgumentException("Unsupported partition type: " + partition);
        };
    }

    private static String getPartitionSuffix(EntityDefinition.Partition partition, LocalDateTime startDateTime) {
        return switch (partition) {
            case YEAR -> startDateTime.format(DateTimeFormatter.ofPattern("yyyy"));
            case MONTH -> startDateTime.format(DateTimeFormatter.ofPattern("yyyyMM"));
            case DAY -> startDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            case HOUR -> startDateTime.format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
            default -> throw new IllegalArgumentException("Unsupported partition type: " + partition);
        };
    }

    /**
     * Generate current partition table name based on main table name and partition type.
     *
     * @param entityDefinition the entity definition
     * @return the current partition table name
     */
    public static PartitionTable getCurrentPartitionTable(EntityDefinition entityDefinition) {
        long timestamp = System.currentTimeMillis();
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zone);
        EntityDefinition.Partition partition = entityDefinition.partition();
        LocalDateTime startDateTime = switch (partition) {
            case YEAR -> dateTime.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case MONTH -> dateTime.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case DAY -> dateTime.withHour(0).withMinute(0).withSecond(0).withNano(0);
            case HOUR -> dateTime.withMinute(0).withSecond(0).withNano(0);
            default -> throw new IllegalArgumentException("Unsupported partition type: " + partition);
        };

        LocalDateTime endDateTime = getPartitionEndDateTime(partition, startDateTime);

        long from = startDateTime.atZone(zone).toInstant().toEpochMilli();
        long to = endDateTime.atZone(zone).toInstant().toEpochMilli();

        String suffix = getPartitionSuffix(partition, startDateTime);

        return new PartitionTable(
                entityDefinition.table() + "_" + suffix,
                from, to);
    }

    public static PartitionTable getNextPartitionTable(EntityDefinition entityDefinition) {
        PartitionTable currentPartitionTable = getCurrentPartitionTable(entityDefinition);

        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentPartitionTable.to()), zone);

        LocalDateTime endDateTime = getPartitionEndDateTime(entityDefinition.partition(), startDateTime);

        long from = startDateTime.atZone(zone).toInstant().toEpochMilli();
        long to = endDateTime.atZone(zone).toInstant().toEpochMilli();

        String suffix = getPartitionSuffix(entityDefinition.partition(), startDateTime);

        return new PartitionTable(
                entityDefinition.table() + "_" + suffix,
                from, to);
    }

    public record PartitionTable(
            String tableName,
            long from,
            long to
    ) {
    }
}
