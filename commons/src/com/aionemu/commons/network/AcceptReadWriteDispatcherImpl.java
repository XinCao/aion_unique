package com.aionemu.commons.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AcceptReadWriteDispatcherImpl extends Dispatcher {
    
    private final DisconnectionThreadPool dcPool;
    private final List<AConnection> pendingClose = new ArrayList<AConnection>();

    public AcceptReadWriteDispatcherImpl(String name, DisconnectionThreadPool dcPool) throws IOException {
        super(name);
        this.dcPool = dcPool;
    }

    @Override
    protected void dispatch() throws IOException {
        int selected = selector.select();
        this.processPendingClose();
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
        if (con.onlyClose()) {
            dcPool.scheduleDisconnection(new DisconnectionTask(con), con.getDisconnectionDelay());
        }
    }

    private void read(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        AConnection con = (AConnection) key.attachment();
        ByteBuffer rb = con.readBuffer;
        int numRead;
        try {
            numRead = socketChannel.read(rb);
        } catch (IOException e) {
            closeConnectionImpl(con);
            return;
        }
        if (numRead == -1) {
            closeConnectionImpl(con);
            return;
        } else if (numRead == 0) {
            return;
        }
        rb.flip();
        while (rb.remaining() > 2 && rb.remaining() >= rb.getShort(rb.position())) { // 读取是否为一个整包（也可能大于一个整包（多个包），因此这里会使用循环）
            if (!parse(con, rb)) { // 判断包是否合法
                closeConnectionImpl(con);
                return;
            }
        }
        if (rb.hasRemaining()) {
            con.readBuffer.compact(); // 将缓冲区的当前位置和界限之间的字节复制到缓冲区的开始处（为下一个包准备）
        } else {
            rb.clear();
        }
    }

    private boolean parse(AConnection con, ByteBuffer buf) {
        short sz = 0;
        try {
            sz = buf.getShort();
            if (sz > 1) {
                sz -= 2;
            }
            ByteBuffer b = (ByteBuffer) buf.slice().limit(sz); // 创建新的缓冲区
            b.order(ByteOrder.LITTLE_ENDIAN); // 小端模式，高字节存储在高地址
            buf.position(buf.position() + sz); // 写一个包数据开始处
            return con.processData(b);
        } catch (IllegalArgumentException e) {
            log.warn("Error on parsing input from client - account: " + con + " packet size: " + sz + " real size:" + buf.remaining(), e);
            return false;
        }
    }

    private void write(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        AConnection con = (AConnection) key.attachment();
        int numWrite;
        ByteBuffer wb = con.writeBuffer;
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
            if (wb.hasRemaining()) { // 不能被写的数据
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
            if (wb.hasRemaining()) { // 不能被写的数据
                return;
            }
        }
        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        if (con.isPendingClose()) {
            closeConnectionImpl(con);
        }
    }
}