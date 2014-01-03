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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.jaspi.utils;

import org.forgerock.auth.common.DebugLogger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;

public class DebugLoggerBufferTest {

    private DebugLoggerBuffer loggerBuffer;

    @BeforeMethod
    public void setUp() {
        loggerBuffer = new DebugLoggerBuffer();
    }

    @Test
    public void shouldBufferTraceLogMessage() {

        //Given
        DebugLogger logger = mock(DebugLogger.class);

        loggerBuffer.trace("MESSAGE");

        //When
        loggerBuffer.setDebugLogger(logger);

        //Then
        verify(logger).trace("MESSAGE");
    }

    @Test
    public void shouldBufferTraceLogMessageWithThrowable() {

        //Given
        DebugLogger logger = mock(DebugLogger.class);
        Throwable t = mock(Throwable.class);

        loggerBuffer.trace("MESSAGE", t);

        //When
        loggerBuffer.setDebugLogger(logger);

        //Then
        verify(logger).trace("MESSAGE", t);
    }

    @Test
    public void shouldBufferDebugLogMessage() {

        //Given
        DebugLogger logger = mock(DebugLogger.class);

        loggerBuffer.debug("MESSAGE");

        //When
        loggerBuffer.setDebugLogger(logger);

        //Then
        verify(logger).debug("MESSAGE");
    }

    @Test
    public void shouldBufferDebugLogMessageWithThrowable() {

        //Given
        DebugLogger logger = mock(DebugLogger.class);
        Throwable t = mock(Throwable.class);

        loggerBuffer.debug("MESSAGE", t);

        //When
        loggerBuffer.setDebugLogger(logger);

        //Then
        verify(logger).debug("MESSAGE", t);
    }

    @Test
    public void shouldBufferErrorLogMessage() {

        //Given
        DebugLogger logger = mock(DebugLogger.class);

        loggerBuffer.error("MESSAGE");

        //When
        loggerBuffer.setDebugLogger(logger);

        //Then
        verify(logger).error("MESSAGE");
    }

    @Test
    public void shouldBufferErrorLogMessageWithThrowable() {

        //Given
        DebugLogger logger = mock(DebugLogger.class);
        Throwable t = mock(Throwable.class);

        loggerBuffer.error("MESSAGE", t);

        //When
        loggerBuffer.setDebugLogger(logger);

        //Then
        verify(logger).error("MESSAGE", t);
    }

    @Test
    public void shouldBufferWarnLogMessage() {

        //Given
        DebugLogger logger = mock(DebugLogger.class);

        loggerBuffer.warn("MESSAGE");

        //When
        loggerBuffer.setDebugLogger(logger);

        //Then
        verify(logger).warn("MESSAGE");
    }

    @Test
    public void shouldBufferWarnLogMessageWithThrowable() {

        //Given
        DebugLogger logger = mock(DebugLogger.class);
        Throwable t = mock(Throwable.class);

        loggerBuffer.warn("MESSAGE", t);

        //When
        loggerBuffer.setDebugLogger(logger);

        //Then
        verify(logger).warn("MESSAGE", t);
    }

    @Test
    public void shouldTraceLogMessageWhenLoggerSet() {

        //Given
        DebugLogger logger = mock(DebugLogger.class);

        loggerBuffer.setDebugLogger(logger);

        //When
        loggerBuffer.trace("MESSAGE");

        //Then
        verify(logger).trace("MESSAGE");
    }

    @Test
    public void shouldTraceLogMessageWithThrowableWhenLoggerSet() {

        //Given
        DebugLogger logger = mock(DebugLogger.class);
        Throwable t = mock(Throwable.class);

        loggerBuffer.setDebugLogger(logger);

        //When
        loggerBuffer.trace("MESSAGE", t);

        //Then
        verify(logger).trace("MESSAGE", t);
    }

    @Test
    public void shouldDebugLogMessageWhenLoggerSet() {

        //Given
        DebugLogger logger = mock(DebugLogger.class);

        loggerBuffer.setDebugLogger(logger);

        //When
        loggerBuffer.debug("MESSAGE");

        //Then
        verify(logger).debug("MESSAGE");
    }

    @Test
    public void shouldDebugLogMessageWithThrowableWhenLoggerSet() {

        //Given
        DebugLogger logger = mock(DebugLogger.class);
        Throwable t = mock(Throwable.class);

        loggerBuffer.setDebugLogger(logger);

        //When
        loggerBuffer.debug("MESSAGE", t);

        //Then
        verify(logger).debug("MESSAGE", t);
    }

    @Test
    public void shouldErrorLogMessageWhenLoggerSet() {

        //Given
        DebugLogger logger = mock(DebugLogger.class);

        loggerBuffer.setDebugLogger(logger);

        //When
        loggerBuffer.error("MESSAGE");

        //Then
        verify(logger).error("MESSAGE");
    }

    @Test
    public void shouldErrorLogMessageWithThrowableWhenLoggerSet() {

        //Given
        DebugLogger logger = mock(DebugLogger.class);
        Throwable t = mock(Throwable.class);

        loggerBuffer.setDebugLogger(logger);

        //When
        loggerBuffer.error("MESSAGE", t);

        //Then
        verify(logger).error("MESSAGE", t);
    }

    @Test
    public void shouldWarnLogMessageWhenLoggerSet() {

        //Given
        DebugLogger logger = mock(DebugLogger.class);

        loggerBuffer.setDebugLogger(logger);

        //When
        loggerBuffer.warn("MESSAGE");

        //Then
        verify(logger).warn("MESSAGE");
    }

    @Test
    public void shouldWarnLogMessageWithThrowableWhenLoggerSet() {

        //Given
        DebugLogger logger = mock(DebugLogger.class);
        Throwable t = mock(Throwable.class);

        loggerBuffer.setDebugLogger(logger);

        //When
        loggerBuffer.warn("MESSAGE", t);

        //Then
        verify(logger).warn("MESSAGE", t);
    }

    @Test
    public void shouldLogAllMessages() {

        //Given
        DebugLogger logger = mock(DebugLogger.class);
        Throwable t = mock(Throwable.class);

        loggerBuffer.trace("MESSAGE");
        loggerBuffer.debug("MESSAGE");
        loggerBuffer.error("MESSAGE");
        loggerBuffer.warn("MESSAGE");
        loggerBuffer.trace("MESSAGE", t);
        loggerBuffer.debug("MESSAGE", t);
        loggerBuffer.error("MESSAGE", t);
        loggerBuffer.warn("MESSAGE", t);

        //When
        loggerBuffer.setDebugLogger(logger);

        //Then
        verify(logger).trace("MESSAGE");
        verify(logger).debug("MESSAGE");
        verify(logger).error("MESSAGE");
        verify(logger).warn("MESSAGE");
        verify(logger).trace("MESSAGE", t);
        verify(logger).debug("MESSAGE", t);
        verify(logger).error("MESSAGE", t);
        verify(logger).warn("MESSAGE", t);
    }
}
