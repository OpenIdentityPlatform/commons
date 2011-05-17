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



import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * A factory of {@link LocalizedLogger} instances which obtains a SLF4J
 * {@link Logger} by calling the appropriate {@link LoggerFactory} method and
 * wrapping it in an instance of {@code LocalizedLogger}.
 */
public final class LocalizedLoggerFactory
{
  private final Locale locale;



  /**
   * Returns a localized logger factory which will create localized loggers for
   * the default locale.
   *
   * @return The localized logger factory.
   */
  public static LocalizedLoggerFactory getInstance()
  {
    return new LocalizedLoggerFactory(Locale.getDefault());
  }



  /**
   * Returns a localized logger factory which will create localized loggers for
   * the provided locale.
   *
   * @param locale
   *          The locale to which loggers created by the factory will localize
   *          all log messages.
   * @return The localized logger factory.
   */
  public static LocalizedLoggerFactory getInstance(Locale locale)
  {
    return new LocalizedLoggerFactory(locale);
  }



  // Private constructor.
  private LocalizedLoggerFactory(Locale locale)
  {
    this.locale = locale;
  }



  /**
   * Returns a localized logger which will forward log messages to an SLF4J
   * {@code Logger} obtained by calling {@link LoggerFactory#getLogger(Class)}.
   *
   * @param clazz
   *          The name of the wrapped SLF4J {@code Logger}.
   * @return The localized logger.
   * @see LoggerFactory#getLogger(Class)
   */
  public LocalizedLogger getLocalizedLogger(Class<?> clazz)
  {
    Logger logger = LoggerFactory.getLogger(clazz);
    return new LocalizedLogger(logger, locale);
  }



  /**
   * Returns a localized logger which will forward log messages to an SLF4J
   * {@code Logger} obtained by calling {@link LoggerFactory#getLogger(String)}.
   *
   * @param name
   *          The name of the wrapped SLF4J {@code Logger}.
   * @return The localized logger.
   * @see LoggerFactory#getLogger(String)
   */
  public LocalizedLogger getLocalizedLogger(String name)
  {
    Logger logger = LoggerFactory.getLogger(name);
    return new LocalizedLogger(logger, locale);
  }

}
