package net.sf.dz3.device.sensor.impl.tcp.server;

import net.sf.dz3.device.sensor.impl.tcp.TcpConnectionSignature;
import net.sf.dz3.util.SSLContextFactory;
import net.sf.jukebox.jmx.JmxAttribute;
import net.sf.jukebox.sem.MutexSemaphore;
import net.sf.jukebox.sem.SemaphoreGroup;
import net.sf.jukebox.service.ActiveService;
import net.sf.jukebox.service.PassiveService;
import net.sf.jukebox.util.network.HostHelper;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * The TCP connection listener.
 * <p/>
 * Provides:
 * <ul>
 * <li> Configuration;
 * <li> Connection setup, including SSL;
 * <li> Client handling logic;
 * <li> Service announcements.
 * </ul>
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2009
 */
public abstract class AbstractListener extends PassiveService {

    /**
     * Set of addresses to listen on. Empty set means that we're listening on
     * all local addresses.
     */
    private Set<String> addressSet = new TreeSet<String>();

    /**
     * The port to broadcast on.
     */
    private int broadcastPort;

    TcpConnectionSignature signature;

    /**
     * The listener services.
     */
    private Set<Listener> listenerSet = new TreeSet<Listener>();

    /**
     * Set of active clients.
     */
    private Set<ConnectionHandler> clientSet = new HashSet<ConnectionHandler>();

    /**
     * Exclusive lock to avoid race conditions between {@link #cleanup
     * cleanup()} and new connections.
     */
    private MutexSemaphore cleanupLock = new MutexSemaphore();

    /*
     * The multicast server.
     * 
     * VT: FIXME: WIll reinstate it later, after things are solidified with direct connections.
     */
    //private MulticastServer multicastServer;

    public AbstractListener(Set<String> addressSet, int port, int broadcastPort) {
        this(addressSet, port, broadcastPort, false, null);
    }

    public AbstractListener(Set<String> addressSet, int port, int broadcastPort, boolean secure, String password) {

        this.addressSet.addAll(addressSet);
        
        this.signature = new TcpConnectionSignature(port, secure, password);
        this.broadcastPort = broadcastPort;
    }

    @JmxAttribute(description="Listening port")
    public int getListenPort() {
        return signature.port;
    }

    @JmxAttribute(description="Broadcasting port")
    public int getBroadcastPort() {
        return broadcastPort;
    }

