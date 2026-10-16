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
 * Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.audit.retention;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.management.ManagementFactory;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TimestampFilenameFilterTest {

    private static final File DIRECTORY = new File("logs");
    private static final File INITIAL_FILE = new File(DIRECTORY, "ldap-access.audit.json");
    /** The suffix format OpenDJ configures for its JSON access log. */
    private static final DateTimeFormatter SUFFIX_FORMAT = DateTimeFormat.forPattern(".yyyyMMddHHmmss");
    private static final int ITERATIONS = 10_000;

    private final TimestampFilenameFilter filter = new TimestampFilenameFilter(INITIAL_FILE, null, SUFFIX_FORMAT);

    @DataProvider
    public Object[][] rotatedFileNames() {
        return new Object[][] {
            { "ldap-access.audit.json.20261015060744" },
            { "ldap-access.audit.json.20261015060744.gz" },
            // rotated twice with the same timestamp
            { "ldap-access.audit.json.20261015060744.1" },
            { "ldap-access.audit.json.20261015060744.12.gz" },
        };
    }

    @Test(dataProvider = "rotatedFileNames")
    public void shouldAcceptRotatedFile(final String name) {
        assertThat(filter.accept(DIRECTORY, name)).isTrue();
    }

    @DataProvider
    public Object[][] otherFileNames() {
        return new Object[][] {
            // the live file
            { "ldap-access.audit.json" },
            // files of other loggers in the same directory
            { "access" },
            { "access.20261015060744Z" },
            { "errors.20261015060744Z" },
            { "replication.20261015060744Z" },
            // archives
            { "ldap-access.audit.json.20261015060744.tar.gz" },
            { "ldap-access.audit.json.20261015060744.gz.gz" },
            // .gz is a compression suffix only at the end of the name
            { "ldap-access.audit.json.gz.20261015060744" },
            { "ldap-access.audit.json.2026.gz1015060744" },
            // second out of range
            { "ldap-access.audit.json.20261015060770" },
            // not a collision counter
            { "ldap-access.audit.json.20261015060744." },
            { "ldap-access.audit.json.20261015060744.1a" },
            { "ldap-access.audit.json.20261015060744.1.2" },
        };
    }

    @Test
    public void shouldRejectLiveFileWhoseNameEndsWithGzipSuffix() {
        final TimestampFilenameFilter gzFilter = new TimestampFilenameFilter(new File(DIRECTORY, "audit.gz"), null,
                SUFFIX_FORMAT);

        assertThat(gzFilter.accept(DIRECTORY, "audit.gz")).isFalse();
        assertThat(gzFilter.accept(DIRECTORY, "audit.gz.20261015060744.gz")).isTrue();
    }

    @Test(dataProvider = "otherFileNames")
    public void shouldRejectOtherFile(final String name) {
        assertThat(filter.accept(DIRECTORY, name)).isFalse();
    }

    @Test
    public void shouldMatchPrefix() {
        final TimestampFilenameFilter prefixFilter = new TimestampFilenameFilter(INITIAL_FILE, "prefix-",
                SUFFIX_FORMAT);

        assertThat(prefixFilter.accept(DIRECTORY, "prefix-ldap-access.audit.json.20261015060744")).isTrue();
        assertThat(prefixFilter.accept(DIRECTORY, "ldap-access.audit.json.20261015060744")).isFalse();
    }

    @Test
    public void shouldRejectFileOfAnotherLoggerWithoutAllocating() {
        final String[] names = { "access", "errors.20261015060744Z", "replication.20261015060744Z",
            "ldap-access.audit.jso" };

        final long allocated = allocatedBytesPerName(names);

        assertThat(allocated).isZero();
    }

    @Test
    public void shouldRejectMalformedTimestampAtNoMoreCostThanAcceptingOne() {
        final long rejecting = allocatedBytesPerName("ldap-access.audit.json.20261015060744.tar.gz");
        final long accepting = allocatedBytesPerName("ldap-access.audit.json.20261015060744.gz");

        // an exception and its stack trace would cost several times what a successful parse does
        assertThat(rejecting).isLessThanOrEqualTo(2 * accepting);
    }

    /** Returns the average number of bytes the current thread allocates to filter one of the names. */
    private long allocatedBytesPerName(final String... names) {
        if (!(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean)) {
            throw new SkipException("Thread allocation counter is not available");
        }
        final com.sun.management.ThreadMXBean threads =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!threads.isThreadAllocatedMemorySupported() || !threads.isThreadAllocatedMemoryEnabled()) {
            throw new SkipException("Thread allocation counter is not enabled");
        }
        final long threadId = Thread.currentThread().getId();
        // warm up, so that class loading and compilation do not count
        filter(names);
        threads.getThreadAllocatedBytes(threadId);
        final long before = threads.getThreadAllocatedBytes(threadId);
        final int accepted = filter(names);
        final long after = threads.getThreadAllocatedBytes(threadId);
        assertThat(accepted).isIn(0, ITERATIONS * names.length);
        return (after - before) / ((long) ITERATIONS * names.length);
    }

    private int filter(final String... names) {
        int accepted = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            for (final String name : names) {
                if (filter.accept(DIRECTORY, name)) {
                    accepted++;
                }
            }
        }
        return accepted;
    }
}
