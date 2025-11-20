package io.github.siyukio.tools.api;

/**
 * API service runtime configuration information.
 *
 * @author Buddy
 */
public abstract class ApiProfiles {

    public static String IP4 = "";

    public static String CONTEXT_PATH = "";

    public static int PORT = -1;

    public static String getApiPath(String path) {
        if (!CONTEXT_PATH.isEmpty()) {
            path = path.replaceFirst(CONTEXT_PATH, "");
        }
        return path;
    }
}