    /**
     * @return Iterator on connection handlers.
     */
    public final Iterator<ConnectionHandler> iterator() {
        return clientSet.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void startup() throws Throwable {

        // Perform the startup actions for the subclasses first - it doesn't
        // make any sense to open the connection listener if the subclass
        // business logic determines that something's not right and we can't
        // start

        startup2();

        // Let's see if we have any addresses configured. If we don't, it
        // means that we'll listen on all local ports.

        if (addressSet.isEmpty()) {

            logger.info("Listening on all local addresses");

            Listener l = new Listener(null, signature.port);

            if (l.start().waitFor()) {
                synchronized (listenerSet) {
                    listenerSet.add(l);
                }
            } else {
                logger.warn("Failed to start listener on *");

            }

        } else {

            // Let's collect the list of existing network interfaces, their
            // addresses and see if we're properly configured. If there's a
            // mismatch, the bad address will be removed from addressSet. If it
            // turns out that the addressSet is empty after verification is
            // done, we refuse to start.

            Set<InetAddress> validAddresses = HostHelper.getLocalAddresses();

            for (Iterator<String> i = addressSet.iterator(); i.hasNext();) {

                String address = i.next();
                InetAddress configuredAddress = InetAddress.getByName(address);

                if (!validAddresses.contains(configuredAddress)) {

                    logger.warn("Address specified in the configuration is not locally present: " + address);
                    i.remove();
                    continue;
                }

                Listener l = new Listener(InetAddress.getByName(address), signature.port);

                // VT: FIXME: It is possible to parallel the startups in order
                // to get all the listeners up faster, but this will complicate
                // the logic, so let's do it when it becomes *absolutely*
                // necessary.

                if (l.start().waitFor()) {
                    synchronized (listenerSet) {
                        listenerSet.add(l);
                    }
                } else {
                    logger.warn("Failed to start listener on '" + address + "'");
                }
            }
        }

        if (listenerSet.isEmpty()) {
            throw new IllegalStateException("No listeners could be started");
        }

        /*

    // VT: FIXME: Make sure we include the server signature (bug
    // #914695). I'd guess that it should be passed down to us in the
    // constructor.

    multicastServer = new SimpleBroadcastServer(new HashSet<String>(addressSet), broadcastPort);

    multicastServer.announce(getAnnounce());
    multicastServer.start();
         */
    }

    /**
     * Do a service specific startup.
     *
     * @throws Throwable if anything goes wrong.
     */
    protected abstract void startup2() throws Throwable;

    /**
     * Provide a service signature. This signature must uniquely identify the
     * module for the purpose of broadcast announcement.
     *
     * @return The service signature.
     */
    @JmxAttribute(description="Service signature")
    public abstract String getServiceSignature();

    /**
     * Provide a reasonable default for the {@link #listenPort port} to listen to.
     *
     * @return Default port to listen on.
     */
    @JmxAttribute(description="Default listening port")
    public abstract int getDefaultListenPort();

    /**
     * Provide a reasonable default for the {@link #listenPort port} to broadcast on.
     *
     * @return Default port to broadcast on.
     */
    @JmxAttribute(description="Default broadcasting port")
    public abstract int getDefaultBroadcastPort();

    /**
     * Syntax sugar to change an announce message.
     *
     * @param message Message to announce.
     */
    protected final void announce(String message) {

        /*
    if (multicastServer != null && multicastServer.isReady()) {

      multicastServer.announce(message);
    }
         */
    }

    /**
     * Get the message to announce to our clients.
     *
     * @return The announce message.
     */
    protected synchronized String getAnnounce() {

        // The only things that make sense to include into *our*
        // announcement are:

        // - The server signature (FIXME);
        // - the port to connect to;
        // - whether the connection is expected to be secure.

        // If the client is able to receive the broadcast, then they will
        // get the broadcast source and use it as the other endpoint, and
        // this is sufficient. If they don't get the broadcast, they can't
        // see us anyway and therefore don't care about all the ports we're
        // listening at.

        return "/" + signature.port + "/" + (signature.secure ? "secure" : "insecure");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void shutdown() throws Throwable {

        logger.info("Shutting down");

        //multicastServer.stop();

        SemaphoreGroup stop = new SemaphoreGroup();

        for (Iterator<Listener> i = new net.sf.jukebox.util.CollectionSynchronizer<Listener>().copy(listenerSet).iterator(); i.hasNext();) {

            Listener l = i.next();

            stop.add(l.stop());
            i.remove();
        }

        logger.info("Shut down listeners");

        // VT: clientSet is modified only by ConnectionHandler

        for (Iterator<ConnectionHandler> i = new net.sf.jukebox.util.CollectionSynchronizer<ConnectionHandler>().copy(clientSet).iterator(); i.hasNext();) {

            ConnectionHandler ch = i.next();

            ch.send("E Shutting down");
            stop.add(ch.stop());
        }

        stop.waitForAll();

        logger.info("All clients shut down");

        // Clean up the mess after the clients are gone

        cleanup();

        // Now let's shut down the subclasses

        shutdown2();
    }

    /**
     * Shut down the subclass.
     *
     * @throws Throwable if anything goes wrong.
     */
    protected abstract void shutdown2() throws Throwable;

    /**
     * Create a connection handler.
     *
     * @param socket Socket to use.
     * @param br     Reader to use.
     * @param pw     Writer to use.
     *
     * @return The connection handler.
     */
    protected abstract ConnectionHandler createHandler(Socket socket, BufferedReader br, PrintWriter pw);

    /**
     * Find out whether more than one connection is allowed.
     *
     * @return false if multiple connections are allowed.
     */
    protected abstract boolean isUnique();

    /**
     * Clean up after the last active connection is gone. This method must be
     * idempotent.
     *
     * @throws Throwable if anything goes wrong.
     */
    protected abstract void cleanup() throws Throwable;

    /**
     * Connection listener.
     */
    protected final class Listener extends ActiveService implements Comparable<Listener>, ListenerMBean {

        /**
         * Address to listen on.
         */
        final InetAddress addr;

        /**
         * Port to listen on.
         */
        final int port;

        /**
         * Server socket to listen with.
         */
        ServerSocket ss;

        /**
         * Create an instance.
         *
         * @param addr Local address to listen on.
         * @param port Local port to listen on.
         */
        Listener(InetAddress addr, int port) {

            this.addr = addr;
            this.port = port;
        }

        @JmxAttribute(description="Host pattern to listen to")
        public String getHost() {
            return (addr == null ? "*" : addr.toString()) + ":" + port;
        }

        @JmxAttribute(description="true if secure connection is requested by configuration")
        public boolean isSecureRequested() {
            return signature.secure;
        }

        @JmxAttribute(description="true if connected in secure mode")
        public boolean isSecure() {
            return ss instanceof SSLServerSocket;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void startup() throws Throwable {

            try {

                if (signature.secure) {

                    logger.info("Secure connection requested");

                    // VT: FIXME: It may make sense to be more flexible in
                    // creating the SSL context, to prevent
                    // NullPointerExceptions from being thrown up

                    try {

                        ss = SSLContextFactory.createContext(signature.password).getServerSocketFactory().createServerSocket(port, 256, addr);

                    } catch (SSLException sslex) {

                        logger.warn("Can't establish a secure listener on " + addr + ":"
                                + port, sslex);
                        logger.warn("Reverting to insecure connection");
                    }
                }

                if (ss == null) {

                    ss = new ServerSocket(port, 256, addr);

                }

                logger.info("Listening on " + addr + ":" + port);

            } catch (Throwable t) {

                throw new IllegalStateException("Can't listen on " + addr + ":" + port, t);
            }
        }

        /**
         * Keep accepting the TCP clients.
         *
         * @throws Throwable if anything goes wrong.
         */
        @Override
        protected final void execute() throws Throwable {

            while (isEnabled()) {

                Socket s = ss.accept();

                logger.info("Client arrived from " + s.getInetAddress() + ":" + s.getPort());

                // Redundant, but still: need to check if we're enabled -
                // it's been quite a while since we've checked for that, and
                // we may be shutting down right now.

                if (!isEnabled()) {

                    // They'll see this as a dropped connection, but it
                    // doesn't really make sense to be nice and verbose
                    // about it right now...

                    logger.warn("Shutting down - dropped " + s.getInetAddress() + ":"
                            + s.getPort());

                    s.close();
                    return;
                }

                // Bug #978029: let's acquire a lock before allowing the new
                // connection to start

                try {

                    cleanupLock.waitFor();

                    BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    PrintWriter pw = new PrintWriter(s.getOutputStream());

                    ConnectionHandler ch = createHandler(s, br, pw);

                    // VT: It is possible to have a race between the connection
                    // that has just been created and old connection, unless the
                    // old one is dropped before the new one is activated.

                    if (isUnique() && !clientSet.isEmpty()) {

                        logger.info("Unique - dropping existing connection");

                        // Since the new connection has not yet been added to
                        // the client set, an attempt to stop() the last handler
                        // will result in a cleanup() attempt. In order to avoid
                        // that, let's temporarily add the new connection
                        // handler to the set. Remember that
                        // ConnectionHandler.startup() will do it later.

                        try {

                            synchronized (clientSet) {

                                clientSet.add(ch);
                            }

                            drop(ch, s);

                        } finally {

                            synchronized (clientSet) {

                                clientSet.remove(ch);
                            }
                        }
                    }

                    // VT: FIXME: It may make perfect sense to split this off
                    // into a separate thread to prevent a denial of service
                    // caused by slowly starting clients

                    // Failure to start the client handler usually means that
                    // the client haven't complied to some of our protocol
                    // handshake requirements. In this case, we won't accept the
                    // client.

                    if (!ch.start().waitFor()) {

                        logger.info("Client coming from " + s.getInetAddress() + ":"
                                + s.getPort() + " failed to complete handshake - dropped");

                        // Since it may have been the last remaining connection
                        // (others may have been dropped without a cleanup,
                        // above), let's see if we have to clean things up.

                        if (clientSet.isEmpty()) {

                            // Yes, we do. Since the lock is already acquired,
                            // we don't have to do it again.

                            logger.info("Oops, performing cleanup()");

                            cleanup();
                        }

                        return;

                    }

                    logger.info("" + s.getInetAddress() + ":" + s.getPort() + ": started");

                } finally {

                    cleanupLock.release();
                }
            }
        }

        /**
         * Disconnect existing connection[s] and notify them about the newcomer.
         *
         * @param ch    Connection handler *not* to stop.
         * @param other Socket created for the other connection - for logging
         *              purposes.
         *
         * @throws InterruptedException if the wait is interrupted.
         */
        private void drop(ConnectionHandler ch, Socket other) throws InterruptedException {

            // Have to shut down all others (actually, there's just
            // zero or one)

            SemaphoreGroup stopped = new SemaphoreGroup();

            synchronized (clientSet) {

                for (Iterator<ConnectionHandler> i = clientSet.iterator(); i.hasNext();) {

                    ConnectionHandler oldHandler = i.next();

                    if (oldHandler == ch) {

                        // That's the one not to stop

                        continue;
                    }

                    oldHandler.send("E Disconnected: another client came from " + other.getInetAddress() + ":"
                            + other.getPort());
                    stopped.add(oldHandler.stop());
                }
            }

            // This is required to avoid a race condition between the new
            // connection startup and old connection shutdown.

            stopped.waitForAll();

            logger.info("Dropped existing connections");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void shutdown() throws Throwable {

        }

        /**
         * {@inheritDoc}
         */
        public int compareTo(Listener other) {
            
            if (other == null) {
                throw new IllegalArgumentException("other can't be null");
            }

            return (addr + ":" + port).compareTo(other.addr + ":" + other.port);
        }
    }

    /**
     * The connection handler.
     */
    abstract protected class ConnectionHandler extends PassiveService {

        /**
         * Socket to connect through.
         */
        protected Socket socket;

        /**
         * Reader to use.
         */
        protected BufferedReader br;

        /**
         * Writer to use.
         */
        protected PrintWriter pw;

        /**
         * Parser thread to use.
         */
        protected Thread parser;

        /**
         * Create an instance.
         *
         * @param socket Socket to use.
         * @param br     Reader to use.
         * @param pw     Writer to use.
         */
        public ConnectionHandler(Socket socket, BufferedReader br, PrintWriter pw) {

            this.socket = socket;
            this.br = br;
            this.pw = pw;
        }

        /**
         * Tell the listener what devices we have.
         */
        public abstract void iHave();

        /**
         * Send a message to the client.
         *
         * @param message Message to send.
         */
        public synchronized void send(String message) {

            if (!isEnabled()) {

                throw new IllegalStateException("Not enabled now, stopped?");
            }

            pw.println(message);
            pw.flush();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void startup() throws Throwable {

            // Dump all the known data on the client

            sayHello();

            // Start the command parser thread

            parser = new Thread(createParser());
            
            if (parser != null) {
                parser.start();
            }

            synchronized (clientSet) {

                clientSet.add(this);
            }

            logger.info("Active connections: " + clientSet.size());
        }

        /**
         * Execute the protocol handshake.
         */
        protected abstract void sayHello();

        /**
         * Create a command parser.
         *
         * @return Command parser.
         */
        protected abstract CommandParser createParser();

        /**
         * {@inheritDoc}
         */
        @Override
        protected void shutdown() throws Throwable {

            logger.info("Active connections: " + clientSet.size());
            logger.info("Shutting down...");

            try {

                // Shut down the command parser thread, unless it is down
                // already

                if (parser != null) {
                    parser.interrupt();
                }

                // Remove ourselves from the client list

                synchronized (clientSet) {

                    clientSet.remove(this);
                }

                socket.close();

                // VT: FIXME: Do I need this (below)?

                br.close();
                pw.close();

                logger.info("Shut down");

            } catch (Throwable t) {

                logger.warn("Unexpected exception:", t);
            }

            logger.info("Active connections: " + clientSet.size());

            if (clientSet.isEmpty()) {

                logger.info("Last active connection is gone, cleaning up");

                {
                    // VT: If cleanup is taking long enough (and we can
                    // safely assume it is), a race condition between
                    // shutting down connection and the connection that has
                    // just arrived is inevitable, especially in case of a
                    // control connection (bug #978029). To prevent this
                    // from happening, we must disallow initializing new
                    // connections while the cleanup is still in progress.
                    //
                    // This will slow down the handshake, which may or may
                    // not be a good thing - let's wait and see.

                    try {

                        cleanupLock.waitFor();

                        cleanup();

                    } finally {

                        cleanupLock.release();
                    }
                }
            }
        }

        /**
         * Command parser.
         */
        protected abstract class CommandParser implements Runnable {

            /**
             * Keep reading the data from {@link ConnectionHandler#br the reader} and
             * {@link #parse(String) parsing} it.
             */
            public void run() {

                while (true) {

                    String line = null;

                    try {

                        line = br.readLine();

                        if (!isEnabled()) {

                            logger.info("Interrupted, input ignored: '" + line + "'");
                            return;
                        }

                        if (line == null) {

                            logger.info("Lost the client");

                            stop();
                            return;
                        }

                        // Let's try to make sure that nobody tries to
                        // interfere with us.

                        // VT: FIXME: Just make sure we don't create a deadlock
                        // here

                        synchronized (this) {

                            parse(line);
                        }

                    } catch (InterruptedException ignored) {

                        // We've probably been stopped

                        logger.info("Interrupted");

                        if (isEnabled()) {

                            stop();
                        }

                        return;

                    } catch (SocketException sex) {

                        // Either a network error occured, or we've been stopped

                        logger.info("Socket error: " + sex.getMessage());

                        if (isEnabled()) {

                            stop();
                        }

                        return;

                    } catch (SSLException sslex) {

                        // I don't want to deal with SSL errors...

                        logger.info("SSL problem: " + sslex.getMessage());

                        if (isEnabled()) {

                            stop();
                        }

                        return;

                    } catch (Throwable t) {

                        if (t instanceof IOException && "Stream closed".equals(t.getMessage())) {

                            // This is normal, the client is gone

                            return;
                        }

                        logger.warn("Huh? Command received: '" + line + "'", t);

                        // The exception message is usually empty

                        send("E " + ((t.getMessage() == null) ? "Bad command" : t.getMessage()) + ": " + line);
                    }
                }
            }

            /**
             * Parse the received command.
             *
             * @param command Command to parse.
             *
             * @throws Throwable if anything goes wrong.
             */
            public final void parse(String command) throws Throwable {

                // VT: NOTE: Make sure the control flow is right. It is
                // if-elseif-...else, in other words, if there's a match, we
                // process the command and return

                if ("q".equalsIgnoreCase(command)) {

                    logger.info("Client disconnected");
                    stop();

                } else if ("".equalsIgnoreCase(command)) {

                    // Nothing, really, they're just checking if we're alive

                    // VT: FIXME: Make sure the heartbeat bug is fixed

                } else if ("heartbeat".equals(command)) {

                    send("OK");
                    return;
                } else {

                    parse2(command);
                }
            }

            /**
             * Parse the command further.
             *
             * @param command Command to parse.
             *
             * @throws Throwable if anything goes wrong.
             */
            protected abstract void parse2(String command) throws Throwable;
        }
    }
}
