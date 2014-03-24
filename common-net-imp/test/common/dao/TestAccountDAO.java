package common.dao;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import common.dao.util.Util;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author caoxin
 */
public class TestAccountDAO {

    private static AccountDAO accountDAO;

    public TestAccountDAO() {
        Util.printSection("DataBase"); // 数据库初始化
        DatabaseFactory.init();
        DAOManager.init();
        accountDAO = DAOManager.getDAO(AccountDAO.class);
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testGetAccount() {
        System.out.println(accountDAO.getAccount("caoxin").toString());
    }
}