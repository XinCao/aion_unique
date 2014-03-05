/*
 * This file is part of aion-unique <aion-unique.org>.
 *
 *  aion-unique is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  aion-unique is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with aion-unique.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.commons.utils.concurrent;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.log4j.Logger;

/**
 *  A handler for tasks that cannot be executed by a {@link ThreadPoolExecutor}
 * .
 * @author
 */
public final class AionRejectedExecutionHandler implements RejectedExecutionHandler {

    private static final Logger log = Logger.getLogger(AionRejectedExecutionHandler.class);

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        if (executor != null && !executor.isShutdown()) {
            executor.execute(r);
            return;
        }
        log.warn(r + " from " + executor, new RejectedExecutionException());
        if (Thread.currentThread().getPriority() > Thread.NORM_PRIORITY) {
            new Thread(r).start(); // start()可以协调系统的资源
        } else {
            r.run();
        }
    }
}