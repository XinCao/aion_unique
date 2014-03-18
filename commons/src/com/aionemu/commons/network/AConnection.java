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

    protected final void enableWriteInterest() {
        if (key.isValid()) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            key.selector().wakeup();
        }
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
	 * 关闭套接字管道
	 */
    public final boolean onlyClose() {
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

	protected final boolean isPendingClose() {
        return pendingClose && !closed;
    }

    protected final boolean isWriteDisabled() {
        return pendingClose || closed;
    }

	public boolean tryLockConnection() {
        if (locked) {
            return false;
        }
        return locked = true;
    }

    public void unlockConnection() {
        locked = false;
    }

    private Dispatcher getDispatcher() {
        return dispatcher;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }
	
	public final void setKey(SelectionKey key) {
        this.key = key;
    }

    public final String getIP() {
        return ip;
    }

    abstract protected boolean processData(ByteBuffer data);

    abstract protected boolean writeData(ByteBuffer data);

    abstract protected long getDisconnectionDelay();

    abstract protected void onDisconnect();

    abstract protected void onServerClose();
}