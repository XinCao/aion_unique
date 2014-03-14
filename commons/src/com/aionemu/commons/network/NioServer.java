package com.aionemu.commons.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.aionemu.commons.options.Assertion;

public class NioServer {

    private static final Logger log = Logger.getLogger(NioServer.class.getName());
    private final List<SelectionKey> serverChannelKeys = new ArrayList<SelectionKey>();
    private Dispatcher acceptDispatcher;
    private int currentReadWriteDispatcher;
    private Dispatcher[] readWriteDispatchers;
    private final DisconnectionThreadPool dcPool;
    private int readWriteThreads;
    private ServerCfg[] cfgs;

    public NioServer(int readWriteThreads, DisconnectionThreadPool dcPool, ServerCfg... cfgs) {
        if (Assertion.NetworkAssertion) { // Test if this build should use assertion and enforce it. If NetworkAssertion == false javac will remove this code block
            boolean assertionEnabled = false;
            assert assertionEnabled = true;
            if (!assertionEnabled) {
                throw new RuntimeException("This is unstable build. Assertion must be enabled! Add -ea to your start script or consider using stable build instead.");
            }
        }
        this.dcPool = dcPool;
        this.readWriteThreads = readWriteThreads;
        this.cfgs = cfgs;
    }

    public void connect() {
        try {
            this.initDispatchers(readWriteThreads, dcPool);
            for (ServerCfg cfg : cfgs) {
                ServerSocketChannel serverChannel = ServerSocketChannel.open();
                serverChannel.configureBlocking(false);
                InetSocketAddress isa;
                if ("*".equals(cfg.hostName)) {
                    isa = new InetSocketAddress(cfg.port);
                    log.info("Server listening on all available IPs on Port " + cfg.port + " for " + cfg.connectionName);
                } else {
                    isa = new InetSocketAddress(cfg.hostName, cfg.port);
                    log.info("Server listening on IP: " + cfg.hostName + " Port " + cfg.port + " for " + cfg.connectionName);
                }
                serverChannel.socket().bind(isa);
                SelectionKey acceptKey = ((AcceptDispatcherImpl)getAcceptDispatcher()).register(serverChannel, SelectionKey.OP_ACCEPT, new Acceptor(cfg.factory, this));
                serverChannelKeys.add(acceptKey);
            }
        } catch (Exception e) {
            log.fatal("NioServer Initialization Error: " + e, e);
            throw new Error("NioServer Initialization Error!");
        }
    }

    public final Dispatcher getAcceptDispatcher() {
        return acceptDispatcher;
    }

    public final Dispatcher getReadWriteDispatcher() {
        if (readWriteDispatchers.length == 1) {
            return readWriteDispatchers[0];
        }
        if (currentReadWriteDispatcher >= readWriteDispatchers.length) {
            currentReadWriteDispatcher = 0;
        }
        return readWriteDispatchers[currentReadWriteDispatcher++];
    }

    private void initDispatchers(int readWriteThreads, DisconnectionThreadPool dcPool) throws IOException {
        acceptDispatcher = new AcceptDispatcherImpl("Accept Dispatcher");
        acceptDispatcher.start();
        if (readWriteThreads <= 0) {
            acceptDispatcher = new AcceptReadWriteDispatcherImpl("AcceptReadWrite Dispatcher", dcPool);
            acceptDispatcher.start();
        } else {
            readWriteDispatchers = new Dispatcher[readWriteThreads];
            for (int i = 0; i < readWriteDispatchers.length; i++) {
                readWriteDispatchers[i] = new AcceptReadWriteDispatcherImpl("ReadWrite-" + i + " Dispatcher", dcPool);
                readWriteDispatchers[i].start();
            }
        }
    }

    public final int getActiveConnections() {
        int count = 0;
        if (readWriteDispatchers != null) {
            for (Dispatcher d : readWriteDispatchers) {
                count += d.selector().keys().size();
            }
        }
        return count;
    }

    public final void shutdown() {
        log.info("Closing ServerChannels...");
        try {
            for (SelectionKey key : serverChannelKeys) {
                key.cancel();
            }
            log.info("ServerChannel closed.");
        } catch (Exception e) {
            log.error("Error during closing ServerChannel, " + e, e);
        }
        this.notifyServerClose();
        try {
            Thread.sleep(1000);
        } catch (Throwable t) {
            log.warn("Nio thread was interrupted during shutdown", t);
        }
        log.info(" Active connections: " + getActiveConnections());
        log.info("Forced Disconnecting all connections...");
        this.closeAll();
        log.info(" Active connections: " + getActiveConnections());
        dcPool.waitForDisconnectionTasks();
        try {
            Thread.sleep(1000);
        } catch (Throwable t) {
            log.warn("Nio thread was interrupted during shutdown", t);
        }
    }

    /**
     * Calls onServerClose method for all active connections.
     */
    private void notifyServerClose() {
        if (readWriteDispatchers != null) {
            for (Dispatcher d : readWriteDispatchers) {
                for (SelectionKey key : d.selector().keys()) {
                    if (key.attachment() instanceof AConnection) {
                        ((AConnection) key.attachment()).onServerClose();
                    }
                }
            }
        }
    }

    /**
     * Close all active connections.
     */
    private void closeAll() {
        if (readWriteDispatchers != null) {
            for (Dispatcher d : readWriteDispatchers) {
                for (SelectionKey key : d.selector().keys()) {
                    if (key.attachment() instanceof AConnection) {
                        ((AConnection) key.attachment()).close(true);
                    }
                }
            }
        }
    }
}