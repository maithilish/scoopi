package org.codetab.scoopi.metrics;

import static com.codahale.metrics.MetricRegistry.name;
import static java.util.Objects.isNull;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codetab.scoopi.metrics.serialize.Serializer;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

public class MetricsHelper {

    static final MetricRegistry METRICS =
            SharedMetricRegistries.getOrCreate("scoopi");

    private static final Logger LOG = LogManager.getLogger();

    public Timer getTimer(final Object clz, final String... names) {
        return METRICS.timer(getName(clz, names));
    }

    public Meter getMeter(final Object clz, final String... names) {
        return METRICS.meter(getName(clz, names));
    }

    public Counter getCounter(final Object clz, final String... names) {
        return METRICS.counter(getName(clz, names));
    }

    public <T> void registerGuage(final T value, final Object clz,
            final String... names) {
        String guageName = getName(clz, names);
        LOG.info("register metrics guage: {}", guageName);
        METRICS.register(guageName, new Gauge<T>() {
            @Override
            public T getValue() {
                return value;
            }
        });
    }

    public void clearGuages() {
        for (String key : METRICS.getGauges().keySet()) {
            METRICS.remove(key);
        }
    }

    private String getName(final Object clz, final String... names) {
        return name(clz.getClass().getSimpleName(), names);
    }

    public void initMetrics() {
        METRICS.counter("ParserCache.parser.cache.hit");
        METRICS.counter("ParserCache.parser.cache.miss");
        METRICS.counter("PageLoader.fetch.web");
        METRICS.counter("Task.system.error");
    }

    /**
     * Serialize Metrics Registry as JSON bytes array and add it to metrics
     * distributed map on specified intervals
     * @param memberId
     * @param metricsMap
     */
    public Serializer startJsonSerializer(final String memberId,
            final Map<String, byte[]> metricsMap, final int period) {
        Consumer<byte[]> outputter = metricsJsonData -> {
            try {
                metricsMap.put(memberId, metricsJsonData);
            } catch (Exception e) {
                LOG.error("unable to put metrics json to metrics map {}",
                        e.getMessage());
                LOG.debug("{}", e);
                // throw e;
            }
        };
        Serializer serializer = Serializer.forRegistry(METRICS)
                .consumer(outputter).convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.SECONDS)
                .shutdownExecutorOnStop(true).build();
        serializer.start(period, TimeUnit.SECONDS);
        return serializer;
    }

    public URL getURL(final String path) throws FileNotFoundException {
        URL url = MetricsHelper.class.getResource(path);
        if (isNull(url)) {
            url = ClassLoader.getSystemResource(path);
            if (isNull(url)) {
                Path fsPath = Paths.get(path);
                if (Files.exists(fsPath)) {
                    try {
                        url = fsPath.toUri().toURL();
                    } catch (MalformedURLException e) {
                        // can't test
                        LOG.debug("{}", e);
                    }
                }
            }
        }
        if (isNull(url)) {
            throw new FileNotFoundException(path);
        }
        return url;
    }

    public String printSnapshot(final String name, final long count,
            final Snapshot ss, final long divisor) {
        String cr = System.lineSeparator();
        StringBuilder b = new StringBuilder();
        b.append("metrics snapshot: ").append(name).append(cr);
        b.append("count: ").append(count).append(cr);
        b.append("min: ").append(format(ss.getMin(), divisor)).append(cr);
        b.append("max: ").append(format(ss.getMax(), divisor)).append(cr);
        b.append("mean: ").append(format(ss.getMean(), divisor)).append(cr);
        b.append("median: ").append(format(ss.getMedian(), divisor)).append(cr);
        b.append("75p: ").append(format(ss.get75thPercentile(), divisor))
                .append(cr);
        b.append("95p: ").append(format(ss.get95thPercentile(), divisor))
                .append(cr);
        b.append("98p: ").append(format(ss.get98thPercentile(), divisor))
                .append(cr);
        b.append("99p: ").append(format(ss.get99thPercentile(), divisor))
                .append(cr);
        b.append("99.9p: ").append(format(ss.get999thPercentile(), divisor))
                .append(cr);
        return b.toString();
    }

    private String format(final double value, final long divisor) {
        return String.format("%4.2f", value / divisor);
    }
}
