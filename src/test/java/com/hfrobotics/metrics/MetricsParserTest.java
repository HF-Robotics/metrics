package com.hfrobotics.metrics;

import com.hfrobots.metrics.Gauge;
import com.hfrobots.metrics.Metric;
import com.hfrobots.metrics.MetricsParser;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class MetricsParserTest {
    private MetricsParser metricsParser = new MetricsParser();

    @Test
    public void entirelyWrongData() {
        // Can't parse, don't return any guages
        Assert.assertEquals(0, metricsParser.parseMetric("the answer is 42").size());
        Assert.assertEquals(0, metricsParser.parseMetric("foo1c").size());

        List<Metric> parsed = metricsParser.parseMetric("s_pos:100|g\ngarbage\ns_pos:200|g");
        Assert.assertEquals(2, parsed.size());
    }

    @Test
    public void validGaugeData() {
        List<Metric> parsed = metricsParser.parseMetric("s_pos:100|g");
        Assert.assertEquals(1, parsed.size());

        Metric metric = parsed.get(0);
        Assert.assertEquals(Gauge.class, metric.getClass());
        Assert.assertEquals("s_pos", metric.getName());
        Assert.assertEquals(100, ((Gauge)metric).getValue(), 0.01);

        parsed = metricsParser.parseMetric("s_pos:100|g\ns_pos:200|g");
        Assert.assertEquals(2, parsed.size());

        metric = parsed.get(0);
        Assert.assertEquals(Gauge.class, metric.getClass());
        Assert.assertEquals("s_pos", metric.getName());
        Assert.assertEquals(100, ((Gauge)metric).getValue(), 0.01);
    }

    @Test
    public void tags() {
        List<Metric> parsed = metricsParser.parseMetric("s_pos:100|g|#foo:bar,_ts:42");
        Assert.assertEquals(1, parsed.size());

        Metric metric = parsed.get(0);
        Assert.assertEquals(Gauge.class, metric.getClass());
        Assert.assertEquals("s_pos", metric.getName());
        Assert.assertEquals(100, ((Gauge)metric).getValue(), 0.01);

        Map<String, String> tags = ((Gauge) metric).getTags();
        Assert.assertTrue("Expected to find foo tag", tags.containsKey("foo"));
        Assert.assertEquals("bar", tags.get("foo"));
        Assert.assertEquals(42, metric.getTimestamp());
        Assert.assertFalse("Tags should not contain timestamp", tags.containsKey("_ts"));
    }
}
