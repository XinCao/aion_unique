package com.aionemu.commons.network;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.aionemu.commons.network.packet.BaseClientPacket;

public class PacketProcessor<T extends AConnection> {

    private static final Logger log = Logger.getLogger(PacketProcessor.class.getName());
    private final static int reduceThreshold = 3;
    private final static int increaseThreshold = 50;
    private final Lock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final List<BaseClientPacket<T>> packets = new LinkedList<BaseClientPacket<T>>();
    private final List<Thread> threads = new ArrayList<Thread>();
    private final int minThreads;
    private final int maxThreads;

    public PacketProcessor(int minThreads, int maxThreads) {
        if (minThreads <= 0) {
            minThreads = 1;
        }
        if (maxThreads < minThreads) {
            maxThreads = minThreads;
        }

        this.minThreads = minThreads;
        this.maxThreads = maxThreads;

        if (minThreads != maxThreads) {
            startCheckerThread();
        }

        for (int i = 0; i < minThreads; i++) {
            newThread();
        }
    }

    private void startCheckerThread() {
        new Thread(new CheckerTask(), "PacketProcessor:Checker").start();
    }

    private boolean newThread() {
        if (threads.size() >= maxThreads) {
            return false;
        }

        String name = "PacketProcessor:" + threads.size();
        log.debug("Creating new PacketProcessor Thread: " + name);

        Thread t = new Thread(new PacketProcessorTask(), name);
        threads.add(t);
        t.start();

        return true;
    }

    private void killThread() {
        if (threads.size() < minThreads) {
            Thread t = threads.remove((threads.size() - 1));
            log.debug("Killing PacketProcessor Thread: " + t.getName());
            t.interrupt();
        }
    }

    public final void executePacket(BaseClientPacket<T> packet) {
        lock.lock();
        try {
            packets.add(packet);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    private BaseClientPacket<T> getFirstAviable() {
        for (;;) {
            while (packets.isEmpty()) {
                notEmpty.awaitUninterruptibly();
            }

            ListIterator<BaseClientPacket<T>> it = packets.listIterator();
            while (it.hasNext()) {
                BaseClientPacket<T> packet = it.next();
                if (packet.getConnection().tryLockConnection()) {
                    it.remove();
                    return packet;
                }
            }
            notEmpty.awaitUninterruptibly();
        }
    }

    private final class PacketProcessorTask implements Runnable {

        @Override
        public void run() {
            BaseClientPacket<T> packet = null;
            for (;;) {
                lock.lock();
                try {
                    if (packet != null) {
                        packet.getConnection().unlockConnection();
                    }

                    if (Thread.interrupted()) {
                        return;
                    }

                    packet = getFirstAviable();
                } finally {
                    lock.unlock();
                }
                packet.run();
            }
        }
    }

    private final class CheckerTask implements Runnable {

        private final int sleepTime = 60 * 1000;
        private int lastSize = 0;

        @Override
        public void run() {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
            }

            int sizeNow = packets.size();

            if (sizeNow < lastSize) {
                if (sizeNow < reduceThreshold) { // 降低阈值
                    killThread();
                }
            } else if (sizeNow > lastSize && sizeNow > increaseThreshold) {
                if (!newThread() && sizeNow >= increaseThreshold * 3) {
                    log
                            .info("Lagg detected! ["
                            + sizeNow
                            + " client packets are waiting for execution]. You should consider increasing PacketProcessor maxThreads or hardware upgrade.");
                }
            }
            lastSize = sizeNow;
        }
    }
}