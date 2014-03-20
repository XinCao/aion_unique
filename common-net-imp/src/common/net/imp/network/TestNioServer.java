package common.net.imp.network;

import common.net.imp.network.core.IOServer;
import common.net.imp.network.util.DeadLockDetector;
import common.net.imp.network.util.ThreadPoolManager;

/**
 *
 * @author caoxin
 */
public class TestNioServer {

    public static void main(String... args) {
        new DeadLockDetector(60, DeadLockDetector.RESTART).start(); // 检查死锁
        ThreadPoolManager.getInstance();
        IOServer.getInstance().connect();
    }
}