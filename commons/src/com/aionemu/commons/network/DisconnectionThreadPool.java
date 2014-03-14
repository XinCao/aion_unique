package com.aionemu.commons.network;

public interface DisconnectionThreadPool {

    public void scheduleDisconnection(DisconnectionTask dt, long delay);

    public void waitForDisconnectionTasks();
}