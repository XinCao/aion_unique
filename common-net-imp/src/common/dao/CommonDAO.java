package common.dao;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import common.dao.util.Util;

/**
 *
 * @author caoxin
 */
public class CommonDAO {

    public static void main(String ...args) {
        Util.printSection("DataBase"); // 数据库初始化
        DatabaseFactory.init();
        DAOManager.init();
    }
}