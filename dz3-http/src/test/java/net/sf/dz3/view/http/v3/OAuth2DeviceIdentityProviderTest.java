package net.sf.dz3.view.http.v3;

import net.sf.dz3.view.http.common.ContextChecker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

class OAuth2DeviceIdentityProviderTest {

    private final Logger logger = LogManager.getLogger(getClass());
    private final OAuth2DeviceIdentityProvider provider = new OAuth2DeviceIdentityProvider();

    private final String HCC_CLIENT_ID = "HCC_CLIENT_ID";
    private final String HCC_CLIENT_SECRET = "HCC_CLIENT_SECRET";
    
    @Test
    public void testIdentity() throws IOException, InterruptedException {
        
        ThreadContext.push("testIdentity");

        try {

            if (!ContextChecker.runNow(logger)) {
                return;
            }
            
            Map<String, String> env = ContextChecker.check(new String[] {HCC_CLIENT_ID, HCC_CLIENT_SECRET}, true);
            
            String clientId = env.get(HCC_CLIENT_ID);
            String clientSecret = env.get(HCC_CLIENT_SECRET);
            File refreshTokenFile = new File(System.getProperty("user.home"), ".dz/test/" + getClass().getSimpleName());
            
            String identity = provider.getIdentity(clientId, clientSecret, refreshTokenFile, getClass().getSimpleName());
            
            logger.info("identity: " + identity);
            
        } finally {
            ThreadContext.pop();
        }
    }
}
