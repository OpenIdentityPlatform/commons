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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 *      Copyright 2011-2014 ForgeRock AS
 */

package org.forgerock.i18n.slf4j;

import static org.fest.assertions.Assertions.*;
import static org.forgerock.i18n.slf4j.MyTestMessages.*;
import static org.mockito.Mockito.*;

import java.util.Locale;

import org.forgerock.i18n.LocalizableMessage;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.testng.annotations.Test;

/**
 * Tests the {@code LocalizedLogger} class.
 */
@Test
public final class LocalizedLoggerTest {

    /**
     * Tests logging of English no-args message with errors enabled.
     */
    @Test
    public void testEnglishNoArgsErrorEnabled() {
        Logger mockedLogger = mock(Logger.class);
        when(mockedLogger.isErrorEnabled()).thenReturn(true);
        LocalizedLogger logger = new LocalizedLogger(mockedLogger, Locale.ENGLISH);

        logger.error(MESSAGE_WITH_NO_ARGS);

        verify(mockedLogger).error(isA(LocalizedMarker.class), eq("Message with no args"));
    }

    /**
     * Tests logging of French no-args message with errors enabled.
     */
    @Test
    public void testFrenchNoArgsErrorEnabled() {
        Logger mockedLogger = mock(Logger.class);
        when(mockedLogger.isErrorEnabled()).thenReturn(true);
        LocalizedLogger logger = new LocalizedLogger(mockedLogger, Locale.FRENCH);

        logger.error(MESSAGE_WITH_NO_ARGS);

        verify(mockedLogger).error(isA(LocalizedMarker.class), eq("French message with no args"));
    }

    /**
     * Tests logging of English no-args message with errors disabled.
     */
    @Test
    public void testEnglishNoArgsErrorDisabled() {
        Logger mockedLogger = mock(Logger.class);
        when(mockedLogger.isErrorEnabled()).thenReturn(false);
        LocalizedLogger logger = new LocalizedLogger(mockedLogger, Locale.ENGLISH);

        logger.error(MESSAGE_WITH_NO_ARGS);

        verify(mockedLogger, never()).error(anyString());
        verify(mockedLogger, never()).error(isA(LocalizedMarker.class), anyString());
    }

    /**
     * Tests logging of English one arg message with errors enabled.
     */
    @Test
    public void testEnglishOneArgErrorEnabled() {
        Logger mockedLogger = mock(Logger.class);
        when(mockedLogger.isErrorEnabled()).thenReturn(true);
        LocalizedLogger logger = new LocalizedLogger(mockedLogger, Locale.ENGLISH);

        logger.error(MESSAGE_WITH_STRING, "a string");

        verify(mockedLogger).error(isA(LocalizedMarker.class), eq("Arg1=a string"));
    }

    /**
     * Tests logging of English two arg message with errors enabled.
     */
    @Test
    public void testEnglishTwoArgErrorEnabled() {
        Logger mockedLogger = mock(Logger.class);
        when(mockedLogger.isErrorEnabled()).thenReturn(true);
        LocalizedLogger logger = new LocalizedLogger(mockedLogger, Locale.ENGLISH);

        logger.error(MESSAGE_WITH_STRING_AND_NUMBER, "a string", 123);

        verify(mockedLogger).error(isA(LocalizedMarker.class), eq("Arg1=a string Arg2=123"));
    }

    /**
     * Test that a LocalizedMarker is automatically added when logging.
     */
    @Test
    public void testLocalizedMarker() {
        Logger mockedLogger = mock(Logger.class);
        when(mockedLogger.isDebugEnabled()).thenReturn(true);
        LocalizedLogger logger = new LocalizedLogger(mockedLogger, Locale.ENGLISH);

        logger.debug(MESSAGE_WITH_NO_ARGS);

        ArgumentCaptor<LocalizedMarker> argument = ArgumentCaptor.forClass(LocalizedMarker.class);
        verify(mockedLogger).debug(argument.capture(), eq("Message with no args"));
        LocalizableMessage message = argument.getValue().getMessage();
        assertThat(message.resourceName()).isEqualTo(MyTestMessages.resourceName());
        assertThat(message.toString()).isEqualTo("Message with no args");
    }

    /**
     * Test trace logging with format string and two arguments.
     */
    @Test
    public void testTrace2Args() {
        Logger mockedLogger = mock(Logger.class);
        when(mockedLogger.isTraceEnabled()).thenReturn(true);
        LocalizedLogger logger = new LocalizedLogger(mockedLogger, Locale.ENGLISH);

        logger.trace("Message with two args: %s, %d", "s1", 1);

        String expectedMessage = "Message with two args: s1, 1";
        ArgumentCaptor<LocalizedMarker> argument = ArgumentCaptor.forClass(LocalizedMarker.class);
        verify(mockedLogger).trace(argument.capture(), eq(expectedMessage));
        LocalizableMessage message = argument.getValue().getMessage();
        assertThat(message.resourceName()).isNull();
        assertThat(message.toString()).isEqualTo(expectedMessage);
    }

    /**
     * Test trace logging with format string, four arguments and a throwable.
     */
    @Test
    public void testTrace4ArgsAndThrowable() {
        Logger mockedLogger = mock(Logger.class);
        when(mockedLogger.isTraceEnabled()).thenReturn(true);
        LocalizedLogger logger = new LocalizedLogger(mockedLogger, Locale.ENGLISH);

        logger.traceException(new IllegalStateException("ex"),
            "Message with 4 args: %s, %d, %s, %s", "s1", 1, "s2", "s3");

        String expectedMessage = "Message with 4 args: s1, 1, s2, s3";
        ArgumentCaptor<LocalizedMarker> argument = ArgumentCaptor.forClass(LocalizedMarker.class);
        verify(mockedLogger).trace(argument.capture(), eq(expectedMessage), isA(IllegalStateException.class));
        LocalizableMessage message = argument.getValue().getMessage();
        assertThat(message.resourceName()).isNull();
        assertThat(message.toString()).isEqualTo(expectedMessage);

    }

}
