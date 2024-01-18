package net.sf.dz3r.test;

/**
 * Syntax sugar for using environment variables in tests.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class TestVariableProvider {

    /**
     * Get the variable.
     *
     * @param variable Environment variable name.
     * @param description Variable description to provide in the exception message.
     * @return Environment variable value, if exists.
     * @exception IllegalStateException if the variable is undefined.
     */
    public static String getEnv(String variable, String description) {
        String value = System.getenv(variable);
        if (value == null) {
            throw new IllegalStateException("Define environment variable " + variable + "=<" + description + "> to run this test");
        }
        return value;
    }
}
