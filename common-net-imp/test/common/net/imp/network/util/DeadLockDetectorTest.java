package common.net.imp.network.util;

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
public class DeadLockDetectorTest {
    
    private DeadLockDetector deadLockDetector;
    public DeadLockDetectorTest() {
        deadLockDetector = new DeadLockDetector(60, DeadLockDetector.RESTART);
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

    @Test(timeout = 1000)
    public void testRun() {
        long startT = System.currentTimeMillis();
        deadLockDetector.start();
        long haveUsedT = System.currentTimeMillis() - startT;
        if (haveUsedT > 1) {
            fail("Running out of time!");
        }
    }
}