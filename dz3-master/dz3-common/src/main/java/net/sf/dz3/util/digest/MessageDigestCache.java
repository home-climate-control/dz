package net.sf.dz3.util.digest;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import net.sf.jukebox.util.MessageDigestFactory;

/**
 * Caching wrapper for {@link MessageDigestFactory}
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2015
 */
public class MessageDigestCache {

    private static final Logger logger = Logger.getLogger(MessageDigestCache.class);
    private static final MessageDigestFactory provider = new MessageDigestFactory();
    
    /**
     * The cache.
     * 
     * Note that the visibility is package private - for testability.
     */
    static final Map<String, String> cache = new HashMap<String, String>();
    
    /**
     * Cache size limit.
     * 
     * It is expected that the number of cache entries will be finite, and will
     * stabilize pretty soon after the system is started. Let's hardcode it
     * according to "worse is better", and see whetherthis is good enough.
     * 
     * Note that the visibility is package private - for testability.
     */
    static int cacheSizeLimitSoft = 500;
    
    public static final int cacheSizeLimitHard = (int) (cacheSizeLimitSoft * Math.pow(2, 4));
    
    /**
     * This operation is supposed to be called pretty often, and the log will be flooded with warnings if it happens.
     * 
     * Note that the visibility is package private - for testability.
     */
    static boolean warningGiven = false;

    public synchronized static String getMD5(String message) {
        
        String hash = cache.get(message);
        
        if (hash != null) {
            return hash;
        }
        
        hash = provider.getMD5(message);
        
        cache.put(message, hash);
        
        checkSize();
        
        return hash;
    }

    private static void checkSize() {


        
        int cacheSize = cache.size();
        
        if (cacheSize >= cacheSizeLimitHard) {

            if (!warningGiven) {
            
                logger.fatal("Cache size (" + cacheSize + ") exceeded hard limet, assumptions are flawed, need to fix this, submit a bug report here: https://github.com/home-climate-control/dz/issues");
                warningGiven = true;
            }
            
            return;
        }
        
        if (cacheSize >= cacheSizeLimitSoft) {
            
            cacheSizeLimitSoft *= 2;

            logger.warn("cache is growing too big (" + cacheSize + " entries), soft limit bumped up to " + cacheSizeLimitSoft);
        }
    }
}
