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

import org.HdrHistogram.AtomicHistogram;
import org.forgerock.bloomfilter.BloomFilter;
import org.forgerock.bloomfilter.BloomFilterStatistics;

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

/**
 * Generic Bloom Filter JMX monitoring.
 */
public final class BloomFilterMonitor<T> implements BloomFilterMXBean, BloomFilter<T> {
    private static final String LOG_CHARSET = "UTF-8";
    // 100,000 microseconds largest expected timing (100ms)
    private static final long LONGEST_EXPECTED_RESPONSE_TIME_MICROS = 100000L;
    // 3 significant digits = approx. 65kB storage per histogram
    private static final int SIGNIFICANT_DIGITS = 3;

    private static final String OBJECT_NAME_TEMPLATE = "%s:type=%s,name=%s";

    private final BloomFilter<T> delegate;

    private final AtomicHistogram addStats = new AtomicHistogram(LONGEST_EXPECTED_RESPONSE_TIME_MICROS,
            SIGNIFICANT_DIGITS);
    private final AtomicHistogram addAllStats = new AtomicHistogram(LONGEST_EXPECTED_RESPONSE_TIME_MICROS,
            SIGNIFICANT_DIGITS);
    private final AtomicHistogram mightContainStats = new AtomicHistogram(LONGEST_EXPECTED_RESPONSE_TIME_MICROS,
            SIGNIFICANT_DIGITS);

    public BloomFilterMonitor(final BloomFilter<T> delegate) {
        this.delegate = delegate;
    }

    public ObjectInstance register(final MBeanServer mBeanServer, final String packageName, final String instanceName)
            throws InstanceAlreadyExistsException, MBeanRegistrationException, MalformedObjectNameException {
        final ObjectName objectName = objectName(packageName, "BloomFilterMonitor", instanceName);
        try {
            // Register the method-call mbeans
            mBeanServer.registerMBean(new LiveMethodCallStatistics(addStats), objectName(packageName,
                    "BloomFilterMonitor.MethodCallStatistics", instanceName + ",method=add"));
            mBeanServer.registerMBean(new LiveMethodCallStatistics(addAllStats), objectName(packageName,
                    "BloomFilterMonitor.MethodCallStatistics", instanceName + ",method=addAll"));
            mBeanServer.registerMBean(new LiveMethodCallStatistics(mightContainStats), objectName(packageName,
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
    public boolean add(final T element) {
        final long startTime = System.nanoTime();
        try {
            return delegate.add(element);
        } finally {
            addStats.recordValue((System.nanoTime() - startTime) / 1000L);
        }
    }

    @Override
    public boolean addAll(final Collection<? extends T> elements) {
        final long startTime = System.nanoTime();
        try {
            return delegate.addAll(elements);
        } finally {
            addAllStats.recordValue((System.nanoTime() - startTime) / 1000L);
        }
    }

    @Override
    public boolean mightContain(final T element) {
        final long startTime = System.nanoTime();
        try {
            return delegate.mightContain(element);
        } finally {
            mightContainStats.recordValue((System.nanoTime() - startTime) / 1000L);
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



    private static final class LiveMethodCallStatistics implements MethodCallStatisticsMXBean {
        private final AtomicHistogram histogram;

        public LiveMethodCallStatistics(final AtomicHistogram histogram) {
            this.histogram = histogram;
        }

        @Override
        public long getCallCount() {
            return histogram.getTotalCount();
        }

        @Override
        public TimeUnit getTimeUnit() {
            return TimeUnit.MICROSECONDS;
        }

        @Override
        public long getMinimumTime() {
            return histogram.getMinValue();
        }

        @Override
        public long getMaximumTime() {
            return histogram.getMaxValue();
        }

        @Override
        public double getMeanTime() {
            return histogram.getMean();
        }

        @Override
        public double getStdDeviation() {
            return histogram.getStdDeviation();
        }

        @Override
        public long getMedianTime() {
            return histogram.getValueAtPercentile(50.0d);
        }

        @Override
        public long get75thPercentileTime() {
            return histogram.getValueAtPercentile(75.0d);
        }

        @Override
        public long get90thPercentileTime() {
            return histogram.getValueAtPercentile(90.0d);
        }

        @Override
        public long get95thPercentileTime() {
            return histogram.getValueAtPercentile(95.0d);
        }

        @Override
        public long get98thPercentileTime() {
            return histogram.getValueAtPercentile(98.0d);
        }

        @Override
        public long get99thPercentileTime() {
            return histogram.getValueAtPercentile(99.0d);
        }

        @Override
        public long get99Point9thPercentileTime() {
            return histogram.getValueAtPercentile(99.9d);
        }

        @Override
        public long get99Point99thPercentileTime() {
            return histogram.getValueAtPercentile(99.99d);
        }

        @Override
        public String getPercentileDump() {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                histogram.copy().outputPercentileDistribution(new PrintStream(out, true, LOG_CHARSET), 1.0d);
                return out.toString(LOG_CHARSET);
            } catch (UnsupportedEncodingException ex) {
                return "Unable to determine percentile log";
            }
        }
    }

    @Override
    public String toString() {
        return "BloomFilterMonitor{" +
                "stats=" + delegate.getStatistics() +
                ", add=" + addStats.getValueAtPercentile(99.0d) +
                ", addAll=" + addAllStats.getValueAtPercentile(99.0d) +
                ", mightContain=" + mightContainStats.getValueAtPercentile(99.0d) +
                '}';
    }
}
