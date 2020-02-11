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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.audit.batch;

import java.util.concurrent.TimeUnit;

import org.forgerock.util.time.Duration;

/**
 * This class stores the common audit logging batch process configurations.
 *
 * Asynchronous audit event handlers write events to a queue allowing the producer thread to return
 * immediately rather than waiting for the audit event to be written to a file or socket.
 *
 * The queue is read by one or more consumer threads. When the queue is empty, these consumer threads
 * go into a polling (blocked) state until a new event is added to the queue.
 *
 * In order to allow these consumer threads to be shutdown, when waiting for events to appear in the
 * queue, these threads periodically awake to check for shutdown; by default, this period is set to
 * 100ms.
 *
 */
public final class CommonAuditBatchConfiguration {

    /**
     * Common Audit Batch log records queue polling timeout.
     * Details: {@link CommonAuditBatchConfiguration}
     */
    public static final long POLLING_TIMEOUT = 100L;

    /**
     * Common Audit Batch log records queue polling timeout unit.
     * Details: {@link CommonAuditBatchConfiguration}
     */
    public static final TimeUnit POLLING_TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

    /**
     * Common Audit Batch log records queue polling timeout as {@link org.forgerock.util.time.Duration}.
     * Details: {@link CommonAuditBatchConfiguration}
     */
    public static final Duration POLLING_INTERVAL = Duration.duration(POLLING_TIMEOUT, POLLING_TIMEOUT_UNIT);

    private CommonAuditBatchConfiguration() {
        //private constructor
    }
}
