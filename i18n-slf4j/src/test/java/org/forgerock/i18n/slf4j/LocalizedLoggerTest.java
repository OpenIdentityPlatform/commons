/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at legal/CDDLv1_0.txt or
 * http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at legal/CDDLv1_0.txt.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *      Copyright 2011 ForgeRock AS
 */

package org.forgerock.i18n.slf4j;



import static org.forgerock.i18n.slf4j.MyTestMessages.*;
import static org.mockito.Mockito.*;

import java.util.Locale;

import org.slf4j.Logger;
import org.testng.annotations.Test;



/**
 * Tests the {@code LocalizedLogger} class.
 */
@Test
public final class LocalizedLoggerTest
{

  /**
   * Tests logging of English no-args message with errors enabled.
   */
  @Test
  public void testEnglishNoArgsErrorEnabled()
  {
    Logger mockedLogger = mock(Logger.class);
    when(mockedLogger.isErrorEnabled()).thenReturn(true);
    LocalizedLogger logger = new LocalizedLogger(mockedLogger,
        Locale.ENGLISH);

    logger.error(MESSAGE_WITH_NO_ARGS);

    verify(mockedLogger).error("Message with no args");
  }



  /**
   * Tests logging of French no-args message with errors enabled.
   */
  @Test
  public void testFrenchNoArgsErrorEnabled()
  {
    Logger mockedLogger = mock(Logger.class);
    when(mockedLogger.isErrorEnabled()).thenReturn(true);
    LocalizedLogger logger = new LocalizedLogger(mockedLogger,
        Locale.FRENCH);

    logger.error(MESSAGE_WITH_NO_ARGS);

    verify(mockedLogger).error("French message with no args");
  }



  /**
   * Tests logging of English no-args message with errors disabled.
   */
  @Test
  public void testEnglishNoArgsErrorDisabled()
  {
    Logger mockedLogger = mock(Logger.class);
    when(mockedLogger.isErrorEnabled()).thenReturn(false);
    LocalizedLogger logger = new LocalizedLogger(mockedLogger,
        Locale.ENGLISH);

    logger.error(MESSAGE_WITH_NO_ARGS);

    verify(mockedLogger, never()).error(anyString());
  }



  /**
   * Tests logging of English one arg message with errors enabled.
   */
  @Test
  public void testEnglishOneArgErrorEnabled()
  {
    Logger mockedLogger = mock(Logger.class);
    when(mockedLogger.isErrorEnabled()).thenReturn(true);
    LocalizedLogger logger = new LocalizedLogger(mockedLogger,
        Locale.ENGLISH);

    logger.error(MESSAGE_WITH_STRING, "a string");

    verify(mockedLogger).error("Arg1=a string");
  }



  /**
   * Tests logging of English two arg message with errors enabled.
   */
  @Test
  public void testEnglishTwoArgErrorEnabled()
  {
    Logger mockedLogger = mock(Logger.class);
    when(mockedLogger.isErrorEnabled()).thenReturn(true);
    LocalizedLogger logger = new LocalizedLogger(mockedLogger,
        Locale.ENGLISH);

    logger.error(MESSAGE_WITH_STRING_AND_NUMBER, "a string", 123);

    verify(mockedLogger).error("Arg1=a string Arg2=123");
  }

}
