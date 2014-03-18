package common.net.imp.network;

import common.net.imp.network.core.IOServer;

/**
 *
 * @author caoxin
 */
public class TestNioServer {

    public static void main(String... args) {
        IOServer.getInstance().connect();
    }
}