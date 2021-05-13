package net.sf.dz3.util.digest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Random;

import org.junit.Test;

import com.homeclimatecontrol.jukebox.util.MessageDigestFactory;

public class MessageDigestCacheTest {

    private final MessageDigestFactory md = new MessageDigestFactory();
    private final Random rg = new Random();
    
    private String nextRandomString() {
    
        return Long.toHexString(rg.nextLong());
    }
    
    @Test
    public void testHit() {
        
        MessageDigestCache.cache.clear();

        String key = nextRandomString();
        
        String hash = MessageDigestCache.getMD5(key);
        
        assertEquals("Wrong hash computed", md.getMD5(key), hash);

        String hash2 = MessageDigestCache.getMD5(key);
        
        assertEquals("Different hashes computed???", hash, hash2);
        
        assertEquals("Wrong cache size", 1, MessageDigestCache.cache.size());
    }

    @Test
    public void testMiss() {
        
        MessageDigestCache.cache.clear();

        String key1 = nextRandomString();
        String key2 = nextRandomString();
        
        assertFalse("Same random value twice in a row? That's a rarity", key1.equals(key2));
        
        String hash1 = MessageDigestCache.getMD5(key1);
        String hash2 = MessageDigestCache.getMD5(key2);
        
        assertEquals("Wrong hash computed", md.getMD5(key1), hash1);
        assertEquals("Wrong hash computed", md.getMD5(key2), hash2);

        assertEquals("Wrong cache size", 2, MessageDigestCache.cache.size());
    }
    
    @Test
    public void testLimitSoft() {
        
        MessageDigestCache.cache.clear();
        MessageDigestCache.cacheSizeLimitSoft = 500;
        
        int limit = MessageDigestCache.cacheSizeLimitSoft;
        int count = limit + limit / 2;
        
        while (count-- > 0) {
            
            MessageDigestCache.getMD5(nextRandomString());
        }
        
        assertEquals("Wrong soft limit", limit * 2, MessageDigestCache.cacheSizeLimitSoft);
    }

    @Test
    public void testLimitHard() {
        
        MessageDigestCache.cache.clear();
        MessageDigestCache.cacheSizeLimitSoft = 500;
        
        int limit = MessageDigestCache.cacheSizeLimitHard;
        int count = limit + limit / 2;
        
        while (count-- > 0) {
            
            MessageDigestCache.getMD5(nextRandomString());
        }
        
        assertEquals("Wrong limit", MessageDigestCache.cacheSizeLimitHard, MessageDigestCache.cacheSizeLimitSoft);
    }
}
