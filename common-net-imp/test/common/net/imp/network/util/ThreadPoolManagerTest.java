package common.net.imp.network.util;

import common.net.imp.util.ThreadPoolManager;
import com.aionemu.commons.network.DisconnectionTask;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author caoxin
 */
public class ThreadPoolManagerTest {

    private Logger log = Logger.getLogger(ThreadPoolManagerTest.class);
    private static ThreadPoolManager threadPoolManager;
    private static int id;

    public ThreadPoolManagerTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        threadPoolManager = ThreadPoolManager.getInstance();
        id = 1;
    }

    @AfterClass
    public static void tearDownClass() {
        id = 1;
    }

    @Before
    public void setUp() {
        log.info("test start = " + id);
    }

    @After
    public void tearDown() {
        log.info("test stop");
        id ++;
    }

    @Test
    public void testGetInstance() {
        if (!(threadPoolManager instanceof ThreadPoolManager)) {
            fail("The test case is a prototype.");
        }
    }

    @Test
    public void testSchedule() {
        if (threadPoolManager == null) {
            fail("threadPoolManager is null");
            return;
        }
        threadPoolManager.schedule(new Runnable() {
            @Override
            public void run() {
                log.info("testSchedule is operating normally");
            }
        }, 0);
    }

    @Test
    public void testScheduleAtFixedRate() {
        if (threadPoolManager == null) {
            fail("threadPoolManager is null");
            return;
        }
        long startT = System.currentTimeMillis();
        threadPoolManager.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                log.info("testScheduleAtFixedRate : time = { " + System.currentTimeMillis() + " }");
            }
        }, startT + 5000, 5000);
    }

    @Test
    public void testExecuteGsPacket() {
        threadPoolManager.executeGsPacket(new Runnable() {
            @Override
            public void run() {
                log.info("testExecuteGsPacket : time = { " + System.currentTimeMillis() + " }");
            }
        });
    }

    @Test
    public void testShutdown() {
        threadPoolManager.shutdown();
    }

//    @Test
//    public void testScheduleDisconnection() {
//    }

//    @Test
//    public void testWaitForDisconnectionTasks() {
//    }
}