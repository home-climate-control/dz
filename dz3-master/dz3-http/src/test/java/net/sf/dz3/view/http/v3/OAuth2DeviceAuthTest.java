package net.sf.dz3.view.http.v3;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Test case for {@link https://developers.google.com/identity/protocols/OAuth2ForDevices OAuth 2.0 for TV and Limited-Input Device Applications}.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public class OAuth2DeviceAuthTest {
    
    private final Logger logger = LogManager.getLogger(getClass());

    private final String HCC_CLIENT_ID = "HCC_CLIENT_ID";
    private final String HCC_CLIENT_SECRET = "HCC_CLIENT_SECRET";
    
    @Test
    public void testAuth() throws IOException, InterruptedException {

        ThreadContext.push("testAuth");

        try {
            
            // Make sure the environment is set correctly, otherwise this code will not have enough information to proceed
            
            String clientId = System.getenv(HCC_CLIENT_ID);
            
            if (clientId == null) {
                fail("Please set " + HCC_CLIENT_ID);
            }
            
            String clientSecret = System.getenv(HCC_CLIENT_SECRET);
            
            if (clientSecret == null) {
                fail("Please set " + HCC_CLIENT_SECRET);
            }

            HttpClient httpClient = new HttpClient();
            PostMethod post = new PostMethod("https://accounts.google.com/o/oauth2/device/code");
            
            post.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
            post.addParameter(new NameValuePair("client_id", clientId));
            post.addParameter(new NameValuePair("scope", "email profile"));
            
            int rc = httpClient.executeMethod(post);
            
            logger.info("RC=" + rc);

            if (rc != 200) {

                logger.error("HTTP rc=" + rc + ", text follows:");
                logger.error(post.getResponseBodyAsString());
                
                fail("request failed with HTTP code " + rc + ", see log for details");
            }
            
            String responseJson = post.getResponseBodyAsString();
            StringReader sr = new StringReader(responseJson);
            Gson gson = new Gson();
            Type mapType  = new TypeToken<Map<String,String>>(){}.getType();
            Map<String,String> responseMap = gson.fromJson(responseJson, mapType);
            
            logger.info("response: " + responseMap);
            
            String verificationUrl = responseMap.get("verification_url");
            String userCode =  responseMap.get("user_code");
            int interval = Integer.parseInt(responseMap.get("interval").toString());
            
            while (true) {
            
                logger.info("Please go to " + verificationUrl + " and enter this code: " + userCode);
                
                Thread.sleep(interval * 1000);
                
                {
                    post = new PostMethod("https://www.googleapis.com/oauth2/v4/token");
                    post.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
                    post.addParameter(new NameValuePair("client_id", clientId));
                    post.addParameter(new NameValuePair("client_secret", clientSecret));
                    post.addParameter(new NameValuePair("code", responseMap.get("device_code")));
                    post.addParameter(new NameValuePair("grant_type", "http://oauth.net/grant_type/device/1.0"));
                    
                    rc = httpClient.executeMethod(post);
                    
                    logger.info("RC=" + rc);

                    if (rc != 200) {

                        logger.error("HTTP rc=" + rc + ", text follows:");
                        logger.error(post.getResponseBodyAsString());
                        
                        logger.warn("request failed with HTTP code " + rc + ", will retry in " + interval + " seconds");
                    }
                    
                    responseJson = post.getResponseBodyAsString();
                    
                    logger.info("response: " + responseJson);
                    
                    if (rc == 200) {
                        break;
                    }
                }
            }
            
        } finally {
            ThreadContext.pop();
        }

    }
}
