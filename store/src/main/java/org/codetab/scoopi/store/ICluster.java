package org.codetab.scoopi.store;

import java.util.Map;

import org.codetab.scoopi.config.Configs;

public interface ICluster {

    boolean start();

    boolean shutdown();

    Object getInstance(); // member instance

    String getMemberId(); // node/member id

    String getLeader();

    Map<String, byte[]> getMetricsHolder();

    Object getTxOptions(Configs configs);
}
