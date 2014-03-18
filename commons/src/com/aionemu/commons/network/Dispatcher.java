package com.aionemu.commons.network;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import org.apache.log4j.Logger;

public abstract class Dispatcher extends Thread {

    protected final Logger log = Logger.getLogger(this.getClass());
    protected final Selector selector;
    protected final Object gate = new Object(); // Object on witch register vs selector.select are synchronized

    public Dispatcher(String name) throws IOException {
        super(name);
        this.selector = SelectorProvider.provider().openSelector();
    }

    abstract protected void dispatch() throws IOException;

    @Override
    public void run() {
        while(true) {
            try {
                dispatch();
                synchronized (gate) {
                }
            } catch (Exception e) {
                log.error("Dispatcher error! " + e, e);
            }
        }
    }
    
    public final Selector selector() {
        return this.selector;
    }
}