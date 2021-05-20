package net.sf.dz3.view.http.common;


import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.fail;

/**
 * Simple class to check whether the test cases run in an environment they need
 */
public class ContextChecker {

    /**
     * @return {@code true} if test needs to be run now, otherwise print a warning and return {@code false}.
     */
    public static boolean runNow(Logger logger) {

        String RUN_NOW = "HCC_RUN_TESTS_NOW";

        String runTest = System.getenv(RUN_NOW);

        if (runTest == null) {

            logger.warn("not configured to run - set " + RUN_NOW + " environment variable to a non-empty value to run");
            return false;
        }

        return true;
    }

    /**
     * Make sure all the variables specified are set and retrieve them.
     *
     * @param variables Variables to check.
     * @param abort If {@code true}, will {@code fail()} after informing the user
     * about the necessary variables to set.
     *
     * @return All the variables in the map, so we don't have to call {@link System#getenv()} again.
     */
    public static Map<String, String> check(String[] variables, boolean abort) {

        Map<String, String> result = new HashMap<>();
        Set<String> missing = new TreeSet<>();

        for (int offset = 0; offset < variables.length; offset++) {

            String key = variables[offset];
            String value = System.getenv(key);

            result.put(key, value);

            if (value == null) {
                missing.add(key);
            }
        }

        if (!missing.isEmpty() && abort) {

            fail("Please set environment variables: " + missing);
        }

        return result;
    }
}
