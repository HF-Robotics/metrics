package com.hfrobots.metrics;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;

import java.net.*;
import java.util.Enumeration;

public class Ingest {
    public static void main(String[] args) throws Exception {
        InfluxDB influxDB = InfluxDBFactory.connect(
                "http://localhost:8086",
                "metrics", "metrics");
        influxDB.setDatabase("metrics");
        influxDB.enableBatch(BatchOptions.DEFAULTS);

        int port = 8126;

        final NioEventLoopGroup group = new NioEventLoopGroup();

        try {
            NetworkInterface ni = NetworkInterface.getByName("en1");
            Enumeration<InetAddress> addresses = ni.getInetAddresses();

            InetAddress localAddress = null;

            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (address instanceof Inet4Address){
                    localAddress = address;
                }
            }

            final Bootstrap b = new Bootstrap();

            b.group(group).channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInitializer<NioDatagramChannel>() {
                        @Override
                        public void initChannel(final NioDatagramChannel ch) throws Exception {

                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new IncomingPacketHandler(influxDB));
                        }
                    });

            // Bind and start to accept incoming connections.
            System.out.printf("waiting for messages...");
            b.bind(port).sync().channel().closeFuture().await();
        } finally {
            System.out.print("In Server Finally");
        }
    }
}
