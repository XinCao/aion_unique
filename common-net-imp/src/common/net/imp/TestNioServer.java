package common.net.imp;

import common.net.imp.core.IOServer;
import common.net.imp.util.DeadLockDetector;
import common.net.imp.util.ThreadPoolManager;

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