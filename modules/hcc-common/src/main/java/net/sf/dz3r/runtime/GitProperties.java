package net.sf.dz3r.runtime;

import java.io.IOException;
import java.util.Properties;

public class GitProperties {

    private static Properties props;

    private GitProperties() {

    }

    public static synchronized Properties get() throws IOException {

        if (props == null) {

            var p = new Properties();

            try (var is = GitProperties.class.getClassLoader().getResourceAsStream("git.properties")) {
                p.load(is);
                props = p;
            }
        }

        return props;
    }
    public static synchronized String getProperty(String key) throws IOException {
        return get().getProperty(key);
    }
}
