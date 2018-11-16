package net.sf.dz3.view.http.v3;

import static org.junit.Assert.fail;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.Test;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

/**
 * Test case for {@link https://cloud.google.com/docs/authentication/production Google Service Account Authentication}.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public class ServiceAccountAuthTest {
    
    private final Logger logger = LogManager.getLogger(getClass());

    private final String GOOGLE_APPLICATION_CREDENTIALS = "GOOGLE_APPLICATION_CREDENTIALS";
    
    @Test
    public void testAuth() {

        ThreadContext.push("testAuth");

        try {

            if (!ContextChecker.runNow(logger)) {
                return;
            }

            // We don't need the return value, just need to have it set

            ContextChecker.check(new String[] {GOOGLE_APPLICATION_CREDENTIALS}, true);

            Storage storage = StorageOptions.getDefaultInstance().getService();

            Page<Bucket> buckets = storage.list();
            
            int count = 0;

            for (Bucket bucket : buckets.iterateAll()) {

                logger.info("bucket: " + bucket);
                count++;
            }

            logger.info("bucket count: " + count);
            
        } finally {
            ThreadContext.pop();
        }

    }
}
