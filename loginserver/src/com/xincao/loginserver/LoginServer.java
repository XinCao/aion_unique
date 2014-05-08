/**
 * This file is part of aion-emu <aion-emu.com>.
 *
 * aion-emu is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * aion-emu is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * aion-emu. If not, see <http://www.gnu.org/licenses/>.
 */
package com.xincao.loginserver;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.services.LoggingService;
import com.aionemu.commons.utils.AEInfos;
import com.aionemu.commons.utils.ExitCode;
import com.xincao.loginserver.configs.Config;
import com.xincao.loginserver.controller.BannedIpController;
import com.xincao.loginserver.network.IOServer;
import com.xincao.loginserver.network.ncrypt.KeyGen;
import com.xincao.loginserver.utils.DeadLockDetector;
import com.xincao.loginserver.utils.ThreadPoolManager;
import com.xincao.loginserver.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author -Nemesiss-
 */
public class LoginServer {

    /**
     * Logger for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(LoginServer.class);

    /**
     * @param args
     */
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        LoggingService.init(); // 日志初始化
        Config.load();
        Util.printSection("DataBase"); // 数据库初始化
        DatabaseFactory.init();
        DAOManager.init();
        /**
         * Start deadlock detector that will restart server if deadlock happened
         */
        new DeadLockDetector(60, DeadLockDetector.RESTART).start(); // 检查死锁
        ThreadPoolManager.getInstance();
        /**
         * Initialize Key Generator
         */
        try {
            Util.printSection("KeyGen");
            KeyGen.init();
        } catch (Exception e) {
            log.error("Failed initializing Key Generator. Reason: " + e.getMessage(), e);
            System.exit(ExitCode.CODE_ERROR);
        }
        Util.printSection("GSTable");
        GameServerTable.load();
        Util.printSection("BannedIP");
        BannedIpController.load();
        Util.printSection("IOServer");
        IOServer.getInstance().connect();
        Runtime.getRuntime().addShutdownHook(Shutdown.getInstance());
        Util.printSection("System");
        AEInfos.printAllInfos();
        Util.printSection("LoginServerLog");
        log.info("AE Login Server started in " + (System.currentTimeMillis() - start) / 1000 + " seconds.");
    }
}