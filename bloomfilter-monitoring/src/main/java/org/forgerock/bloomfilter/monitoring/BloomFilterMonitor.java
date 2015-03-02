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

/**
 * Generic Bloom Filter JMX monitoring.
 */
public final class BloomFilterMonitor<T> implements BloomFilterMXBean, BloomFilter<T> {
    // 100,000 microseconds largest expected timing (100ms)
    private static final long LONGEST_EXPECTED_RESPONSE_TIME_MICROS = 100000L;
    // 3 significant digits = approx. 65kB storage per histogram
    private static final int SIGNIFICANT_DIGITS = 3;
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

    public ObjectInstance register(final MBeanServer mBeanServer, final String name)
            throws InstanceAlreadyExistsException, MBeanRegistrationException, MalformedObjectNameException {
        final ObjectName objectName = new ObjectName(name);
        try {
            return mBeanServer.registerMBean(this, objectName);
        } catch (NotCompliantMBeanException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public void register(final MBeanServer mBeanServer)
            throws InstanceAlreadyExistsException, MBeanRegistrationException{
        try {
            this.register(mBeanServer,
                    delegate.getClass().getPackage().getName() + ":type=" + delegate.getClass().getSimpleName());
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
    public BloomFilterStatistics getBloomFilterStatistics() {
        return getStatistics();
    }

    @Override
    public MethodCallStatistics getAddStatistics() {
        return summarize(addStats.copy());
    }

    @Override
    public MethodCallStatistics getAllAllStatistics() {
        return summarize(addAllStats.copy());
    }

    @Override
    public MethodCallStatistics getMightContainStatistics() {
        return summarize(mightContainStats.copy());
    }

    private MethodCallStatistics summarize(final AtomicHistogram histogram) {
        final ByteArrayOutputStream log = new ByteArrayOutputStream();
        try {
            histogram.outputPercentileDistribution(new PrintStream(log, true, "UTF-8"), 1.0d);

            return new MethodCallStatistics(histogram.getTotalCount(),
                    histogram.getMinValue(), histogram.getMaxValue(),
                    histogram.getValueAtPercentile(50.0d), histogram.getValueAtPercentile(95.0d),
                    histogram.getMean(), histogram.getStdDeviation(),
                    log.toString("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("Broken JVM: UTF-8 unknown encoding!");
        }
    }

}
