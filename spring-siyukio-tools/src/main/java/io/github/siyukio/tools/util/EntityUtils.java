package io.github.siyukio.tools.util;

import io.github.siyukio.tools.entity.ColumnType;
import io.github.siyukio.tools.entity.EntityConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Date;

/**
 * @author Bugee
 */
public class EntityUtils {

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

    public static String getTableName(Class<?> clazz) {
        String tableName = clazz.getName();
        int index = tableName.lastIndexOf(".");
        if (index >= 0) {
            tableName = tableName.substring(index + 1);
        }
        return camelToSnake(tableName);
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

    public static ColumnType getKeyType(Field field) {
        Class<?> type = field.getType();
        if (int.class == type || Integer.class == type) {
            return ColumnType.INT;
        } else if (long.class == type || Long.class == type) {
            return ColumnType.BIGINT;
        } else if (String.class == type) {
            return ColumnType.TEXT;
        } else {
            String error = String.format(EntityConstants.ERROR_FILED_UNSUPPORTED_FORMAT, "Key", field.getType());
            throw new IllegalArgumentException(error);
        }
    }

    public static ColumnType getColumnType(Field field) {
        Class<?> type = field.getType();
        if (boolean.class == type || Boolean.class == type) {
            return ColumnType.BOOLEAN;
        } else if (int.class == type || Integer.class == type) {
            return ColumnType.INT;
        } else if (long.class == type || Long.class == type) {
            return ColumnType.BIGINT;
        } else if (double.class == type || Double.class == type) {
            return ColumnType.DOUBLE;
        } else if (Date.class == type) {
            return ColumnType.DATETIME;
        } else if (String.class == type) {
            return ColumnType.TEXT;
        } else if (JSONObject.class == type) {
            return ColumnType.JSON_OBJECT;
        } else if (JSONArray.class == type) {
            return ColumnType.JSON_ARRAY;
        } else {
            String error = String.format(EntityConstants.ERROR_FILED_UNSUPPORTED_FORMAT, "Column", field.getType());
            throw new IllegalArgumentException(error);
        }
    }

    public static void isSafe(String identifier) {
        // Only letters, numbers, and underscores are allowed.
        if (!identifier.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("Invalid entity identifier: " + identifier);
        }
    }
}
