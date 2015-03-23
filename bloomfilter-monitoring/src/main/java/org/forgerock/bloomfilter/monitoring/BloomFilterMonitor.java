/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.bloomfilter.monitoring;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;
import org.forgerock.bloomfilter.BloomFilter;
import org.forgerock.bloomfilter.BloomFilterStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generic Bloom Filter JMX monitoring.
 */
public final class BloomFilterMonitor<T> implements BloomFilterMXBean, BloomFilter<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BloomFilterMonitor.class);

    private static final String LOG_CHARSET = "UTF-8";
    // 1,000,000 microseconds largest expected timing (1 second)
    private static final long LONGEST_EXPECTED_RESPONSE_TIME_MICROS = MICROSECONDS.convert(1, SECONDS);
    // 3 significant digits = approx. 89kB storage per histogram
    private static final int SIGNIFICANT_DIGITS = 3;

    private static final String OBJECT_NAME_TEMPLATE = "%s:type=%s,name=%s";

    private final BloomFilter<T> delegate;

    private final LiveMethodCallStatistics addStats = new LiveMethodCallStatistics("add");
    private final LiveMethodCallStatistics addAllStats = new LiveMethodCallStatistics("addAll");
    private final LiveMethodCallStatistics mightContainStats = new LiveMethodCallStatistics("mightContain");

    public BloomFilterMonitor(final BloomFilter<T> delegate) {
        this.delegate = delegate;
    }

    public ObjectInstance register(final MBeanServer mBeanServer, final String packageName, final String instanceName)
            throws InstanceAlreadyExistsException, MBeanRegistrationException, MalformedObjectNameException {
        final ObjectName objectName = objectName(packageName, "BloomFilterMonitor", instanceName);
        try {
            // Register the method-call mbeans
            mBeanServer.registerMBean(addStats, objectName(packageName,
                    "BloomFilterMonitor.MethodCallStatistics", instanceName + ",method=add"));
            mBeanServer.registerMBean(addAllStats, objectName(packageName,
                    "BloomFilterMonitor.MethodCallStatistics", instanceName + ",method=addAll"));
            mBeanServer.registerMBean(mightContainStats, objectName(packageName,
                    "BloomFilterMonitor.MethodCallStatistics", instanceName + ",method=mightContain"));

            return mBeanServer.registerMBean(this, objectName);
        } catch (NotCompliantMBeanException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private ObjectName objectName(final String packageName, final String type, final String instanceName)
            throws MalformedObjectNameException {
        return new ObjectName(String.format(Locale.US, OBJECT_NAME_TEMPLATE, packageName, type, instanceName));
    }

    public void register(final MBeanServer mBeanServer)
            throws InstanceAlreadyExistsException, MBeanRegistrationException{
        try {
            this.register(mBeanServer,
                    delegate.getClass().getPackage().getName(), delegate.getClass().getSimpleName());
        } catch (MalformedObjectNameException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public void register() throws InstanceAlreadyExistsException, MBeanRegistrationException {
        this.register(ManagementFactory.getPlatformMBeanServer());
    }

    @Override
    public void add(final T element) {
        final long startTime = System.nanoTime();
        try {
            delegate.add(element);
        } finally {
            addStats.recordValue(System.nanoTime() - startTime, NANOSECONDS);
        }
    }

    @Override
    public void addAll(final Collection<? extends T> elements) {
        final long startTime = System.nanoTime();
        try {
            delegate.addAll(elements);
        } finally {
            addAllStats.recordValue(System.nanoTime() - startTime, NANOSECONDS);
        }
    }

    @Override
    public boolean mightContain(final T element) {
        final long startTime = System.nanoTime();
        try {
            return delegate.mightContain(element);
        } finally {
            mightContainStats.recordValue(System.nanoTime() - startTime, NANOSECONDS);
        }
    }

    @Override
    public BloomFilterStatistics getStatistics() {
        return delegate.getStatistics();
    }

    @Override
    public double getConfiguredFalsePositiveProbability() {
        return getStatistics().getConfiguredFalsePositiveProbability();
    }

    @Override
    public double getExpectedFalsePositiveProbability() {
        return getStatistics().getExpectedFalsePositiveProbability();
    }

    @Override
    public long getCurrentCapacity() {
        return getStatistics().getCapacity();
    }

    @Override
    public long getEstimatedRemainingCapacity() {
        return getStatistics().getEstimatedRemainingCapacity();
    }

    @Override
    public long getMemorySizeKB() {
        return getStatistics().getBitSize() / (8l * 1024l);
    }

    @Override
    public Date getExpiryTime() {
        return new Date(getStatistics().getExpiryTime());
    }

    /**
     * Maintains live on-going statistics on method call timing latencies. An HdrHistogram {@link Recorder} is used
     * to keep track of live performance data. A snapshot is taken periodically according to the update interval and
     * copied into an ongoing histogram.
     * <p/>
     * No attempt is currently made to compensate for coordinated omission, so the worst case latencies may be
     * over-optimistic under heavy load (i.e., when the latency exceeds the expected interval between operations).
     */
    private static final class LiveMethodCallStatistics implements MethodCallStatisticsMXBean {

        private final String name;
        private final Recorder recorder;
        private final AtomicLong lastSnapshotTime;

        private volatile long updateIntervalMillis = TimeUnit.SECONDS.toMillis(30);
        private volatile Histogram snapshot;
        private volatile long lastResetTime;

        private final Histogram overallHistogram = new Histogram(LONGEST_EXPECTED_RESPONSE_TIME_MICROS,
                SIGNIFICANT_DIGITS);

        public LiveMethodCallStatistics(final String name) {
            this.name = name;
            this.recorder = new Recorder(LONGEST_EXPECTED_RESPONSE_TIME_MICROS, SIGNIFICANT_DIGITS);
            this.snapshot = recorder.getIntervalHistogram();
            this.lastSnapshotTime = new AtomicLong(System.currentTimeMillis());
            this.lastResetTime = lastSnapshotTime.get();
        }

        void recordValue(long timing, TimeUnit unit) {
            try {
                recorder.recordValue(getTimeUnit().convert(timing, unit));
            } catch (IndexOutOfBoundsException ex) {
                LOGGER.warn("Method call time out of bounds for histogram: method={}, timing={} ({})", name,
                        timing, unit);

            }
        }

        @Override
        public long getCallCount() {
            return getSnapshot().getTotalCount();
        }

        @Override
        public TimeUnit getTimeUnit() {
            return MICROSECONDS;
        }

        @Override
        public long getMinimumTime() {
            return getSnapshot().getMinValue();
        }

        @Override
        public long getMaximumTime() {
            return getSnapshot().getMaxValue();
        }

        @Override
        public double getMeanTime() {
            return getSnapshot().getMean();
        }

        @Override
        public double getStdDeviation() {
            return getSnapshot().getStdDeviation();
        }

        @Override
        public long getMedianTime() {
            return getSnapshot().getValueAtPercentile(50.0d);
        }

        @Override
        public long get75thPercentileTime() {
            return getSnapshot().getValueAtPercentile(75.0d);
        }

        @Override
        public long get90thPercentileTime() {
            return getSnapshot().getValueAtPercentile(90.0d);
        }

        @Override
        public long get95thPercentileTime() {
            return getSnapshot().getValueAtPercentile(95.0d);
        }

        @Override
        public long get98thPercentileTime() {
            return getSnapshot().getValueAtPercentile(98.0d);
        }

        @Override
        public long get99thPercentileTime() {
            return getSnapshot().getValueAtPercentile(99.0d);
        }

        @Override
        public long get99Point9thPercentileTime() {
            return getSnapshot().getValueAtPercentile(99.9d);
        }

        @Override
        public long get99Point99thPercentileTime() {
            return getSnapshot().getValueAtPercentile(99.99d);
        }

        @Override
        public String getPercentileDump() {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                getSnapshot().outputPercentileDistribution(new PrintStream(out, true, LOG_CHARSET), 1.0d);
                return out.toString(LOG_CHARSET);
            } catch (UnsupportedEncodingException ex) {
                return "Unable to determine percentile log";
            }
        }

        private Histogram getSnapshot() {
            return getSnapshot(false);
        }

        private Histogram getSnapshot(boolean forceUpdate) {
            long lastSnapshot = lastSnapshotTime.get();
            if (forceUpdate || System.currentTimeMillis() - updateIntervalMillis > lastSnapshot) {
                if (lastSnapshotTime.compareAndSet(lastSnapshot, System.currentTimeMillis())) {
                    snapshot = recorder.getIntervalHistogram(snapshot);
                    synchronized (overallHistogram) {
                        overallHistogram.add(snapshot);
                    }
                }
            }
            return overallHistogram;
        }

        @Override
        public long getUpdateIntervalMillis() {
            return updateIntervalMillis;
        }

        @Override
        public void setUpdateIntervalMillis(final long intervalMillis) {
            this.updateIntervalMillis = intervalMillis;
        }

        @Override
        public void reset() {
            synchronized (overallHistogram) {
                overallHistogram.reset();
                lastResetTime = System.currentTimeMillis();
            }
        }

        @Override
        public Date getMonitoringStartTime() {
            return new Date(lastResetTime);
        }

        @Override
        public Date getLastUpdateTime() {
            return new Date(lastSnapshotTime.get());
        }

        @Override
        public String toString() {
            final long callCount = getSnapshot(true).getTotalCount();
            final StringBuilder sb = new StringBuilder()
                    .append("{ \"count\": ").append(callCount);
            if (callCount > 0) {
                sb.append(", \"units\": \"").append(getTimeUnit()).append('\"');
                sb.append(", \"min\": ").append(getMinimumTime());
                sb.append(", \"median\": ").append(getMedianTime());
                sb.append(", \"75%\": ").append(get75thPercentileTime());
                sb.append(", \"90%\": ").append(get90thPercentileTime());
                sb.append(", \"95%\": ").append(get95thPercentileTime());
                sb.append(", \"98%\": ").append(get98thPercentileTime());
                sb.append(", \"99%\": ").append(get99thPercentileTime());
                sb.append(", \"99.9%\": ").append(get99Point9thPercentileTime());
                sb.append(", \"99.99%\": ").append(get99Point99thPercentileTime());
                sb.append(", \"max\": ").append(getMaximumTime());
                sb.append(", \"mean\": ").append(getMeanTime());
                sb.append(", \"std.dev.\": ").append(getStdDeviation());
            }
            return sb.append(" }").toString();
        }
    }

    @Override
    public String toString() {
        return "{ " +
                "\"statistics\": " + delegate.getStatistics() +
                ", \"add\": " + addStats +
                ", \"addAll\": " + addAllStats +
                ", \"mightContain\": " + mightContainStats +
                " }";
    }
}
