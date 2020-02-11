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
 *      Copyright 2011 ForgeRock AS
 */

package org.forgerock.i18n.jul;

import static org.forgerock.i18n.jul.MyTestMessages.MESSAGE_WITH_NO_ARGS;
import static org.forgerock.i18n.jul.MyTestMessages.MESSAGE_WITH_STRING;
import static org.forgerock.i18n.jul.MyTestMessages.MESSAGE_WITH_STRING_AND_NUMBER;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        when(mockedLogger.isLoggable(Level.WARNING)).thenReturn(true);
        LocalizedLogger logger = new LocalizedLogger(mockedLogger,
                Locale.ENGLISH);

        logger.warning(MESSAGE_WITH_NO_ARGS);

        verify(mockedLogger).warning("Message with no args");
    }

    /**
     * Tests logging of French no-args message with errors enabled.
     */
    @Test
    public void testFrenchNoArgsErrorEnabled() {
        Logger mockedLogger = mock(Logger.class);
        when(mockedLogger.isLoggable(Level.WARNING)).thenReturn(true);
        LocalizedLogger logger = new LocalizedLogger(mockedLogger,
                Locale.FRENCH);

        logger.warning(MESSAGE_WITH_NO_ARGS);

        verify(mockedLogger).warning("French message with no args");
    }

    /**
     * Tests logging of English no-args message with errors disabled.
     */
    @Test
    public void testEnglishNoArgsErrorDisabled() {
        Logger mockedLogger = mock(Logger.class);
        when(mockedLogger.isLoggable(Level.WARNING)).thenReturn(false);
        LocalizedLogger logger = new LocalizedLogger(mockedLogger,
                Locale.ENGLISH);

        logger.warning(MESSAGE_WITH_NO_ARGS);

        verify(mockedLogger, never()).warning(anyString());
    }

    /**
     * Tests logging of English one arg message with errors enabled.
     */
    @Test
    public void testEnglishOneArgErrorEnabled() {
        Logger mockedLogger = mock(Logger.class);
        when(mockedLogger.isLoggable(Level.WARNING)).thenReturn(true);
        LocalizedLogger logger = new LocalizedLogger(mockedLogger,
                Locale.ENGLISH);

        logger.warning(MESSAGE_WITH_STRING, "a string");

        verify(mockedLogger).warning("Arg1=a string");
    }

    /**
     * Tests logging of English two arg message with errors enabled.
     */
    @Test
    public void testEnglishTwoArgErrorEnabled() {
        Logger mockedLogger = mock(Logger.class);
        when(mockedLogger.isLoggable(Level.WARNING)).thenReturn(true);
        LocalizedLogger logger = new LocalizedLogger(mockedLogger,
                Locale.ENGLISH);

        logger.warning(MESSAGE_WITH_STRING_AND_NUMBER, "a string", 123);

        verify(mockedLogger).warning("Arg1=a string Arg2=123");
    }

}
