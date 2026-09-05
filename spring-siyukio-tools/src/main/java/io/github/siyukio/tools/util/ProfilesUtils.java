package io.github.siyukio.tools.util;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Bugee
 */
@Slf4j
public class ProfilesUtils {

    /**
     * Manually specified JUnit flag.
     * When not null, it takes precedence over the value detected from the JVM launch command.
     */
    private static Boolean junit;

    /**
     * Check if the JVM launch command indicates a test execution.
     * Covers IDEA JUnitStarter and Maven Surefire commands.
     * If {@link #junit} has been set manually, that value is returned directly.
     *
     * @return true if the launch command suggests test execution
     */
    public static boolean isJUnit() {
        if (junit != null) {
            return junit;
        }
        String cmd = System.getProperty("sun.java.command", "");
        log.info("sun.java.command: {}", cmd);
        return cmd.contains("JUnitStarter")
                || cmd.contains("org.junit")
                || cmd.contains("surefire")
                || cmd.contains("surefirebooter");
    }

    /**
     * Manually specify whether the current environment is a JUnit test environment.
     *
     * @param junit true to force JUnit mode, false to force non-JUnit mode, null to restore auto detection
     */
    public static void setJUnit(Boolean junit) {
        ProfilesUtils.junit = junit;
    }

}
