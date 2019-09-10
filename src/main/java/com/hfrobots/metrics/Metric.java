package com.hfrobots.metrics;

import org.influxdb.InfluxDB;

public interface Metric {
    String getName();

    long getTimestamp();

    void persist(InfluxDB database);
}
