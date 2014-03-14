package com.aionemu.commons.network;

import com.aionemu.commons.options.Assertion;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;

public class AcceptReadWriteDispatcherImpl extends Dispatcher {
    
    private final DisconnectionThreadPool dcPool; // ThreadPool on witch disconnection tasks will be executed.
    private final List<AConnection> pendingClose = new ArrayList<AConnection>(); // List of connections that should be closed by this <code>Dispatcher</code> as soon as possible.

    public AcceptReadWriteDispatcherImpl(String name, DisconnectionThreadPool dcPool) throws IOException {
        super(name);
        this.dcPool = dcPool;
    }

    @Override
    protected void dispatch() throws IOException {
        int selected = selector.select();
        processPendingClose();
        if (selected != 0) {
            Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                selectedKeys.remove();
                if (!key.isValid()) {
                    continue;
                }
                switch (key.readyOps()) {
                    case SelectionKey.OP_READ:
                        this.read(key);
                        break;
                    case SelectionKey.OP_WRITE:
                        this.write(key);
                        break;
                    case SelectionKey.OP_READ | SelectionKey.OP_WRITE:
                        this.read(key);
                        if (key.isValid()) {
                            this.write(key);
                        }
                        break;
                }
            }
        }
    }

    public final void register(SelectableChannel ch, int ops, AConnection att) throws IOException {
        synchronized (gate) {
            selector.wakeup();
            att.setKey(ch.register(selector, ops, att));
        }
    }

    /**
     * 关闭连接
     * 
     * @param con 
     */
    public void closeConnection(AConnection con) {
        synchronized (pendingClose) {
            pendingClose.add(con);
        }
    }

    private void processPendingClose() {
        synchronized (pendingClose) {
            for (AConnection connection : pendingClose) {
                closeConnectionImpl(connection);
            }
            pendingClose.clear();
        }
    }

    private void closeConnectionImpl(AConnection con) {
        /**
         * Test if this build should use assertion. If NetworkAssertion == false
         * javac will remove this code block
         */
        if (Assertion.NetworkAssertion) {
            assert Thread.currentThread() == this;
        }

        if (con.onlyClose()) {
            dcPool.scheduleDisconnection(new DisconnectionTask(con), con.getDisconnectionDelay());
        }
    }

    /**
     * Read data from socketChannel represented by SelectionKey key. Parse and
     * Process data. Prepare buffer for next read.
     *
     * @param key
     */
    final void read(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        AConnection con = (AConnection) key.attachment();

        ByteBuffer rb = con.readBuffer;
        // Test if this build should use assertion. If NetworkAssertion == false javac will remove this code block
        if (Assertion.NetworkAssertion) {
            assert con.readBuffer.hasRemaining();
        }
        // Attempt to read off the channel
        int numRead;
        try {
            numRead = socketChannel.read(rb);
        } catch (IOException e) {
            closeConnectionImpl(con);
            return;
        }

        if (numRead == -1) {
            // Remote entity shut the socket down cleanly. Do the same from our end and cancel the channel.
            closeConnectionImpl(con);
            return;
        } else if (numRead == 0) {
            return;
        }

        rb.flip();
        while (rb.remaining() > 2 && rb.remaining() >= rb.getShort(rb.position())) {
            // got full message
            if (!parse(con, rb)) {
                closeConnectionImpl(con);
                return;
            }
        }
        if (rb.hasRemaining()) {
            con.readBuffer.compact();
            // Test if this build should use assertion. If NetworkAssertion == false javac will remove this code block
            if (Assertion.NetworkAssertion) {
                assert con.readBuffer.hasRemaining();
            }
        } else {
            rb.clear();
        }
    }

    /**
     * Parse data from buffer and prepare buffer for reading just one packet -
     * call processData(ByteBuffer b).
     *
     * @param con Connection
     * @param buf Buffer with packet data
     * @return True if packet was parsed.
     */
    private boolean parse(AConnection con, ByteBuffer buf) {
        short sz = 0;
        try {
            sz = buf.getShort();
            if (sz > 1) {
                sz -= 2;
            }
            ByteBuffer b = (ByteBuffer) buf.slice().limit(sz);
            b.order(ByteOrder.LITTLE_ENDIAN);
            // read message fully
            buf.position(buf.position() + sz);
            return con.processData(b);
        } catch (IllegalArgumentException e) {
            log.warn("Error on parsing input from client - account: " + con + " packet size: " + sz + " real size:"
                    + buf.remaining(), e);
            return false;
        }
    }

    /**
     * Write as much as possible data to socketChannel represented by
     * SelectionKey key. If all data were written key write interest will be
     * disabled.
     *
     * @param key
     */
    final void write(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        AConnection con = (AConnection) key.attachment();

        int numWrite;
        ByteBuffer wb = con.writeBuffer;
        // We have not writted data
        if (wb.hasRemaining()) {
            try {
                numWrite = socketChannel.write(wb);
            } catch (IOException e) {
                closeConnectionImpl(con);
                return;
            }
            if (numWrite == 0) {
                log.info("Write " + numWrite + " ip: " + con.getIP());
                return;
            }
            // Again not all data was send
            if (wb.hasRemaining()) {
                return;
            }
        }
        while (true) {
            wb.clear();
            boolean writeFailed = !con.writeData(wb);
            if (writeFailed) {
                wb.limit(0);
                break;
            }
            // Attempt to write to the channel
            try {
                numWrite = socketChannel.write(wb);
            } catch (IOException e) {
                closeConnectionImpl(con);
                return;
            }
            if (numWrite == 0) {
                log.info("Write " + numWrite + " ip: " + con.getIP());
                return;
            }
            // not all data was send
            if (wb.hasRemaining()) {
                return;
            }
        }
        // Test if this build should use assertion. If NetworkAssertion == false javac will remove this code block
        if (Assertion.NetworkAssertion) {
            assert !wb.hasRemaining();
        }
        // We wrote away all data, so we're no longer interested in writing on this socket.
        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        // We wrote all data so we can close connection that is "PandingClose"
        if (con.isPendingClose()) {
            closeConnectionImpl(con);
        }
    }
}