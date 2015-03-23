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

import javax.management.MXBean;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Exposes statistics on method call timings and rates to JMX monitoring.
 */
@MXBean
public interface MethodCallStatisticsMXBean {
    /**
     * The total number of times that this method has been called since monitoring began.
     */
    long getCallCount();

    /**
     * The units used for all time measurements.
     */
    TimeUnit getTimeUnit();

    /**
     * The minimum time that this method took to execute.
     */
    long getMinimumTime();

    /**
     * The maximum time that this method took to execute. This is almost always the first call to the method due to
     * classloading and compilation overheads.
     */
    long getMaximumTime();

    /**
     * The average (mean) time that this method took to execute. In most cases, the median and percentile
     * distributions are a more accurate reflection of system performance as the mean is influenced by outliers which
     * typically occur initially before JIT compilation has completed.
     */
    double getMeanTime();

    /**
     * The standard deviation of the average execution time.
     */
    double getStdDeviation();

    /**
     * The median (50th percentile) time taken to execute this method. 50% of all requests completed within this time.
     */
    long getMedianTime();

    /**
     * The 75th percentile execution time. 75% of all requests completed within this time.
     */
    long get75thPercentileTime();

    /**
     * The 90th percentile execution time. 90% of all requests completed within this time.
     */
    long get90thPercentileTime();

    /**
     * The 95th percentile execution time. 95% of all requests completed within this time.
     */
    long get95thPercentileTime();

    /**
     * The 98th percentile execution time. 98% of all requests completed within this time.
     */
    long get98thPercentileTime();

    /**
     * The 99th percentile execution time. 99% of all requests completed within this time.
     */
    long get99thPercentileTime();

    /**
     * The 99.9th percentile execution time. 99% of all requests completed within this time.
     */
    long get99Point9thPercentileTime();

    /**
     * The 99.99th percentile execution time. 99.99% of all requests completed within this time.
     */
    long get99Point99thPercentileTime();

    /**
     * Gets a raw summary dump of call time percentile distribution data, suitable for plotting.
     */
    String getPercentileDump();

    /**
     * The interval at which the implementation polls for updates from the underlying performance monitoring code, in
     * milliseconds. A value of -1 indicates continuous monitoring.
     */
    long getUpdateIntervalMillis();

    /**
     * Sets the interval at which to poll for updates from the underlying performance monitoring framework.
     *
     * @param intervalMillis the update interval in milliseconds. Must be greater than 0.
     */
    void setUpdateIntervalMillis(long intervalMillis);

    /**
     * Resets all statistics to zero.
     */
    void reset();

    /**
     * The timestamp at which monitoring began or the most recent call to {@link #reset()}.
     */
    Date getMonitoringStartTime();

    /**
     * The timestamp at which the statistics were last updated.
     */
    Date getLastUpdateTime();
}
