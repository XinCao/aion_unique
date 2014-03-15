package common.net.imp.network.util;

import org.apache.log4j.Logger;

public class ThreadUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final Logger log = Logger.getLogger(ThreadUncaughtExceptionHandler.class);

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error("Critical Error - Thread: " + t.getName() + " terminated abnormaly: " + e, e);
        if (e instanceof OutOfMemoryError) {
        }
    }
}