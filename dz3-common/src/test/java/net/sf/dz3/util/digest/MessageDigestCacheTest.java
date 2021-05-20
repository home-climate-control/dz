package net.sf.dz3.util.digest;

import com.homeclimatecontrol.jukebox.util.MessageDigestFactory;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(hash).isEqualTo(md.getMD5(key));

        String hash2 = MessageDigestCache.getMD5(key);
        assertThat(hash2).isEqualTo(hash);

        assertThat(MessageDigestCache.cache).hasSize(1);
    }

    @Test
    public void testMiss() {
        
        MessageDigestCache.cache.clear();

        String key1 = nextRandomString();
        String key2 = nextRandomString();

        // Same random value twice in a row? That's a rarity
        assertThat(key2).isNotEqualTo(key1);

        String hash1 = MessageDigestCache.getMD5(key1);
        String hash2 = MessageDigestCache.getMD5(key2);

        assertThat(hash1).isEqualTo(md.getMD5(key1));
        assertThat(hash2).isEqualTo(md.getMD5(key2));

        assertThat(MessageDigestCache.cache).hasSize(2);
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

        assertThat(MessageDigestCache.cacheSizeLimitSoft).isEqualTo(limit * 2);
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

        assertThat(MessageDigestCache.cacheSizeLimitSoft).isEqualTo(MessageDigestCache.cacheSizeLimitHard);
    }
}
