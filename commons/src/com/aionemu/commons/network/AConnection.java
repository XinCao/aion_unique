package com.aionemu.commons.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import com.aionemu.commons.options.Assertion;

public abstract class AConnection {

    private final SocketChannel socketChannel;
    private final Dispatcher dispatcher;
    private SelectionKey key;
    protected boolean pendingClose;
    protected boolean isForcedClosing;
    protected boolean closed;
    protected final Object guard = new Object();
    public final ByteBuffer writeBuffer;
    public final ByteBuffer readBuffer;
    private final String ip;
    private boolean locked = false;

    public AConnection(SocketChannel sc, Dispatcher d) throws IOException {
        socketChannel = sc;
        dispatcher = d;
        writeBuffer = ByteBuffer.allocate(8192 * 2);
        writeBuffer.flip();
        writeBuffer.order(ByteOrder.LITTLE_ENDIAN);
        readBuffer = ByteBuffer.allocate(8192 * 2);
        readBuffer.order(ByteOrder.LITTLE_ENDIAN);
        ((AcceptReadWriteDispatcherImpl)dispatcher).register(socketChannel, SelectionKey.OP_READ, this);
        this.ip = socketChannel.socket().getInetAddress().getHostAddress();
    }

    public final void setKey(SelectionKey key) {
        this.key = key;
    }

    protected final void enableWriteInterest() {
        if (key.isValid()) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            key.selector().wakeup();
        }
    }

    private Dispatcher getDispatcher() {
        return dispatcher;
    }

    /**
     * @return SocketChannel representing this connection.
     */
    public SocketChannel getSocketChannel() {
        return socketChannel;
    }


    /**
     * 关闭连接
     * 
     * @param forced 
     */
    public final void close(boolean forced) {
        synchronized (guard) {
            if (isWriteDisabled()) {
                return;
            }
            isForcedClosing = forced;
            ((AcceptReadWriteDispatcherImpl)getDispatcher()).closeConnection(this);
        }
    }

    /**
     * This will only close the connection without taking care of the rest. May
     * be called only by Dispatcher Thread. Returns true if connection was not
     * closed before.
     *
     * @return true if connection was not closed before.
     */
    final boolean onlyClose() {
        /**
         * Test if this build should use assertion. If NetworkAssertion == false
         * javac will remove this code block
         */
        if (Assertion.NetworkAssertion) {
            assert Thread.currentThread() == dispatcher;
        }

        synchronized (guard) {
            if (closed) {
                return false;
            }
            try {
                if (socketChannel.isOpen()) {
                    socketChannel.close();
                    key.attach(null);
                    key.cancel();
                }
                closed = true;
            } catch (IOException ignored) {
            }
        }
        return true;
    }

    /**
     * @return True if this connection is pendingClose and not closed yet.
     */
    final boolean isPendingClose() {
        return pendingClose && !closed;
    }

    /**
     * @return True if write to this connection is possible.
     */
    protected final boolean isWriteDisabled() {
        return pendingClose || closed;
    }

    /**
     * @return IP address of this Connection.
     */
    public final String getIP() {
        return ip;
    }

    /**
     * Used only for PacketProcessor synchronization purpose. Return true if
     * locked successful - if wasn't locked before.
     *
     * @return locked
     */
    boolean tryLockConnection() {
        if (locked) {
            return false;
        }
        return locked = true;
    }

    /**
     * Used only for PacketProcessor synchronization purpose. Unlock this
     * connection.
     */
    void unlockConnection() {
        locked = false;
    }

    /**
     * @param data
     * @return True if data was processed correctly, False if some error
     * occurred and connection should be closed NOW.
     */
    abstract protected boolean processData(ByteBuffer data);

    /**
     * This method will be called by Dispatcher, and will be repeated till
     * return false.
     *
     * @param data
     * @return True if data was written to buffer, False indicating that there
     * are not any more data to write.
     */
    abstract protected boolean writeData(ByteBuffer data);

    /**
     * This method is called by Dispatcher when connection is ready to be
     * closed.
     *
     * @return time in ms after witch onDisconnect() method will be called.
     */
    abstract protected long getDisconnectionDelay();

    /**
     * This method is called by Dispatcher to inform that this connection was
     * closed and should be cleared. This method is called only once.
     */
    abstract protected void onDisconnect();

    /**
     * This method is called by NioServer to inform that NioServer is shouting
     * down. This method is called only once.
     */
    abstract protected void onServerClose();
}