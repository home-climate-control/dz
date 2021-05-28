package net.sf.dz3.util;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

/**
 * A utility class to simplify creation of {@link SSLContext SSLContext}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2009
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

            var random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            var ctx = SSLContext.getInstance(protocol);

            if (password == null) {

                // Whatever...
                password = "";
            }

            var passwordArray = new char[password.length()];

            for (var idx = 0; idx < password.length(); idx++) {
                passwordArray[idx] = password.charAt(idx);
            }

            try (var keyStoreFile = new FileInputStream(keyStoreName)) {

                var ks = KeyStore.getInstance("JKS");

                ks.load(keyStoreFile, null);

                var keyManagementAlgorithm = "SunX509";
                var km = KeyManagerFactory.getInstance(keyManagementAlgorithm);
                km.init(ks, passwordArray);

                KeyManager[] keyManagerSet = km.getKeyManagers();

                for (int i = 0; i < keyManagerSet.length; i++) {

                    // System.err.println("KeyManager " + keyManagerSet[i]);
                }

                var tmFactory = TrustManagerFactory.getInstance(keyManagementAlgorithm);

                tmFactory.init(ks);

                TrustManager[] trustManagerSet = tmFactory.getTrustManagers();

                for (int i = 0; i < trustManagerSet.length; i++) {

                    // System.err.println("TrustManager " + trustManagerSet[i]);
                }

                ctx.init(keyManagerSet, trustManagerSet, random);

                return ctx;
            }

        } catch (Throwable t) {

            throw new SSLException("Can't create secure connection (SSLContext)", t);
        }
    }
}
