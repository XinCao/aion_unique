package common.net.imp.core;

import com.aionemu.commons.network.NioServer;
import com.aionemu.commons.network.ServerCfg;
import common.net.imp.util.ThreadPoolManager;

public class IOServer {

    private final static NioServer instance;

    static {
        ServerCfg aionCfg = new ServerCfg("127.0.0.1", 8888, "Aion Connections", new AionConnectionFactoryImpl());
        instance = new NioServer(5, ThreadPoolManager.getInstance(), aionCfg);
    }

    public static NioServer getInstance() {
        return instance;
    }
}