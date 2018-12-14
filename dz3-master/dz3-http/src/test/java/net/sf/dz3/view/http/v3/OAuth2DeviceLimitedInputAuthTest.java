package net.sf.dz3.view.http.v3;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.sf.dz3.view.http.common.ContextChecker;

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

            if (!ContextChecker.runNow(logger)) {
                return;
            }
            
            Map<String, String> env = ContextChecker.check(new String[] {HCC_CLIENT_ID, HCC_CLIENT_SECRET}, true);
            
            String clientId = env.get(HCC_CLIENT_ID);
            String clientSecret = env.get(HCC_CLIENT_SECRET);

            HttpClient httpClient = HttpClientBuilder.create().build();
            String refreshToken = getRefreshToken();
            String accessToken;

            if (refreshToken == null) {

                accessToken = acquire(httpClient, clientId, clientSecret);

            } else {

                accessToken = refresh(httpClient, clientId, clientSecret, refreshToken);
            }

            // All we need now is to make sure we get HTTP 200 on subsequent calls to Google APIs (including HCC Proxy)

            {
                HttpPost post = new HttpPost("https://www.googleapis.com/oauth2/v3/userinfo");

                post.setHeader("Content-Type", "application/x-www-form-urlencoded");
                post.setHeader("Authorization", "Bearer " + accessToken);

                HttpResponse rsp = httpClient.execute(post);
                int rc = rsp.getStatusLine().getStatusCode();

                logger.info("RC=" + rc);

                if (rc != 200) {

                    logger.error("HTTP rc=" + rc + ", text follows:");
                    logger.error(EntityUtils.toString(rsp.getEntity()));

                    fail("OAuth 2.0 for TV and Limited-Input Device Applications: couldn't finish the sequence");
                }

                String responseJson = EntityUtils.toString(rsp.getEntity());

                logger.info("response: " + responseJson);
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * @return {@code access_token}.
     */
    private String acquire(HttpClient httpClient, String clientId, String clientSecret) throws IOException, InterruptedException {

        ThreadContext.push("acquire");

        try {

            // Step 1
            // https://developers.google.com/identity/protocols/OAuth2ForDevices#step-1-request-device-and-user-codes

            URIBuilder builder = new URIBuilder("https://accounts.google.com/o/oauth2/device/code");
            
            builder.addParameter("client_id", clientId);
            builder.addParameter("scope", "email");

            HttpPost post = new HttpPost(builder.toString());

            post.setHeader("Content-Type", "application/x-www-form-urlencoded");

            HttpResponse rsp = httpClient.execute(post);
            int rc = rsp.getStatusLine().getStatusCode();
            
            // Step 2
            // https://developers.google.com/identity/protocols/OAuth2ForDevices#step-2-handle-the-authorization-server-response

            logger.info("RC=" + rc);

            if (rc != 200) {

                logger.error("HTTP rc=" + rc + ", text follows:");
                logger.error(EntityUtils.toString(rsp.getEntity()));
                
                fail("request failed with HTTP code " + rc + ", see log for details");
            }
            
            String responseJson = EntityUtils.toString(rsp.getEntity());
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

                    builder = new URIBuilder("https://www.googleapis.com/oauth2/v4/token");

                    builder.addParameter("client_id", clientId);
                    builder.addParameter("client_secret", clientSecret);
                    builder.addParameter("code", responseMap.get("device_code"));
                    builder.addParameter("grant_type", "http://oauth.net/grant_type/device/1.0");

                    post = new HttpPost(builder.toString());
                    post.setHeader("Content-Type", "application/x-www-form-urlencoded");
                    
                    rsp = httpClient.execute(post);
                    rc = rsp.getStatusLine().getStatusCode();
                    String responseBody = EntityUtils.toString(rsp.getEntity());
                    
                    logger.info("RC=" + rc);

                    if (rc != 200) {

                        logger.error("HTTP rc=" + rc + ", text follows:");
                        logger.error(responseBody);
                        
                        logger.warn("request failed with HTTP code " + rc + ", will retry in " + interval + " seconds");
                    }
                    
                    responseJson = responseBody;
                    
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
                storeRefreshToken(refreshToken);

                return accessToken;
            }

        } catch (URISyntaxException ex) {

            throw new IOException("malformed URI", ex);

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * @return {@code access_token}.
     */
    private String refresh(HttpClient httpClient, String clientId, String clientSecret, String refreshToken) throws IOException {

        ThreadContext.push("refresh");

        try {

            URIBuilder builder = new URIBuilder("https://www.googleapis.com/oauth2/v4/token");

            builder.addParameter("client_id", clientId);
            builder.addParameter("client_secret", clientSecret);
            builder.addParameter("refresh_token", refreshToken);
            builder.addParameter("grant_type", "refresh_token");

            HttpPost post = new HttpPost(builder.toString());
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");

            HttpResponse rsp = httpClient.execute(post);
            int rc = rsp.getStatusLine().getStatusCode();

            logger.info("RC=" + rc);

            if (rc != 200) {

                logger.error("HTTP rc=" + rc + ", text follows:");
                logger.error(EntityUtils.toString(rsp.getEntity()));

                fail("request failed with HTTP code " + rc + ", see log for details");
            }

            String responseJson = EntityUtils.toString(rsp.getEntity());
            Gson gson = new Gson();
            Type mapType  = new TypeToken<Map<String,String>>(){}.getType();
            Map<String,String> responseMap = gson.fromJson(responseJson, mapType);

            logger.info("response: " + responseMap);

            return responseMap.get("access_token");

        } catch (URISyntaxException ex) {

            throw new IOException("malformed URI", ex);

        } finally {
            ThreadContext.pop();
        }
    }

    private File getRefreshTokenLocation() {

        return new File(System.getProperty("user.home"), ".dz/test/" + getClass().getSimpleName());
    }

    private void storeRefreshToken(String token) throws IOException {

        ThreadContext.push("storeRefreshToken");

        try {

            File target = getRefreshTokenLocation();

            logger.info("target: " + target);

            // Create the directory if it doesn't exist
            File dir = target.getParentFile();

            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw new IOException("couldn't create " + dir);
                }
            }

            PrintWriter pw = new PrintWriter(new FileWriter(target));

            pw.println(token);
            pw.close();

        } finally {
            ThreadContext.pop();
        }
    }

    private String getRefreshToken() throws IOException {

        ThreadContext.push("getRefreshToken");

        try {

            File target = getRefreshTokenLocation();

            logger.info("source: " + target);

            if (!target.exists()) {
                return null;
            }

            logger.warn("Stored refresh token detected in " + target + ", remove that file to reacquire access_token from scratch");

            BufferedReader br = new BufferedReader(new FileReader(target));

            return br.readLine();

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
