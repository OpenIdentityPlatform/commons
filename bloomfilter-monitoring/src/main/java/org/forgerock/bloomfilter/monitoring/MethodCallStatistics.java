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

import java.beans.ConstructorProperties;

/**
 * Holds statistics on method call timings and counts for performance monitoring purposes. All timings are given in
 * microseconds unless otherwise stated.
 */
public final class MethodCallStatistics {
    private final long callCount;
    private final long minimumTime;
    private final long maximumTime;
    private final long medianTime;
    private final long ninetyFifthPercentile;
    private final double meanTime;
    private final double stdDeviationTime;
    private final String percentileLog;

    @ConstructorProperties({"callCount", "minimumTime", "maximumTime", "medianTime", "ninetyFifthPercentile",
            "meanTime", "stdDeviationTime", "percentileLog"})
    public MethodCallStatistics(final long callCount, final long minimumTime, final long maximumTime,
                                final long medianTime, final long ninetyFifthPercentile, final double meanTime,
                                final double stdDeviationTime, final String percentileLog) {
        this.callCount = callCount;
        this.minimumTime = minimumTime;
        this.maximumTime = maximumTime;
        this.medianTime = medianTime;
        this.ninetyFifthPercentile = ninetyFifthPercentile;
        this.meanTime = meanTime;
        this.stdDeviationTime = stdDeviationTime;
        this.percentileLog = percentileLog;
    }

    /**
     * Total number of times that this method has been called.
     */
    public long getCallCount() {
        return callCount;
    }

    /**
     * The minimum time taken to execute this method.
     */
    public long getMinimumTime() {
        return minimumTime;
    }

    /**
     * The maximum time taken to execute this method.
     */
    public long getMaximumTime() {
        return maximumTime;
    }

    /**
     * The median time taken to execute this method.
     */
    public long getMedianTime() {
        return medianTime;
    }

    /**
     * The time taken to execute 95% of all calls to this method.
     */
    public long getNinetyFifthPercentile() {
        return ninetyFifthPercentile;
    }

    /**
     * The average (mean) time taken to execute this method.
     */
    public double getMeanTime() {
        return meanTime;
    }

    /**
     * The standard deviation of all call timings for this method.
     */
    public double getStdDeviationTime() {
        return stdDeviationTime;
    }

    /**
     * A dump of the overall percentile distribution data in HdrHistogram log format. This can be fed into the
     * histogram analyzer service at <a href="http://hdrhistogram.github.io/HdrHistogram/plotFiles.html">
     *     http://hdrhistogram.github.io/HdrHistogram/plotFiles.html</a>.
     */
    public String getPercentileLog() {
        return percentileLog;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MethodCallStatistics that = (MethodCallStatistics) o;

        return callCount == that.callCount
                && maximumTime == that.maximumTime
                && Double.compare(that.meanTime, meanTime) == 0
                && medianTime == that.medianTime
                && minimumTime == that.minimumTime
                && ninetyFifthPercentile == that.ninetyFifthPercentile
                && Double.compare(that.stdDeviationTime, stdDeviationTime) == 0
                && percentileLog.equals(that.percentileLog);

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = (int) (callCount ^ (callCount >>> 32));
        result = 31 * result + (int) (minimumTime ^ (minimumTime >>> 32));
        result = 31 * result + (int) (maximumTime ^ (maximumTime >>> 32));
        result = 31 * result + (int) (medianTime ^ (medianTime >>> 32));
        result = 31 * result + (int) (ninetyFifthPercentile ^ (ninetyFifthPercentile >>> 32));
        temp = Double.doubleToLongBits(meanTime);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(stdDeviationTime);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + percentileLog.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "{ \"callCount\": " + callCount +
                ", \"minimumTime\": " + minimumTime +
                ", \"maximumTime\": " + maximumTime +
                ", \"medianTime\": " + medianTime +
                ", \"ninetyFifthPercentile\": " + ninetyFifthPercentile +
                ", \"meanTime\": " + meanTime +
                ", \"stdDeviationTime\": " + stdDeviationTime +
                " }";
    }
}
