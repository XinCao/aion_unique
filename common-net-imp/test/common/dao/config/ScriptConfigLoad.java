package common.dao.config;

import com.aionemu.commons.database.DatabaseConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author caoxin
 */
public class ScriptConfigLoad {

    public ScriptConfigLoad() {
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
    public void scriptConfigLoad() {
        DatabaseConfig.load();
    }
}