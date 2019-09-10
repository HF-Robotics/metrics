package com.hfrobots.metrics;

import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.TimeUnit;

public class Foo {

    public static void mainBBBBB(String[] args) throws Exception {
        InfluxDB influxDB = InfluxDBFactory.connect("http://localhost:8086", "metrics", "metrics");
        influxDB.setDatabase("metrics");
        influxDB.enableBatch(BatchOptions.DEFAULTS);

        for (int i = 0; i < 50; i++) {
            influxDB.write(Point.measurement("cpu")
                    .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .addField("value", Math.random() * 400)
                    .build());
            Thread.sleep(12);
        }

        influxDB.close();
    }

    public static void main(String[] args) throws Exception {
        byte[] buf = new byte[256];
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        InetAddress address = null;
        try {
            address = InetAddress.getLocalHost();
            System.out.println(address);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < 50; i++) {
            buf = ("cpu:" + String.valueOf((Math.random() * 400)) + "|g").getBytes();

            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 8126);
            try {
                socket.send(packet);
                System.out.println(buf);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Thread.sleep(20);
        }

    }
}
