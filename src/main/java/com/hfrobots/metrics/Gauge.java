package com.hfrobots.metrics;

import lombok.Builder;
import lombok.Value;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;

import java.util.concurrent.TimeUnit;

@Value
@Builder
public class Gauge implements Metric {
    private final long timestamp;

    private final String name;

    private final Double value;

    @Override
    public void persist(InfluxDB database) {
        database.write(
                Point.measurement(name)
                .time( timestamp, TimeUnit.MILLISECONDS)
                .addField("value", value )
                .build());
    }
}
