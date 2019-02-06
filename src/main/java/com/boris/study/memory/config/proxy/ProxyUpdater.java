package com.boris.study.memory.config.proxy;

import lombok.Data;

@Data
class ProxyUpdater {
    private final int UPDATE_DELAY, CONNECT_TIME_LIMIT;
    private long lastUpdated = System.currentTimeMillis();

    boolean isUpdateNeeded() {
        return (System.currentTimeMillis() - lastUpdated) / 1000 >= UPDATE_DELAY;
    }

    boolean updateProxy(ProxyResponse proxyResponse) {
        return ProxyResponse.NOT_RESPONDING != proxyResponse.updateResponseTime(CONNECT_TIME_LIMIT);
    }

    ProxyUpdater(int updateDelay, int connectTimeLimit) {
        UPDATE_DELAY = updateDelay;
        CONNECT_TIME_LIMIT = connectTimeLimit;
    }
}
