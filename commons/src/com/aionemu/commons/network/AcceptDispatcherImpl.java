package com.aionemu.commons.network;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Iterator;

public class AcceptDispatcherImpl extends Dispatcher {

    public AcceptDispatcherImpl(String name) throws IOException {
        super(name);
    }

    @Override
    protected void dispatch() throws IOException {
        if (selector.select() != 0) {
            Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                selectedKeys.remove();
                if (key.isValid()) {
                    this.accept(key);
                }
            }
        }
    }
    
    public final SelectionKey register(SelectableChannel ch, int ops, Acceptor att) throws IOException {
        synchronized (gate) {
            selector.wakeup();
            return ch.register(selector, ops, att);
        }
    }

    public final void accept(SelectionKey key) {
        try {
            ((Acceptor) key.attachment()).accept(key);
        } catch (Exception e) {
            log.error("Error while accepting connection: +" + e, e);
        }
    }
}