package net.sf.dz3.view.http.v3;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
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
public class OAuth2DeviceLimitedInputAuthTest {
    
    private final Logger logger = LogManager.getLogger(getClass());

    private final String HCC_CLIENT_ID = "HCC_CLIENT_ID";
    private final String HCC_CLIENT_SECRET = "HCC_CLIENT_SECRET";
    
    @Test
    public void testAuth() throws IOException, InterruptedException {

        ThreadContext.push("testAuth");

        try {
            
            Map<String, String> env = ContextChecker.check(new String[] {HCC_CLIENT_ID, HCC_CLIENT_SECRET}, true);
            
            String clientId = env.get(HCC_CLIENT_ID);
            String clientSecret = env.get(HCC_CLIENT_SECRET);

            HttpClient httpClient = new HttpClient();
            PostMethod post = new PostMethod("https://accounts.google.com/o/oauth2/device/code");
            
            // Step 1
            // https://developers.google.com/identity/protocols/OAuth2ForDevices#step-1-request-device-and-user-codes

            post.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
            post.addParameter(new NameValuePair("client_id", clientId));
            post.addParameter(new NameValuePair("scope", "email"));
            
            int rc = httpClient.executeMethod(post);
            
            // Step 2
            // https://developers.google.com/identity/protocols/OAuth2ForDevices#step-2-handle-the-authorization-server-response

            logger.info("RC=" + rc);

            if (rc != 200) {

                logger.error("HTTP rc=" + rc + ", text follows:");
                logger.error(post.getResponseBodyAsString());
                
                fail("request failed with HTTP code " + rc + ", see log for details");
            }
            
            String responseJson = post.getResponseBodyAsString();
            Gson gson = new Gson();
            Type mapType  = new TypeToken<Map<String,String>>(){}.getType();
            Map<String,String> responseMap = gson.fromJson(responseJson, mapType);
            
            logger.info("response: " + responseMap);
            
            String verificationUrl = responseMap.get("verification_url");
            String userCode =  responseMap.get("user_code");
            int interval = Integer.parseInt(responseMap.get("interval").toString());
            
            // VT: FIXME: The loop will break after "expires_in" - but this is beyond the test case

            while (true) {

                // Step 3
                // https://developers.google.com/identity/protocols/OAuth2ForDevices#displayingthecode

                // It will most probably display this URL: https://www.google.com/device

                logger.info("Please go to " + verificationUrl + " and enter this code: " + userCode);
                
                Thread.sleep(interval * 1000);
                
                {
                    // Step 4
                    // https://developers.google.com/identity/protocols/OAuth2ForDevices#step-4-poll-googles-authorization-server

                    // Step 5 - on a different device
                    // https://developers.google.com/identity/protocols/OAuth2ForDevices#step-5-user-responds-to-access-request

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

            // Step 6
            // https://developers.google.com/identity/protocols/OAuth2ForDevices#step-6-handle-responses-to-polling-requests

            {
                responseMap = gson.fromJson(responseJson, mapType);

                String accessToken = responseMap.get("access_token");
                String expiresIn = responseMap.get("expires_in");
                String refreshToken = responseMap.get("refresh_token");
                String tokenType = responseMap.get("token_type");

                // Two fields below are not documented un the page above

                String scope = responseMap.get("scope");

                // This is JWT: https://stackoverflow.com/questions/8311836/how-to-identify-a-google-oauth2-user/13016081#13016081
                // Has the information we need to match HCC Core with HCC Remote.

                String idToken = responseMap.get("id_token");

                // VT: FIXME: This is a temporary solution sufficient for the test case.
                // Need to use a dedicated library, and verify the signature.
                // Good candidate: https://github.com/auth0/java-jwt

                dumpJWT(idToken);

                // All we need now is to make sure we get HTTP 200 on subsequent calls to Google APIs (including HCC Proxy)

                {
                    post = new PostMethod("https://www.googleapis.com/oauth2/v3/userinfo");
                    post.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
                    post.addParameter(new NameValuePair("access_token", accessToken));

                    rc = httpClient.executeMethod(post);

                    logger.info("RC=" + rc);

                    if (rc != 200) {

                        logger.error("HTTP rc=" + rc + ", text follows:");
                        logger.error(post.getResponseBodyAsString());

                        fail("OAuth 2.0 for TV and Limited-Input Device Applications: couldn't finish the sequence");
                    }

                    responseJson = post.getResponseBodyAsString();

                    logger.info("response: " + responseJson);
                }
            }
            
        } finally {
            ThreadContext.pop();
        }

    }

    private void dumpJWT(String source) {

        ThreadContext.push("dumpJWT");

        try {

            String[] split_string = source.split("\\.");

            String base64EncodedHeader = split_string[0];
            String base64EncodedBody = split_string[1];
            String base64EncodedSignature = split_string[2];

            Base64 base64Url = new Base64(true);
            String header = new String(base64Url.decode(base64EncodedHeader));
            String body = new String(base64Url.decode(base64EncodedBody));

            logger.info("header: " + header);
            logger.info("body: " + body);

        } finally {
            ThreadContext.pop();
        }
    }
}
