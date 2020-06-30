/*
 Copyright (c) 2019 HF Robotics (http://www.hfrobots.com)
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:
 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.
 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

package com.hfrobots.metrics;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;

public class Ingest {
    public static void main(String[] args) throws Exception {
        InfluxDB influxDB = null;

        Config conf = ConfigFactory.load();

        try {
            final String influxDbUrl = conf.getString("influxDbUrl");

            final String influxDbUsername = conf.getString("influxDbUsername");

            final String influxDbPassword = conf.getString("influxDbPassword");

            final String influxDbDatabase = conf.getString("influxDbDatabase");

            if (influxDbUrl == null || influxDbUrl.isEmpty()) {
                System.exit(-1);
            }

            if (influxDbUsername == null || influxDbUsername.isEmpty()) {
                System.exit(-1);
            }

            if (influxDbPassword == null || influxDbPassword.isEmpty()) {
                System.exit(-1);
            }

            if (influxDbDatabase == null || influxDbDatabase.isEmpty()) {
                System.exit(-1);
            }

            influxDB = InfluxDBFactory.connect(influxDbUrl, influxDbUsername, influxDbPassword);

            influxDB.setDatabase(influxDbDatabase);
            influxDB.enableBatch(BatchOptions.DEFAULTS);
        } catch (ConfigException.Missing missingConfig) {
            System.err.println("Missing or incomplete configuration, is config file provided via -Dconfig.file=path/to/config-file ?");
            missingConfig.printStackTrace(System.err);

            System.exit(1);
        }

        int port = 8126;

        final NioEventLoopGroup group = new NioEventLoopGroup();

        try {
            final Bootstrap b = new Bootstrap();

            InfluxDB finalInfluxDB = influxDB;

            b.group(group).channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInitializer<NioDatagramChannel>() {
                        @Override
                        public void initChannel(final NioDatagramChannel ch) throws Exception {

                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new IncomingPacketHandler(finalInfluxDB));
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
