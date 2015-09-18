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
package org.forgerock.audit.events.handlers;

import static java.util.Arrays.*;

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.forgerock.audit.events.handlers.EventHandlerConfiguration.EventBufferingConfiguration;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.mockito.InOrder;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class BufferedAuditEventHandlerTest {

    private static final String TOPIC = "access";

    private static class SomeHandlerConfig extends EventHandlerConfiguration {}

    @Test
    public void shouldFlushWhenSizeIsReachedAndNoTimer() throws Exception {
        AuditEventHandler<SomeHandlerConfig> handler = mock(AuditEventHandler.class);
        Context context = new RootContext();
        BufferedAuditEventHandler<SomeHandlerConfig> bufferedHandler = new BufferedAuditEventHandler<>(handler);
        try {
            bufferedHandler.configure(config(0L, 3));

            AuditEventTopicState e1 = event(1, context);
            AuditEventTopicState e2 = event(2, context);
            AuditEventTopicState e3 = event(3, context);
            bufferedHandler.publishEvent(context, e1.getTopic(), e1.getEvent());
            bufferedHandler.publishEvent(context, e2.getTopic(), e2.getEvent());
            bufferedHandler.publishEvent(context, e3.getTopic(), e3.getEvent());

            // wait for callback to publish the events
            Thread.yield();
            Thread.sleep(10);

            assertThat(bufferedHandler.isBufferEmpty()).isTrue();
            verify(handler).configure(any(SomeHandlerConfig.class));
            verify(handler).publishEvents(asList(e1, e2, e3));
            verifyNoMoreInteractions(handler);
        } finally {
            bufferedHandler.close();
        }
    }

    @Test
    public void shouldFlushWhenMaxTimeIsReached() throws Exception {
        long maxTimeInMillis = 100L;
        AuditEventHandler<SomeHandlerConfig> handler = mock(AuditEventHandler.class);
        Context context = new RootContext();
        long startTime = System.currentTimeMillis();
        BufferedAuditEventHandler<SomeHandlerConfig> bufferedHandler = new BufferedAuditEventHandler<>(handler);
        try {
            bufferedHandler.configure(config(maxTimeInMillis, 1000));

            AuditEventTopicState e1 = event(1, context);
            AuditEventTopicState e2 = event(2, context);
            bufferedHandler.publishEvent(context, e1.getTopic(), e1.getEvent());
            bufferedHandler.publishEvent(context, e2.getTopic(), e2.getEvent());

            // wait for time-based flush
            long elapsedTime = 0;
            long timeout = maxTimeInMillis * 2;
            while (!bufferedHandler.isBufferEmpty() && elapsedTime < timeout) {
                Thread.sleep(10);
                elapsedTime = System.currentTimeMillis() - startTime;
            }

            assertThat(bufferedHandler.isBufferEmpty()).isTrue();
            assertThat(elapsedTime).isBetween(maxTimeInMillis, maxTimeInMillis + 50);
            verify(handler).configure(any(SomeHandlerConfig.class));
            verify(handler).publishEvents(Arrays.asList(e1, e2));
            verifyNoMoreInteractions(handler);
        } finally {
            bufferedHandler.close();
        }
    }

    @Test
    public void shouldFlushWhenMaxSizeIsReachedAndMaxTimeIsReached() throws Exception {
        long maxTimeInMillis = 200L;
        AuditEventHandler<SomeHandlerConfig> handler = mock(AuditEventHandler.class);
        Context context = new RootContext();
        BufferedAuditEventHandler<SomeHandlerConfig> bufferedHandler = new BufferedAuditEventHandler<>(handler);
        try {
            bufferedHandler.configure(config(maxTimeInMillis, 3));

            long startTime = System.currentTimeMillis();

            AuditEventTopicState e1 = event(1, context);
            AuditEventTopicState e2 = event(2, context);
            AuditEventTopicState e3 = event(3, context);
            bufferedHandler.publishEvent(context, e1.getTopic(), e1.getEvent());
            bufferedHandler.publishEvent(context, e2.getTopic(), e2.getEvent());
            bufferedHandler.publishEvent(context, e3.getTopic(), e3.getEvent());

            // buffer should be flushed because max size is reached
            assertThat(bufferedHandler.isBufferEmpty()).isTrue();

            // add another event and wait for time-based flush
            AuditEventTopicState e4 = new AuditEventTopicState(context, TOPIC, json(object(field("f4", "v4"))));
            bufferedHandler.publishEvent(context, e4.getTopic(), e4.getEvent());
            long elapsedTime = 0;
            long timeout = maxTimeInMillis * 2;
            while (!bufferedHandler.isBufferEmpty() && elapsedTime < timeout) {
                Thread.sleep(10);
                elapsedTime = System.currentTimeMillis() - startTime;
            }

            // check that everything happened in the expected order and time frame
            InOrder inOrder = inOrder(handler, handler);
            inOrder.verify(handler).configure(any(SomeHandlerConfig.class));
            inOrder.verify(handler).publishEvents(Arrays.asList(e1, e2, e3));
            inOrder.verify(handler).publishEvents(Arrays.asList(e4));
            verifyNoMoreInteractions(handler);
            assertThat(bufferedHandler.isBufferEmpty()).isTrue();
            assertThat(elapsedTime).isBetween(maxTimeInMillis, maxTimeInMillis + 50);
        } finally {
            bufferedHandler.close();
        }
    }

    @Test
    public void shouldNotFlushWhenMaxTimeIsReachedAndZeroEvent() throws Exception {
        long maxTimeInMillis = 100L;
        AuditEventHandler<SomeHandlerConfig> handler = mock(AuditEventHandler.class);
        BufferedAuditEventHandler<SomeHandlerConfig> bufferedHandler = new BufferedAuditEventHandler<>(handler);
        try {
            bufferedHandler.configure(config(0L, 3));

            Thread.sleep(maxTimeInMillis + 25);
            verify(handler, never()).publishEvents(anyList());
        } finally {
            bufferedHandler.close();
        }
    }

    /** Returns a buffering configuration. */
    private SomeHandlerConfig config(long maxTimeInMillis, int maxSize) {
        EventBufferingConfiguration config = new EventBufferingConfiguration();
        config.setEnabled(true);
        config.setMaxSize(maxSize);
        config.setMaxTime(maxTimeInMillis);
        SomeHandlerConfig handlerConfiguration = new SomeHandlerConfig();
        handlerConfiguration.setBufferingConfiguration(config);
        return handlerConfiguration;
    }

    /** Returns an event with a single field and value named after the provided number. */
    private AuditEventTopicState event(int number, Context context) {
        return new AuditEventTopicState(context, TOPIC, json(object(field("field" + number, "value" + number))));
    }
}
