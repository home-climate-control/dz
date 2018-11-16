package net.sf.dz3.view.http.v3;

import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Simple class to check whether the test cases run in an environment they need
 */
public class ContextChecker {

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
