package com.boris.study.memory.config.proxy;

import lombok.Data;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

@Data
class ProxyResponse implements Comparable<ProxyResponse> {
    static final int NOT_RESPONDING = -1;

    private String proxy;
    private int connectTime;

    // Must be in host:port format
    ProxyResponse(String proxy, int connectTimeout) throws IllegalArgumentException {
        if (!isValid(proxy))
            throw new IllegalArgumentException("Invalid proxy");

        this.proxy = proxy;
        connectTime = updateResponseTime(connectTimeout);
    }

    int updateResponseTime(int connectTimeout) {
        try {
            Proxy p = new Proxy(Proxy.Type.SOCKS,
                    new InetSocketAddress(
                            proxy.split(":")[0],
                            Integer.parseInt(proxy.split(":")[1])));
            URL url = new URL(ProxyService.CONNECTION_DESTINATION);
            URLConnection connection = url.openConnection(p);
            connection.setConnectTimeout(connectTimeout);

            long start = System.currentTimeMillis();
            connection.connect();
            return connectTime = (int) (System.currentTimeMillis() - start);

        } catch (IOException ignored) {
            return connectTime = NOT_RESPONDING;
        }
    }

    private boolean isValid(String proxy) {
        if (!proxy.contains(":")
                || proxy.indexOf(":") == 0
                || proxy.indexOf(":") == proxy.length() - 1
                || proxy.split(":").length != 2
                || proxy.matches(".*\\s.*")
                || proxy.contains(".."))
            return false;

        try {
            int port = Integer.valueOf(proxy.split(":")[1]);
            return port <= 65535 && port >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public int compareTo(ProxyResponse o) {
        if (connectTime == o.connectTime)
            return proxy.compareTo(o.proxy);
        if (connectTime == -1)
            return 1;
        if (o.connectTime == -1)
            return -1;
        return connectTime - o.connectTime;
    }

}
