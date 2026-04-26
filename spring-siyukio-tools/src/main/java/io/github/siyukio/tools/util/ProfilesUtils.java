package io.github.siyukio.tools.util;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Bugee
 */
@Slf4j
public class ProfilesUtils {

    /**
     * Check if the JVM launch command indicates a test execution.
     * Covers IDEA JUnitStarter and Maven Surefire commands.
     *
     * @return true if the launch command suggests test execution
     */
    public static boolean isJUnit() {
        String cmd = System.getProperty("sun.java.command", "");
        log.info("sun.java.command: {}", cmd);
        return cmd.contains("JUnitStarter")
                || cmd.contains("org.junit")
                || cmd.contains("surefire")
                || cmd.contains("surefirebooter");
    }

}
