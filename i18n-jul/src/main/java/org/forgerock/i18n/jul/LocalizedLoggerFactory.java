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

package org.forgerock.i18n.jul;



import java.util.Locale;
import java.util.logging.Logger;



/**
 * A factory of {@link LocalizedLogger} instances which obtains a Java
 * {@link Logger} by calling the appropriate {@link Logger} factory method and
 * wrapping it in an instance of {@code LocalizedLogger}.
 */
public final class LocalizedLoggerFactory
{
  /**
   * Returns a localized logger factory which will create localized loggers for
   * the default locale.
   *
   * @return The localized logger factory.
   */
  public static LocalizedLoggerFactory getInstance()
  {
    // This can't be cached because the default locale can change.
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
  public static LocalizedLoggerFactory getInstance(final Locale locale)
  {
    return new LocalizedLoggerFactory(locale);
  }



  private final Locale locale;



  // Private constructor.
  private LocalizedLoggerFactory(final Locale locale)
  {
    this.locale = locale;
  }



  /**
   * Returns a localized logger which will forward log messages to an anonymous
   * Java {@code Logger} obtained by calling {@link Logger#getAnonymousLogger()}
   * .
   *
   * @return The localized logger.
   * @see Logger#getAnonymousLogger()
   */
  public LocalizedLogger getLocalizedAnonymousLogger()
  {
    final Logger logger = Logger.getAnonymousLogger();
    return new LocalizedLogger(logger, locale);
  }



  /**
   * Returns a localized logger which will forward log messages to the provided
   * Java {@code Logger}.
   *
   * @param logger
   *          The wrapped Java {@code Logger}.
   * @return The localized logger.
   */
  public LocalizedLogger getLocalizedLogger(final Logger logger)
  {
    return new LocalizedLogger(logger, locale);
  }



  /**
   * Returns a localized logger which will forward log messages to the named
   * Java {@code Logger} obtained by calling {@link Logger#getLogger(String)}.
   *
   * @param name
   *          The name of the wrapped Java {@code Logger}.
   * @return The localized logger.
   * @see Logger#getLogger(String)
   */
  public LocalizedLogger getLocalizedLogger(final String name)
  {
    final Logger logger = Logger.getLogger(name);
    return new LocalizedLogger(logger, locale);
  }

}
