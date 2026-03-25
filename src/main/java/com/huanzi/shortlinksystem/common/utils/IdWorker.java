package com.huanzi.shortlinksystem.common.utils;

public class IdWorker {

    private static final long START_STAMP = 1704038400000L;
    private static final long SEQUENCE_BIT = 12;
    private static final long MACHINE_BIT = 5;
    private static final long DATACENTER_BIT = 5;
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BIT);
    private static final long MACHINE_LEFT = SEQUENCE_BIT;
    private static final long DATACENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
    private static final long TIMESTAMP_LEFT = DATACENTER_LEFT + DATACENTER_BIT;

    private final long datacenterId;
    private final long machineId;
    private long sequence;
    private long lastStamp = -1L;

    public IdWorker(long datacenterId, long machineId) {
        this.datacenterId = datacenterId;
        this.machineId = machineId;
    }

    public synchronized long nextId() {
        long currentStamp = System.currentTimeMillis();
        if (currentStamp < lastStamp) {
            throw new IllegalStateException("clock moved backwards");
        }
        if (currentStamp == lastStamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                currentStamp = waitNextMillis();
            }
        } else {
            sequence = 0L;
        }
        lastStamp = currentStamp;
        return (currentStamp - START_STAMP) << TIMESTAMP_LEFT
                | datacenterId << DATACENTER_LEFT
                | machineId << MACHINE_LEFT
                | sequence;
    }

    private long waitNextMillis() {
        long mill = System.currentTimeMillis();
        while (mill <= lastStamp) {
            mill = System.currentTimeMillis();
        }
        return mill;
    }
}
