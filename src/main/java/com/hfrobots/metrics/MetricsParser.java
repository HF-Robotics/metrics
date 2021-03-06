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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
public class MetricsParser {
    private static final String TS_TAG_NAME = "_ts";

    // From the StatsD docs:
    //
    // The format of exported metrics is UTF-8 text, with metrics separated by newlines.
    //
    // Metrics are generally of the form:
    //
    //  <metric name>:<value>|<type>
    //
    // The protocol allows for both integer and floating point values.
    //
    // Most implementations store values internally as a IEEE 754 double precision float,
    // but many implementations and graphing systems only support integer values. For
    // compatibility all values should be integers in the range (-2^53^, 2^53^).
    //
    // Gauges
    //
    //    A gauge is an instantaneous measurement of a value, like the gas gauge in a car.
    //    It differs from a counter by being calculated at the client rather than the server.
    //
    //    Valid gauge values are in the range [0, 2^64^)
    //
    //    <metric name>:<value>|g
    //
    //  Counters
    //
    //    A counter is a gauge calculated at the server. Metrics sent by the client increment
    //    or decrement the value of the gauge rather than giving its current value. Counters
    //    may also have an associated sample rate, given as a decimal of the number of samples
    //    per event count. For example, a sample rate of 1/10 would be exported as 0.1.
    //
    //    Valid counter values are in the range (-2^63^, 2^63^).
    //
    //    <metric name>:<value>|c[|@<sample rate>]
    //
    //  Timers
    //
    //    A timer is a measure of the number of milliseconds elapsed between a
    //    start and end time, for example the time to complete rendering of a web
    //    page for a user. Valid timer values are in the range [0, 2^64^).
    //
    //    <metric name>:<value>|ms
    //
    //  Histograms
    //
    //    A histogram is a measure of the distribution of timer values over time,
    //    calculated at the server. As the data exported for timers and histograms
    //    is the same, this is currently an alias for a timer. Valid histogram values
    //    are in the range [0, 2^64^).
    //
    //    <metric name>:<value>|h

    public List<Metric> parseMetric(String message) {
        if (message == null) {
            log.warn("Null metric found while parsing, ignoring");

            return ImmutableList.of();
        }

        String[] metricValues = message.split("\n");
        List<Metric> metricList = new ArrayList<>(metricValues.length);

        for (String metricValue : metricValues) {
            metricValue = metricValue.trim();

            int colonLocation = metricValue.indexOf(":");

            if (colonLocation == -1) { // not found
                log.warn("Metric missing ':' found while parsing, ignoring");

                continue;
            }

            int pipeLocation = metricValue.indexOf("|");

            if (pipeLocation == -1) { // not found
                log.warn("Metric missing '|' found while parsing, ignoring");

                continue;
            }

            if (pipeLocation < colonLocation) {
                log.warn("Metric has '|' before ':' found while parsing, ignoring");

                continue;
            }

            String name = metricValue.substring(0, colonLocation);
            String valueAsString = metricValue.substring(colonLocation + 1, pipeLocation);
            String type = metricValue.substring(pipeLocation + 1);

            if (name.length() == 0) {
                log.warn("Metric missing metric name found while parsing, ignoring");

                continue;
            }

            if (valueAsString.length() == 0) {
                log.warn("Metric missing value found while parsing, ignoring");

                continue;
            }

            if (type.length() == 0) {
                log.warn("Metric missing type found while parsing, ignoring");

                continue;
            }

            // FIXME: What if valueAsString is not numeric?
            try {
                Double.valueOf(valueAsString);
            } catch (NumberFormatException nfe) {
                log.error("{} has an invalid character, please use only number values ", valueAsString);
                continue;
            }

            int possibleTagIndex = type.indexOf("|#");

            Map<String, String> tags = ImmutableMap.of();

            if (possibleTagIndex != -1) {
                try {
                    String tagsString = type.substring(possibleTagIndex + 2);
                    type = type.substring(0, possibleTagIndex);

                    //noinspection UnstableApiUsage
                    tags = Splitter.on(",").trimResults().omitEmptyStrings()
                            .withKeyValueSeparator(":").split(tagsString);
                } catch (IllegalArgumentException illegalArgs) {
                    log.error("While parsing tags", illegalArgs);
                }
            }

            // normalize...Locale is here for a reason, ask me about lower/uppercase i
            type = type.toLowerCase(Locale.US);

            long timestamp;

            String possibleTimestamp = tags.get(TS_TAG_NAME);

            if (possibleTimestamp != null && !possibleTimestamp.isEmpty()) {
                //noinspection ConstantConditions
                tags = Maps.filterEntries(tags, entry -> !TS_TAG_NAME.equals(entry.getKey()));

                try {
                    timestamp = Long.parseLong(possibleTimestamp);
                } catch (NumberFormatException nfe) {
                    log.error("Timestamp (_ts) tag {} has an invalid character, please use only number values ", possibleTimestamp);
                    timestamp = System.currentTimeMillis();
                }
            } else {
                timestamp = System.currentTimeMillis();
            }

            switch (type) {
                case "g":
                    metricList.add(Gauge.builder()
                            .name(name)
                            .value(Double.valueOf(valueAsString))
                            .timestamp(timestamp)
                            .tags(tags)
                            .build());
                case "ms":
                    break;
                case "h":
                    break;
                case "c":
                    // FIXME: If we want to support sample rate for "c", what further parsing do we have to do?

                    break;
                default:
                    System.out.println(String.format("Name: %s, value: %s, type: %s", name, valueAsString, type));
                    break;
            }

        }

        return metricList;
    }
}
