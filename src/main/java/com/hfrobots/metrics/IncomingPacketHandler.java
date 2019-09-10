package com.hfrobots.metrics;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.AllArgsConstructor;
import org.influxdb.InfluxDB;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
public class IncomingPacketHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final InfluxDB database;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        final ByteBuf buf = msg.content();
        final int rcvPktLength = buf.readableBytes();
        final byte[] rcvPktBuf = new byte[rcvPktLength];
        buf.readBytes(rcvPktBuf);

        List<Metric> metrics = new MetricsParser().parseMetric(new String(rcvPktBuf, "UTF-8"));

        for (Metric metric : metrics) {
            metric.persist(database);
        }
    }
}
