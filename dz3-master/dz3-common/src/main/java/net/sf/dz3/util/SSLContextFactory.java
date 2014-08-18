package net.sf.dz3.util;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * A utility class to simplify creation of {@link SSLContext SSLContext}.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2009
 */
public class SSLContextFactory {

    /**
     * Create an SSL context object with default options. The SSL context is
     * created with a protocol {@code TLS}, keystore located at
     * {@code ~/.keystore}, and a given keystore password.
     * 
     * @param password Keystore password.
     * @return The SSL context.
     * @throws SSLException if there was an SSL related problem.
     */
    public static SSLContext createContext(String password) throws SSLException {

        return createContext("TLS", System.getProperty("user.home") + File.separator + ".keystore", password);
    }

    /**
     * Create an SSL context object.
     * 
     * @param protocol Secure protocol. Values that are known to work are:
     * {@code SSLv3}, {@code TLS}.
     * @param keyStoreName Keystore file name.
     * @param password Keystore password.
     * @return The SSL context.
     * @throws SSLException If there was an SSL related problem.
     */
    public static SSLContext createContext(String protocol, String keyStoreName, String password) throws SSLException {

        try {

            SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            SSLContext ctx = SSLContext.getInstance(protocol);

            if (password == null) {

                // Whatever...

                password = "";
            }

            char[] passwordArray = new char[password.length()];

            for (int idx = 0; idx < password.length(); idx++) {

                passwordArray[idx] = password.charAt(idx);
            }

            FileInputStream keyStoreFile = new FileInputStream(keyStoreName);
            KeyStore ks = KeyStore.getInstance("JKS");

            ks.load(keyStoreFile, null);

            String keyManagementAlgorithm = "SunX509";
            KeyManagerFactory km = KeyManagerFactory.getInstance(keyManagementAlgorithm);
            km.init(ks, passwordArray);

            KeyManager[] keyManagerSet = km.getKeyManagers();

            for (int i = 0; i < keyManagerSet.length; i++) {

                // System.err.println("KeyManager " + keyManagerSet[i]);
            }

            TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(keyManagementAlgorithm);

            tmFactory.init(ks);

            TrustManager[] trustManagerSet = tmFactory.getTrustManagers();

            for (int i = 0; i < trustManagerSet.length; i++) {

                // System.err.println("TrustManager " + trustManagerSet[i]);
            }

            ctx.init(keyManagerSet, trustManagerSet, random);

            return ctx;

        } catch (Throwable t) {

            SSLException ex = new SSLException("Can't create secure connection (SSLContext)");

            ex.initCause(t);

            throw ex;
        }
    }
}
