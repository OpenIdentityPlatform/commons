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

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.i18n.LocalizableMessageDescriptor.*;
import org.slf4j.Logger;
import org.slf4j.Marker;



/**
 * A logger implementation which formats and localizes messages before
 * forwarding them to an underlying SLF4J {@link Logger}. For performance
 * reasons this implementation will only localize and format messages if logging
 * has been enabled for the associated log level and marker (if present).
 */
public final class LocalizedLogger
{
  private final Logger logger;
  private final Locale locale;



  /**
   * Creates a new localized logger which will log localizable messages to the
   * provided SLF4J {@code Logger} in the specified locale.
   *
   * @param logger
   *          The underlying SLF4J {@code Logger} wrapped by this logger.
   * @param locale
   *          The locale to which this logger will localize all log messages.
   */
  LocalizedLogger(Logger logger, Locale locale)
  {
    this.locale = locale;
    this.logger = logger;
  }



  /**
   * Logs an error message.
   *
   * @param d
   *          The message descriptor.
   * @see org.slf4j.Logger#error(java.lang.String)
   */
  public void error(Arg0 d)
  {
    if (logger.isErrorEnabled())
    {
      logger.error(d.get().toString(locale));
    }
  }



  /**
   * Logs an error message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @see org.slf4j.Logger#error(java.lang.String)
   */
  public <T1> void error(Arg1<T1> d, T1 a1)
  {
    if (logger.isErrorEnabled())
    {
      logger.error(d.get(a1).toString(locale));
    }
  }



  /**
   * Logs an error message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @see org.slf4j.Logger#error(java.lang.String)
   */
  public <T1, T2> void error(Arg2<T1, T2> d, T1 a1, T2 a2)
  {
    if (logger.isErrorEnabled())
    {
      logger.error(d.get(a1, a2).toString(locale));
    }
  }



  /**
   * Logs an error message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @see org.slf4j.Logger#error(java.lang.String)
   */
  public <T1, T2, T3> void error(Arg3<T1, T2, T3> d, T1 a1, T2 a2,
      T3 a3)
  {
    if (logger.isErrorEnabled())
    {
      logger.error(d.get(a1, a2, a3).toString(locale));
    }
  }



  /**
   * Logs an error message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @see org.slf4j.Logger#error(java.lang.String)
   */
  public <T1, T2, T3, T4> void error(Arg4<T1, T2, T3, T4> d, T1 a1,
      T2 a2, T3 a3, T4 a4)
  {
    if (logger.isErrorEnabled())
    {
      logger.error(d.get(a1, a2, a3, a4).toString(locale));
    }
  }



  /**
   * Logs an error message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @see org.slf4j.Logger#error(java.lang.String)
   */
  public <T1, T2, T3, T4, T5> void error(Arg5<T1, T2, T3, T4, T5> d,
      T1 a1, T2 a2, T3 a3, T4 a4, T5 a5)
  {
    if (logger.isErrorEnabled())
    {
      logger.error(d.get(a1, a2, a3, a4, a5).toString(locale));
    }
  }



  /**
   * Logs an error message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @see org.slf4j.Logger#error(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6> void error(
      Arg6<T1, T2, T3, T4, T5, T6> d, T1 a1, T2 a2, T3 a3, T4 a4,
      T5 a5, T6 a6)
  {
    if (logger.isErrorEnabled())
    {
      logger.error(d.get(a1, a2, a3, a4, a5, a6).toString(locale));
    }
  }



  /**
   * Logs an error message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @see org.slf4j.Logger#error(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7> void error(
      Arg7<T1, T2, T3, T4, T5, T6, T7> d, T1 a1, T2 a2, T3 a3, T4 a4,
      T5 a5, T6 a6, T7 a7)
  {
    if (logger.isErrorEnabled())
    {
      logger
          .error(d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
    }
  }



  /**
   * Logs an error message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param <T8>
   *          The type of the eighth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @param a8
   *          The eighth message argument.
   * @see org.slf4j.Logger#error(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7, T8> void error(
      Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, T1 a1, T2 a2, T3 a3,
      T4 a4, T5 a5, T6 a6, T7 a7, T8 a8)
  {
    if (logger.isErrorEnabled())
    {
      logger.error(d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(
          locale));
    }
  }



  /**
   * Logs an error message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param <T8>
   *          The type of the eighth message argument.
   * @param <T9>
   *          The type of the ninth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @param a8
   *          The eighth message argument.
   * @param a9
   *          The ninth message argument.
   * @see org.slf4j.Logger#error(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void error(
      Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, T1 a1, T2 a2,
      T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9)
  {
    if (logger.isErrorEnabled())
    {
      logger.error(d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9)
          .toString(locale));
    }
  }



  /**
   * Logs an error message.
   *
   * @param d
   *          The message descriptor.
   * @param args
   *          The message arguments.
   * @see org.slf4j.Logger#error(java.lang.String)
   */
  public void error(ArgN d, Object... args)
  {
    if (logger.isErrorEnabled())
    {
      logger.error(d.get(args).toString(locale));
    }
  }



  /**
   * Logs an error message using the provided {@code Marker}.
   *
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @see org.slf4j.Logger#error(org.slf4j.Marker, java.lang.String)
   */
  public void error(Marker m, Arg0 d)
  {
    if (logger.isErrorEnabled(m))
    {
      logger.error(m, d.get().toString(locale));
    }
  }



  /**
   * Logs an error message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @see org.slf4j.Logger#error(java.lang.String)
   */
  public <T1> void error(Marker m, Arg1<T1> d, T1 a1)
  {
    if (logger.isErrorEnabled(m))
    {
      logger.error(m, d.get(a1).toString(locale));
    }
  }



  /**
   * Logs an error message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @see org.slf4j.Logger#error(java.lang.String)
   */
  public <T1, T2> void error(Marker m, Arg2<T1, T2> d, T1 a1, T2 a2)
  {
    if (logger.isErrorEnabled(m))
    {
      logger.error(m, d.get(a1, a2).toString(locale));
    }
  }



  /**
   * Logs an error message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @see org.slf4j.Logger#error(java.lang.String)
   */
  public <T1, T2, T3> void error(Marker m, Arg3<T1, T2, T3> d, T1 a1,
      T2 a2, T3 a3)
  {
    if (logger.isErrorEnabled(m))
    {
      logger.error(m, d.get(a1, a2, a3).toString(locale));
    }
  }



  /**
   * Logs an error message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @see org.slf4j.Logger#error(java.lang.String)
   */
  public <T1, T2, T3, T4> void error(Marker m,
      Arg4<T1, T2, T3, T4> d, T1 a1, T2 a2, T3 a3, T4 a4)
  {
    if (logger.isErrorEnabled(m))
    {
      logger.error(m, d.get(a1, a2, a3, a4).toString(locale));
    }
  }



  /**
   * Logs an error message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @see org.slf4j.Logger#error(java.lang.String)
   */
  public <T1, T2, T3, T4, T5> void error(Marker m,
      Arg5<T1, T2, T3, T4, T5> d, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5)
  {
    if (logger.isErrorEnabled(m))
    {
      logger.error(m, d.get(a1, a2, a3, a4, a5).toString(locale));
    }
  }



  /**
   * Logs an error message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @see org.slf4j.Logger#error(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6> void error(Marker m,
      Arg6<T1, T2, T3, T4, T5, T6> d, T1 a1, T2 a2, T3 a3, T4 a4,
      T5 a5, T6 a6)
  {
    if (logger.isErrorEnabled(m))
    {
      logger.error(m, d.get(a1, a2, a3, a4, a5, a6).toString(locale));
    }
  }



  /**
   * Logs an error message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @see org.slf4j.Logger#error(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7> void error(Marker m,
      Arg7<T1, T2, T3, T4, T5, T6, T7> d, T1 a1, T2 a2, T3 a3, T4 a4,
      T5 a5, T6 a6, T7 a7)
  {
    if (logger.isErrorEnabled(m))
    {
      logger.error(m,
          d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
    }
  }



  /**
   * Logs an error message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param <T8>
   *          The type of the eighth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @param a8
   *          The eighth message argument.
   * @see org.slf4j.Logger#error(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7, T8> void error(Marker m,
      Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, T1 a1, T2 a2, T3 a3,
      T4 a4, T5 a5, T6 a6, T7 a7, T8 a8)
  {
    if (logger.isErrorEnabled(m))
    {
      logger.error(m,
          d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
    }
  }



  /**
   * Logs an error message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param <T8>
   *          The type of the eighth message argument.
   * @param <T9>
   *          The type of the ninth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @param a8
   *          The eighth message argument.
   * @param a9
   *          The ninth message argument.
   * @see org.slf4j.Logger#error(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void error(Marker m,
      Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, T1 a1, T2 a2,
      T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9)
  {
    if (logger.isErrorEnabled(m))
    {
      logger.error(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9)
          .toString(locale));
    }
  }



  /**
   * Logs an error message.
   *
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param args
   *          The message arguments.
   * @see org.slf4j.Logger#error(java.lang.String)
   */
  public void error(Marker m, ArgN d, Object... args)
  {
    if (logger.isErrorEnabled(m))
    {
      logger.error(m, d.get(args).toString(locale));
    }
  }



  /**
   * Logs a debug message.
   *
   * @param m
   *          The pre-formatted message.
   * @see org.slf4j.Logger#debug(java.lang.String)
   */
  public void debug(LocalizableMessage m)
  {
    if (logger.isDebugEnabled())
    {
      logger.debug(m.toString(locale));
    }
  }



  /**
   * Logs an error message.
   *
   * @param m
   *          The pre-formatted message.
   * @see org.slf4j.Logger#error(java.lang.String)
   */
  public void error(LocalizableMessage m)
  {
    if (logger.isErrorEnabled())
    {
      logger.error(m.toString(locale));
    }
  }



  /**
   * Logs an info message.
   *
   * @param m
   *          The pre-formatted message.
   * @see org.slf4j.Logger#info(java.lang.String)
   */
  public void info(LocalizableMessage m)
  {
    if (logger.isInfoEnabled())
    {
      logger.info(m.toString(locale));
    }
  }



  /**
   * Logs a trace message.
   *
   * @param m
   *          The pre-formatted message.
   * @see org.slf4j.Logger#trace(java.lang.String)
   */
  public void trace(LocalizableMessage m)
  {
    if (logger.isTraceEnabled())
    {
      logger.trace(m.toString(locale));
    }
  }



  /**
   * Logs a warning message.
   *
   * @param m
   *          The pre-formatted message.
   * @see org.slf4j.Logger#warn(java.lang.String)
   */
  public void warn(LocalizableMessage m)
  {
    if (logger.isWarnEnabled())
    {
      logger.warn(m.toString(locale));
    }
  }



  /**
   * Logs a debug message.
   *
   * @param d
   *          The message descriptor.
   * @see org.slf4j.Logger#debug(java.lang.String)
   */
  public void debug(Arg0 d)
  {
    if (logger.isDebugEnabled())
    {
      logger.debug(d.get().toString(locale));
    }
  }



  /**
   * Logs a debug message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @see org.slf4j.Logger#debug(java.lang.String)
   */
  public <T1> void debug(Arg1<T1> d, T1 a1)
  {
    if (logger.isDebugEnabled())
    {
      logger.debug(d.get(a1).toString(locale));
    }
  }



  /**
   * Logs a debug message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @see org.slf4j.Logger#debug(java.lang.String)
   */
  public <T1, T2> void debug(Arg2<T1, T2> d, T1 a1, T2 a2)
  {
    if (logger.isDebugEnabled())
    {
      logger.debug(d.get(a1, a2).toString(locale));
    }
  }



  /**
   * Logs a debug message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @see org.slf4j.Logger#debug(java.lang.String)
   */
  public <T1, T2, T3> void debug(Arg3<T1, T2, T3> d, T1 a1, T2 a2,
      T3 a3)
  {
    if (logger.isDebugEnabled())
    {
      logger.debug(d.get(a1, a2, a3).toString(locale));
    }
  }



  /**
   * Logs a debug message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @see org.slf4j.Logger#debug(java.lang.String)
   */
  public <T1, T2, T3, T4> void debug(Arg4<T1, T2, T3, T4> d, T1 a1,
      T2 a2, T3 a3, T4 a4)
  {
    if (logger.isDebugEnabled())
    {
      logger.debug(d.get(a1, a2, a3, a4).toString(locale));
    }
  }



  /**
   * Logs a debug message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @see org.slf4j.Logger#debug(java.lang.String)
   */
  public <T1, T2, T3, T4, T5> void debug(Arg5<T1, T2, T3, T4, T5> d,
      T1 a1, T2 a2, T3 a3, T4 a4, T5 a5)
  {
    if (logger.isDebugEnabled())
    {
      logger.debug(d.get(a1, a2, a3, a4, a5).toString(locale));
    }
  }



  /**
   * Logs a debug message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @see org.slf4j.Logger#debug(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6> void debug(
      Arg6<T1, T2, T3, T4, T5, T6> d, T1 a1, T2 a2, T3 a3, T4 a4,
      T5 a5, T6 a6)
  {
    if (logger.isDebugEnabled())
    {
      logger.debug(d.get(a1, a2, a3, a4, a5, a6).toString(locale));
    }
  }



  /**
   * Logs a debug message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @see org.slf4j.Logger#debug(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7> void debug(
      Arg7<T1, T2, T3, T4, T5, T6, T7> d, T1 a1, T2 a2, T3 a3, T4 a4,
      T5 a5, T6 a6, T7 a7)
  {
    if (logger.isDebugEnabled())
    {
      logger
          .debug(d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
    }
  }



  /**
   * Logs a debug message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param <T8>
   *          The type of the eighth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @param a8
   *          The eighth message argument.
   * @see org.slf4j.Logger#debug(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7, T8> void debug(
      Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, T1 a1, T2 a2, T3 a3,
      T4 a4, T5 a5, T6 a6, T7 a7, T8 a8)
  {
    if (logger.isDebugEnabled())
    {
      logger.debug(d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(
          locale));
    }
  }



  /**
   * Logs a debug message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param <T8>
   *          The type of the eighth message argument.
   * @param <T9>
   *          The type of the ninth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @param a8
   *          The eighth message argument.
   * @param a9
   *          The ninth message argument.
   * @see org.slf4j.Logger#debug(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void debug(
      Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, T1 a1, T2 a2,
      T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9)
  {
    if (logger.isDebugEnabled())
    {
      logger.debug(d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9)
          .toString(locale));
    }
  }



  /**
   * Logs a debug message.
   *
   * @param d
   *          The message descriptor.
   * @param args
   *          The message arguments.
   * @see org.slf4j.Logger#debug(java.lang.String)
   */
  public void debug(ArgN d, Object... args)
  {
    if (logger.isDebugEnabled())
    {
      logger.debug(d.get(args).toString(locale));
    }
  }



  /**
   * Logs a debug message using the provided {@code Marker}.
   *
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @see org.slf4j.Logger#debug(org.slf4j.Marker, java.lang.String)
   */
  public void debug(Marker m, Arg0 d)
  {
    if (logger.isDebugEnabled(m))
    {
      logger.debug(m, d.get().toString(locale));
    }
  }



  /**
   * Logs a debug message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @see org.slf4j.Logger#debug(java.lang.String)
   */
  public <T1> void debug(Marker m, Arg1<T1> d, T1 a1)
  {
    if (logger.isDebugEnabled(m))
    {
      logger.debug(m, d.get(a1).toString(locale));
    }
  }



  /**
   * Logs a debug message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @see org.slf4j.Logger#debug(java.lang.String)
   */
  public <T1, T2> void debug(Marker m, Arg2<T1, T2> d, T1 a1, T2 a2)
  {
    if (logger.isDebugEnabled(m))
    {
      logger.debug(m, d.get(a1, a2).toString(locale));
    }
  }



  /**
   * Logs a debug message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @see org.slf4j.Logger#debug(java.lang.String)
   */
  public <T1, T2, T3> void debug(Marker m, Arg3<T1, T2, T3> d, T1 a1,
      T2 a2, T3 a3)
  {
    if (logger.isDebugEnabled(m))
    {
      logger.debug(m, d.get(a1, a2, a3).toString(locale));
    }
  }



  /**
   * Logs a debug message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @see org.slf4j.Logger#debug(java.lang.String)
   */
  public <T1, T2, T3, T4> void debug(Marker m,
      Arg4<T1, T2, T3, T4> d, T1 a1, T2 a2, T3 a3, T4 a4)
  {
    if (logger.isDebugEnabled(m))
    {
      logger.debug(m, d.get(a1, a2, a3, a4).toString(locale));
    }
  }



  /**
   * Logs a debug message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @see org.slf4j.Logger#debug(java.lang.String)
   */
  public <T1, T2, T3, T4, T5> void debug(Marker m,
      Arg5<T1, T2, T3, T4, T5> d, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5)
  {
    if (logger.isDebugEnabled(m))
    {
      logger.debug(m, d.get(a1, a2, a3, a4, a5).toString(locale));
    }
  }



  /**
   * Logs a debug message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @see org.slf4j.Logger#debug(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6> void debug(Marker m,
      Arg6<T1, T2, T3, T4, T5, T6> d, T1 a1, T2 a2, T3 a3, T4 a4,
      T5 a5, T6 a6)
  {
    if (logger.isDebugEnabled(m))
    {
      logger.debug(m, d.get(a1, a2, a3, a4, a5, a6).toString(locale));
    }
  }



  /**
   * Logs a debug message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @see org.slf4j.Logger#debug(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7> void debug(Marker m,
      Arg7<T1, T2, T3, T4, T5, T6, T7> d, T1 a1, T2 a2, T3 a3, T4 a4,
      T5 a5, T6 a6, T7 a7)
  {
    if (logger.isDebugEnabled(m))
    {
      logger.debug(m,
          d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
    }
  }



  /**
   * Logs a debug message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param <T8>
   *          The type of the eighth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @param a8
   *          The eighth message argument.
   * @see org.slf4j.Logger#debug(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7, T8> void debug(Marker m,
      Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, T1 a1, T2 a2, T3 a3,
      T4 a4, T5 a5, T6 a6, T7 a7, T8 a8)
  {
    if (logger.isDebugEnabled(m))
    {
      logger.debug(m,
          d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
    }
  }



  /**
   * Logs a debug message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param <T8>
   *          The type of the eighth message argument.
   * @param <T9>
   *          The type of the ninth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @param a8
   *          The eighth message argument.
   * @param a9
   *          The ninth message argument.
   * @see org.slf4j.Logger#debug(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void debug(Marker m,
      Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, T1 a1, T2 a2,
      T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9)
  {
    if (logger.isDebugEnabled(m))
    {
      logger.debug(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9)
          .toString(locale));
    }
  }



  /**
   * Logs a debug message.
   *
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param args
   *          The message arguments.
   * @see org.slf4j.Logger#debug(java.lang.String)
   */
  public void debug(Marker m, ArgN d, Object... args)
  {
    if (logger.isDebugEnabled(m))
    {
      logger.debug(m, d.get(args).toString(locale));
    }
  }



  /**
   * Logs a trace message.
   *
   * @param d
   *          The message descriptor.
   * @see org.slf4j.Logger#trace(java.lang.String)
   */
  public void trace(Arg0 d)
  {
    if (logger.isTraceEnabled())
    {
      logger.trace(d.get().toString(locale));
    }
  }



  /**
   * Logs a trace message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @see org.slf4j.Logger#trace(java.lang.String)
   */
  public <T1> void trace(Arg1<T1> d, T1 a1)
  {
    if (logger.isTraceEnabled())
    {
      logger.trace(d.get(a1).toString(locale));
    }
  }



  /**
   * Logs a trace message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @see org.slf4j.Logger#trace(java.lang.String)
   */
  public <T1, T2> void trace(Arg2<T1, T2> d, T1 a1, T2 a2)
  {
    if (logger.isTraceEnabled())
    {
      logger.trace(d.get(a1, a2).toString(locale));
    }
  }



  /**
   * Logs a trace message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @see org.slf4j.Logger#trace(java.lang.String)
   */
  public <T1, T2, T3> void trace(Arg3<T1, T2, T3> d, T1 a1, T2 a2,
      T3 a3)
  {
    if (logger.isTraceEnabled())
    {
      logger.trace(d.get(a1, a2, a3).toString(locale));
    }
  }



  /**
   * Logs a trace message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @see org.slf4j.Logger#trace(java.lang.String)
   */
  public <T1, T2, T3, T4> void trace(Arg4<T1, T2, T3, T4> d, T1 a1,
      T2 a2, T3 a3, T4 a4)
  {
    if (logger.isTraceEnabled())
    {
      logger.trace(d.get(a1, a2, a3, a4).toString(locale));
    }
  }



  /**
   * Logs a trace message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @see org.slf4j.Logger#trace(java.lang.String)
   */
  public <T1, T2, T3, T4, T5> void trace(Arg5<T1, T2, T3, T4, T5> d,
      T1 a1, T2 a2, T3 a3, T4 a4, T5 a5)
  {
    if (logger.isTraceEnabled())
    {
      logger.trace(d.get(a1, a2, a3, a4, a5).toString(locale));
    }
  }



  /**
   * Logs a trace message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @see org.slf4j.Logger#trace(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6> void trace(
      Arg6<T1, T2, T3, T4, T5, T6> d, T1 a1, T2 a2, T3 a3, T4 a4,
      T5 a5, T6 a6)
  {
    if (logger.isTraceEnabled())
    {
      logger.trace(d.get(a1, a2, a3, a4, a5, a6).toString(locale));
    }
  }



  /**
   * Logs a trace message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @see org.slf4j.Logger#trace(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7> void trace(
      Arg7<T1, T2, T3, T4, T5, T6, T7> d, T1 a1, T2 a2, T3 a3, T4 a4,
      T5 a5, T6 a6, T7 a7)
  {
    if (logger.isTraceEnabled())
    {
      logger
          .trace(d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
    }
  }



  /**
   * Logs a trace message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param <T8>
   *          The type of the eighth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @param a8
   *          The eighth message argument.
   * @see org.slf4j.Logger#trace(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7, T8> void trace(
      Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, T1 a1, T2 a2, T3 a3,
      T4 a4, T5 a5, T6 a6, T7 a7, T8 a8)
  {
    if (logger.isTraceEnabled())
    {
      logger.trace(d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(
          locale));
    }
  }



  /**
   * Logs a trace message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param <T8>
   *          The type of the eighth message argument.
   * @param <T9>
   *          The type of the ninth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @param a8
   *          The eighth message argument.
   * @param a9
   *          The ninth message argument.
   * @see org.slf4j.Logger#trace(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void trace(
      Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, T1 a1, T2 a2,
      T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9)
  {
    if (logger.isTraceEnabled())
    {
      logger.trace(d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9)
          .toString(locale));
    }
  }



  /**
   * Logs a trace message.
   *
   * @param d
   *          The message descriptor.
   * @param args
   *          The message arguments.
   * @see org.slf4j.Logger#trace(java.lang.String)
   */
  public void trace(ArgN d, Object... args)
  {
    if (logger.isTraceEnabled())
    {
      logger.trace(d.get(args).toString(locale));
    }
  }



  /**
   * Logs a trace message using the provided {@code Marker}.
   *
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @see org.slf4j.Logger#trace(org.slf4j.Marker, java.lang.String)
   */
  public void trace(Marker m, Arg0 d)
  {
    if (logger.isTraceEnabled(m))
    {
      logger.trace(m, d.get().toString(locale));
    }
  }



  /**
   * Logs a trace message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @see org.slf4j.Logger#trace(java.lang.String)
   */
  public <T1> void trace(Marker m, Arg1<T1> d, T1 a1)
  {
    if (logger.isTraceEnabled(m))
    {
      logger.trace(m, d.get(a1).toString(locale));
    }
  }



  /**
   * Logs a trace message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @see org.slf4j.Logger#trace(java.lang.String)
   */
  public <T1, T2> void trace(Marker m, Arg2<T1, T2> d, T1 a1, T2 a2)
  {
    if (logger.isTraceEnabled(m))
    {
      logger.trace(m, d.get(a1, a2).toString(locale));
    }
  }



  /**
   * Logs a trace message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @see org.slf4j.Logger#trace(java.lang.String)
   */
  public <T1, T2, T3> void trace(Marker m, Arg3<T1, T2, T3> d, T1 a1,
      T2 a2, T3 a3)
  {
    if (logger.isTraceEnabled(m))
    {
      logger.trace(m, d.get(a1, a2, a3).toString(locale));
    }
  }



  /**
   * Logs a trace message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @see org.slf4j.Logger#trace(java.lang.String)
   */
  public <T1, T2, T3, T4> void trace(Marker m,
      Arg4<T1, T2, T3, T4> d, T1 a1, T2 a2, T3 a3, T4 a4)
  {
    if (logger.isTraceEnabled(m))
    {
      logger.trace(m, d.get(a1, a2, a3, a4).toString(locale));
    }
  }



  /**
   * Logs a trace message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @see org.slf4j.Logger#trace(java.lang.String)
   */
  public <T1, T2, T3, T4, T5> void trace(Marker m,
      Arg5<T1, T2, T3, T4, T5> d, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5)
  {
    if (logger.isTraceEnabled(m))
    {
      logger.trace(m, d.get(a1, a2, a3, a4, a5).toString(locale));
    }
  }



  /**
   * Logs a trace message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @see org.slf4j.Logger#trace(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6> void trace(Marker m,
      Arg6<T1, T2, T3, T4, T5, T6> d, T1 a1, T2 a2, T3 a3, T4 a4,
      T5 a5, T6 a6)
  {
    if (logger.isTraceEnabled(m))
    {
      logger.trace(m, d.get(a1, a2, a3, a4, a5, a6).toString(locale));
    }
  }



  /**
   * Logs a trace message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @see org.slf4j.Logger#trace(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7> void trace(Marker m,
      Arg7<T1, T2, T3, T4, T5, T6, T7> d, T1 a1, T2 a2, T3 a3, T4 a4,
      T5 a5, T6 a6, T7 a7)
  {
    if (logger.isTraceEnabled(m))
    {
      logger.trace(m,
          d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
    }
  }



  /**
   * Logs a trace message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param <T8>
   *          The type of the eighth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @param a8
   *          The eighth message argument.
   * @see org.slf4j.Logger#trace(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7, T8> void trace(Marker m,
      Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, T1 a1, T2 a2, T3 a3,
      T4 a4, T5 a5, T6 a6, T7 a7, T8 a8)
  {
    if (logger.isTraceEnabled(m))
    {
      logger.trace(m,
          d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
    }
  }



  /**
   * Logs a trace message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param <T8>
   *          The type of the eighth message argument.
   * @param <T9>
   *          The type of the ninth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @param a8
   *          The eighth message argument.
   * @param a9
   *          The ninth message argument.
   * @see org.slf4j.Logger#trace(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void trace(Marker m,
      Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, T1 a1, T2 a2,
      T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9)
  {
    if (logger.isTraceEnabled(m))
    {
      logger.trace(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9)
          .toString(locale));
    }
  }



  /**
   * Logs a trace message.
   *
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param args
   *          The message arguments.
   * @see org.slf4j.Logger#trace(java.lang.String)
   */
  public void trace(Marker m, ArgN d, Object... args)
  {
    if (logger.isTraceEnabled(m))
    {
      logger.trace(m, d.get(args).toString(locale));
    }
  }



  /**
   * Logs a warning message.
   *
   * @param d
   *          The message descriptor.
   * @see org.slf4j.Logger#warn(java.lang.String)
   */
  public void warn(Arg0 d)
  {
    if (logger.isWarnEnabled())
    {
      logger.warn(d.get().toString(locale));
    }
  }



  /**
   * Logs a warning message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @see org.slf4j.Logger#warn(java.lang.String)
   */
  public <T1> void warn(Arg1<T1> d, T1 a1)
  {
    if (logger.isWarnEnabled())
    {
      logger.warn(d.get(a1).toString(locale));
    }
  }



  /**
   * Logs a warning message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @see org.slf4j.Logger#warn(java.lang.String)
   */
  public <T1, T2> void warn(Arg2<T1, T2> d, T1 a1, T2 a2)
  {
    if (logger.isWarnEnabled())
    {
      logger.warn(d.get(a1, a2).toString(locale));
    }
  }



  /**
   * Logs a warning message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @see org.slf4j.Logger#warn(java.lang.String)
   */
  public <T1, T2, T3> void warn(Arg3<T1, T2, T3> d, T1 a1, T2 a2,
      T3 a3)
  {
    if (logger.isWarnEnabled())
    {
      logger.warn(d.get(a1, a2, a3).toString(locale));
    }
  }



  /**
   * Logs a warning message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @see org.slf4j.Logger#warn(java.lang.String)
   */
  public <T1, T2, T3, T4> void warn(Arg4<T1, T2, T3, T4> d, T1 a1,
      T2 a2, T3 a3, T4 a4)
  {
    if (logger.isWarnEnabled())
    {
      logger.warn(d.get(a1, a2, a3, a4).toString(locale));
    }
  }



  /**
   * Logs a warning message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @see org.slf4j.Logger#warn(java.lang.String)
   */
  public <T1, T2, T3, T4, T5> void warn(Arg5<T1, T2, T3, T4, T5> d,
      T1 a1, T2 a2, T3 a3, T4 a4, T5 a5)
  {
    if (logger.isWarnEnabled())
    {
      logger.warn(d.get(a1, a2, a3, a4, a5).toString(locale));
    }
  }



  /**
   * Logs a warning message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @see org.slf4j.Logger#warn(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6> void warn(
      Arg6<T1, T2, T3, T4, T5, T6> d, T1 a1, T2 a2, T3 a3, T4 a4,
      T5 a5, T6 a6)
  {
    if (logger.isWarnEnabled())
    {
      logger.warn(d.get(a1, a2, a3, a4, a5, a6).toString(locale));
    }
  }



  /**
   * Logs a warning message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @see org.slf4j.Logger#warn(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7> void warn(
      Arg7<T1, T2, T3, T4, T5, T6, T7> d, T1 a1, T2 a2, T3 a3, T4 a4,
      T5 a5, T6 a6, T7 a7)
  {
    if (logger.isWarnEnabled())
    {
      logger.warn(d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
    }
  }



  /**
   * Logs a warning message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param <T8>
   *          The type of the eighth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @param a8
   *          The eighth message argument.
   * @see org.slf4j.Logger#warn(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7, T8> void warn(
      Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, T1 a1, T2 a2, T3 a3,
      T4 a4, T5 a5, T6 a6, T7 a7, T8 a8)
  {
    if (logger.isWarnEnabled())
    {
      logger.warn(d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(
          locale));
    }
  }



  /**
   * Logs a warning message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param <T8>
   *          The type of the eighth message argument.
   * @param <T9>
   *          The type of the ninth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @param a8
   *          The eighth message argument.
   * @param a9
   *          The ninth message argument.
   * @see org.slf4j.Logger#warn(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void warn(
      Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, T1 a1, T2 a2,
      T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9)
  {
    if (logger.isWarnEnabled())
    {
      logger.warn(d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(
          locale));
    }
  }



  /**
   * Logs a warning message.
   *
   * @param d
   *          The message descriptor.
   * @param args
   *          The message arguments.
   * @see org.slf4j.Logger#warn(java.lang.String)
   */
  public void warn(ArgN d, Object... args)
  {
    if (logger.isWarnEnabled())
    {
      logger.warn(d.get(args).toString(locale));
    }
  }



  /**
   * Logs a warning message using the provided {@code Marker}.
   *
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @see org.slf4j.Logger#warn(org.slf4j.Marker, java.lang.String)
   */
  public void warn(Marker m, Arg0 d)
  {
    if (logger.isWarnEnabled(m))
    {
      logger.warn(m, d.get().toString(locale));
    }
  }



  /**
   * Logs a warning message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @see org.slf4j.Logger#warn(java.lang.String)
   */
  public <T1> void warn(Marker m, Arg1<T1> d, T1 a1)
  {
    if (logger.isWarnEnabled(m))
    {
      logger.warn(m, d.get(a1).toString(locale));
    }
  }



  /**
   * Logs a warning message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @see org.slf4j.Logger#warn(java.lang.String)
   */
  public <T1, T2> void warn(Marker m, Arg2<T1, T2> d, T1 a1, T2 a2)
  {
    if (logger.isWarnEnabled(m))
    {
      logger.warn(m, d.get(a1, a2).toString(locale));
    }
  }



  /**
   * Logs a warning message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @see org.slf4j.Logger#warn(java.lang.String)
   */
  public <T1, T2, T3> void warn(Marker m, Arg3<T1, T2, T3> d, T1 a1,
      T2 a2, T3 a3)
  {
    if (logger.isWarnEnabled(m))
    {
      logger.warn(m, d.get(a1, a2, a3).toString(locale));
    }
  }



  /**
   * Logs a warning message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @see org.slf4j.Logger#warn(java.lang.String)
   */
  public <T1, T2, T3, T4> void warn(Marker m, Arg4<T1, T2, T3, T4> d,
      T1 a1, T2 a2, T3 a3, T4 a4)
  {
    if (logger.isWarnEnabled(m))
    {
      logger.warn(m, d.get(a1, a2, a3, a4).toString(locale));
    }
  }



  /**
   * Logs a warning message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @see org.slf4j.Logger#warn(java.lang.String)
   */
  public <T1, T2, T3, T4, T5> void warn(Marker m,
      Arg5<T1, T2, T3, T4, T5> d, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5)
  {
    if (logger.isWarnEnabled(m))
    {
      logger.warn(m, d.get(a1, a2, a3, a4, a5).toString(locale));
    }
  }



  /**
   * Logs a warning message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @see org.slf4j.Logger#warn(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6> void warn(Marker m,
      Arg6<T1, T2, T3, T4, T5, T6> d, T1 a1, T2 a2, T3 a3, T4 a4,
      T5 a5, T6 a6)
  {
    if (logger.isWarnEnabled(m))
    {
      logger.warn(m, d.get(a1, a2, a3, a4, a5, a6).toString(locale));
    }
  }



  /**
   * Logs a warning message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @see org.slf4j.Logger#warn(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7> void warn(Marker m,
      Arg7<T1, T2, T3, T4, T5, T6, T7> d, T1 a1, T2 a2, T3 a3, T4 a4,
      T5 a5, T6 a6, T7 a7)
  {
    if (logger.isWarnEnabled(m))
    {
      logger.warn(m,
          d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
    }
  }



  /**
   * Logs a warning message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param <T8>
   *          The type of the eighth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @param a8
   *          The eighth message argument.
   * @see org.slf4j.Logger#warn(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7, T8> void warn(Marker m,
      Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, T1 a1, T2 a2, T3 a3,
      T4 a4, T5 a5, T6 a6, T7 a7, T8 a8)
  {
    if (logger.isWarnEnabled(m))
    {
      logger.warn(m,
          d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
    }
  }



  /**
   * Logs a warning message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param <T8>
   *          The type of the eighth message argument.
   * @param <T9>
   *          The type of the ninth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @param a8
   *          The eighth message argument.
   * @param a9
   *          The ninth message argument.
   * @see org.slf4j.Logger#warn(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void warn(Marker m,
      Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, T1 a1, T2 a2,
      T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9)
  {
    if (logger.isWarnEnabled(m))
    {
      logger.warn(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9)
          .toString(locale));
    }
  }



  /**
   * Logs a warning message.
   *
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param args
   *          The message arguments.
   * @see org.slf4j.Logger#warn(java.lang.String)
   */
  public void warn(Marker m, ArgN d, Object... args)
  {
    if (logger.isWarnEnabled(m))
    {
      logger.warn(m, d.get(args).toString(locale));
    }
  }



  /**
   * Logs an info message.
   *
   * @param d
   *          The message descriptor.
   * @see org.slf4j.Logger#info(java.lang.String)
   */
  public void info(Arg0 d)
  {
    if (logger.isInfoEnabled())
    {
      logger.info(d.get().toString(locale));
    }
  }



  /**
   * Logs an info message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @see org.slf4j.Logger#info(java.lang.String)
   */
  public <T1> void info(Arg1<T1> d, T1 a1)
  {
    if (logger.isInfoEnabled())
    {
      logger.info(d.get(a1).toString(locale));
    }
  }



  /**
   * Logs an info message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @see org.slf4j.Logger#info(java.lang.String)
   */
  public <T1, T2> void info(Arg2<T1, T2> d, T1 a1, T2 a2)
  {
    if (logger.isInfoEnabled())
    {
      logger.info(d.get(a1, a2).toString(locale));
    }
  }



  /**
   * Logs an info message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @see org.slf4j.Logger#info(java.lang.String)
   */
  public <T1, T2, T3> void info(Arg3<T1, T2, T3> d, T1 a1, T2 a2,
      T3 a3)
  {
    if (logger.isInfoEnabled())
    {
      logger.info(d.get(a1, a2, a3).toString(locale));
    }
  }



  /**
   * Logs an info message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @see org.slf4j.Logger#info(java.lang.String)
   */
  public <T1, T2, T3, T4> void info(Arg4<T1, T2, T3, T4> d, T1 a1,
      T2 a2, T3 a3, T4 a4)
  {
    if (logger.isInfoEnabled())
    {
      logger.info(d.get(a1, a2, a3, a4).toString(locale));
    }
  }



  /**
   * Logs an info message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @see org.slf4j.Logger#info(java.lang.String)
   */
  public <T1, T2, T3, T4, T5> void info(Arg5<T1, T2, T3, T4, T5> d,
      T1 a1, T2 a2, T3 a3, T4 a4, T5 a5)
  {
    if (logger.isInfoEnabled())
    {
      logger.info(d.get(a1, a2, a3, a4, a5).toString(locale));
    }
  }



  /**
   * Logs an info message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @see org.slf4j.Logger#info(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6> void info(
      Arg6<T1, T2, T3, T4, T5, T6> d, T1 a1, T2 a2, T3 a3, T4 a4,
      T5 a5, T6 a6)
  {
    if (logger.isInfoEnabled())
    {
      logger.info(d.get(a1, a2, a3, a4, a5, a6).toString(locale));
    }
  }



  /**
   * Logs an info message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @see org.slf4j.Logger#info(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7> void info(
      Arg7<T1, T2, T3, T4, T5, T6, T7> d, T1 a1, T2 a2, T3 a3, T4 a4,
      T5 a5, T6 a6, T7 a7)
  {
    if (logger.isInfoEnabled())
    {
      logger.info(d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
    }
  }



  /**
   * Logs an info message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param <T8>
   *          The type of the eighth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @param a8
   *          The eighth message argument.
   * @see org.slf4j.Logger#info(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7, T8> void info(
      Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, T1 a1, T2 a2, T3 a3,
      T4 a4, T5 a5, T6 a6, T7 a7, T8 a8)
  {
    if (logger.isInfoEnabled())
    {
      logger.info(d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(
          locale));
    }
  }



  /**
   * Logs an info message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param <T8>
   *          The type of the eighth message argument.
   * @param <T9>
   *          The type of the ninth message argument.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @param a8
   *          The eighth message argument.
   * @param a9
   *          The ninth message argument.
   * @see org.slf4j.Logger#info(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void info(
      Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, T1 a1, T2 a2,
      T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9)
  {
    if (logger.isInfoEnabled())
    {
      logger.info(d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9).toString(
          locale));
    }
  }



  /**
   * Logs an info message.
   *
   * @param d
   *          The message descriptor.
   * @param args
   *          The message arguments.
   * @see org.slf4j.Logger#info(java.lang.String)
   */
  public void info(ArgN d, Object... args)
  {
    if (logger.isInfoEnabled())
    {
      logger.info(d.get(args).toString(locale));
    }
  }



  /**
   * Logs an info message using the provided {@code Marker}.
   *
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @see org.slf4j.Logger#info(org.slf4j.Marker, java.lang.String)
   */
  public void info(Marker m, Arg0 d)
  {
    if (logger.isInfoEnabled(m))
    {
      logger.info(m, d.get().toString(locale));
    }
  }



  /**
   * Logs an info message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @see org.slf4j.Logger#info(java.lang.String)
   */
  public <T1> void info(Marker m, Arg1<T1> d, T1 a1)
  {
    if (logger.isInfoEnabled(m))
    {
      logger.info(m, d.get(a1).toString(locale));
    }
  }



  /**
   * Logs an info message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @see org.slf4j.Logger#info(java.lang.String)
   */
  public <T1, T2> void info(Marker m, Arg2<T1, T2> d, T1 a1, T2 a2)
  {
    if (logger.isInfoEnabled(m))
    {
      logger.info(m, d.get(a1, a2).toString(locale));
    }
  }



  /**
   * Logs an info message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @see org.slf4j.Logger#info(java.lang.String)
   */
  public <T1, T2, T3> void info(Marker m, Arg3<T1, T2, T3> d, T1 a1,
      T2 a2, T3 a3)
  {
    if (logger.isInfoEnabled(m))
    {
      logger.info(m, d.get(a1, a2, a3).toString(locale));
    }
  }



  /**
   * Logs an info message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @see org.slf4j.Logger#info(java.lang.String)
   */
  public <T1, T2, T3, T4> void info(Marker m, Arg4<T1, T2, T3, T4> d,
      T1 a1, T2 a2, T3 a3, T4 a4)
  {
    if (logger.isInfoEnabled(m))
    {
      logger.info(m, d.get(a1, a2, a3, a4).toString(locale));
    }
  }



  /**
   * Logs an info message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @see org.slf4j.Logger#info(java.lang.String)
   */
  public <T1, T2, T3, T4, T5> void info(Marker m,
      Arg5<T1, T2, T3, T4, T5> d, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5)
  {
    if (logger.isInfoEnabled(m))
    {
      logger.info(m, d.get(a1, a2, a3, a4, a5).toString(locale));
    }
  }



  /**
   * Logs an info message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @see org.slf4j.Logger#info(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6> void info(Marker m,
      Arg6<T1, T2, T3, T4, T5, T6> d, T1 a1, T2 a2, T3 a3, T4 a4,
      T5 a5, T6 a6)
  {
    if (logger.isInfoEnabled(m))
    {
      logger.info(m, d.get(a1, a2, a3, a4, a5, a6).toString(locale));
    }
  }



  /**
   * Logs an info message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @see org.slf4j.Logger#info(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7> void info(Marker m,
      Arg7<T1, T2, T3, T4, T5, T6, T7> d, T1 a1, T2 a2, T3 a3, T4 a4,
      T5 a5, T6 a6, T7 a7)
  {
    if (logger.isInfoEnabled(m))
    {
      logger.info(m,
          d.get(a1, a2, a3, a4, a5, a6, a7).toString(locale));
    }
  }



  /**
   * Logs an info message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param <T8>
   *          The type of the eighth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @param a8
   *          The eighth message argument.
   * @see org.slf4j.Logger#info(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7, T8> void info(Marker m,
      Arg8<T1, T2, T3, T4, T5, T6, T7, T8> d, T1 a1, T2 a2, T3 a3,
      T4 a4, T5 a5, T6 a6, T7 a7, T8 a8)
  {
    if (logger.isInfoEnabled(m))
    {
      logger.info(m,
          d.get(a1, a2, a3, a4, a5, a6, a7, a8).toString(locale));
    }
  }



  /**
   * Logs an info message.
   *
   * @param <T1>
   *          The type of the first message argument.
   * @param <T2>
   *          The type of the second message argument.
   * @param <T3>
   *          The type of the third message argument.
   * @param <T4>
   *          The type of the fourth message argument.
   * @param <T5>
   *          The type of the fifth message argument.
   * @param <T6>
   *          The type of the sixth message argument.
   * @param <T7>
   *          The type of the seventh message argument.
   * @param <T8>
   *          The type of the eighth message argument.
   * @param <T9>
   *          The type of the ninth message argument.
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param a1
   *          The first message argument.
   * @param a2
   *          The second message argument.
   * @param a3
   *          The third message argument.
   * @param a4
   *          The fourth message argument.
   * @param a5
   *          The fifth message argument.
   * @param a6
   *          The sixth message argument.
   * @param a7
   *          The seventh message argument.
   * @param a8
   *          The eighth message argument.
   * @param a9
   *          The ninth message argument.
   * @see org.slf4j.Logger#info(java.lang.String)
   */
  public <T1, T2, T3, T4, T5, T6, T7, T8, T9> void info(Marker m,
      Arg9<T1, T2, T3, T4, T5, T6, T7, T8, T9> d, T1 a1, T2 a2,
      T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9)
  {
    if (logger.isInfoEnabled(m))
    {
      logger.info(m, d.get(a1, a2, a3, a4, a5, a6, a7, a8, a9)
          .toString(locale));
    }
  }



  /**
   * Logs an info message.
   *
   * @param m
   *          The marker information associated with this log message.
   * @param d
   *          The message descriptor.
   * @param args
   *          The message arguments.
   * @see org.slf4j.Logger#info(java.lang.String)
   */
  public void info(Marker m, ArgN d, Object... args)
  {
    if (logger.isInfoEnabled(m))
    {
      logger.info(m, d.get(args).toString(locale));
    }
  }



  /**
   * Returns the locale to which this logger will localize all log messages.
   *
   * @return The locale to which this logger will localize all log messages.
   */
  public Locale getLocale()
  {
    return locale;
  }



  /**
   * Returns the underlying SLF4J {@code Logger} wrapped by this logger.
   *
   * @return The underlying SLF4J {@code Logger} wrapped by this logger.
   */
  public Logger getLogger()
  {
    return logger;
  }



  /**
   * Returns the name of this logger.
   *
   * @return The name of this logger.
   * @see org.slf4j.Logger#getName()
   */
  public String getName()
  {
    return logger.getName();
  }



  /**
   * Returns {@code true} if this logger will log debug messages.
   *
   * @return {@code true} if this logger will log debug messages, otherwise
   *         {@code false}.
   * @see org.slf4j.Logger#isDebugEnabled()
   */
  public boolean isDebugEnabled()
  {
    return logger.isDebugEnabled();
  }



  /**
   * Returns {@code true} if this logger will log debug messages associated with
   * the provided marker.
   *
   * @param m
   *          The marker information.
   * @return {@code true} if this logger will log debug messages, otherwise
   *         {@code false}.
   * @see org.slf4j.Logger#isDebugEnabled(org.slf4j.Marker)
   */
  public boolean isDebugEnabled(Marker m)
  {
    return logger.isDebugEnabled(m);
  }



  /**
   * Returns {@code true} if this logger will log error messages.
   *
   * @return {@code true} if this logger will log error messages, otherwise
   *         {@code false}.
   * @see org.slf4j.Logger#isErrorEnabled()
   */
  public boolean isErrorEnabled()
  {
    return logger.isErrorEnabled();
  }



  /**
   * Returns {@code true} if this logger will log error messages associated with
   * the provided marker.
   *
   * @param m
   *          The marker information.
   * @return {@code true} if this logger will log error messages, otherwise
   *         {@code false}.
   * @see org.slf4j.Logger#isErrorEnabled(org.slf4j.Marker)
   */
  public boolean isErrorEnabled(Marker m)
  {
    return logger.isErrorEnabled(m);
  }



  /**
   * Returns {@code true} if this logger will log info messages.
   *
   * @return {@code true} if this logger will log info messages, otherwise
   *         {@code false}.
   * @see org.slf4j.Logger#isInfoEnabled()
   */
  public boolean isInfoEnabled()
  {
    return logger.isInfoEnabled();
  }



  /**
   * Returns {@code true} if this logger will log info messages associated with
   * the provided marker.
   *
   * @param m
   *          The marker information.
   * @return {@code true} if this logger will log info messages, otherwise
   *         {@code false}.
   * @see org.slf4j.Logger#isInfoEnabled(org.slf4j.Marker)
   */
  public boolean isInfoEnabled(Marker m)
  {
    return logger.isInfoEnabled(m);
  }



  /**
   * Returns {@code true} if this logger will log trace messages.
   *
   * @return {@code true} if this logger will log trace messages, otherwise
   *         {@code false}.
   * @see org.slf4j.Logger#isTraceEnabled()
   */
  public boolean isTraceEnabled()
  {
    return logger.isTraceEnabled();
  }



  /**
   * Returns {@code true} if this logger will log trace messages associated with
   * the provided marker.
   *
   * @param m
   *          The marker information.
   * @return {@code true} if this logger will log trace messages, otherwise
   *         {@code false}.
   * @see org.slf4j.Logger#isTraceEnabled(org.slf4j.Marker)
   */
  public boolean isTraceEnabled(Marker m)
  {
    return logger.isTraceEnabled(m);
  }



  /**
   * Returns {@code true} if this logger will log warning messages.
   *
   * @return {@code true} if this logger will log warning messages, otherwise
   *         {@code false}.
   * @see org.slf4j.Logger#isWarnEnabled()
   */
  public boolean isWarnEnabled()
  {
    return logger.isWarnEnabled();
  }



  /**
   * Returns {@code true} if this logger will log warning messages associated
   * with the provided marker.
   *
   * @param m
   *          The marker information.
   * @return {@code true} if this logger will log warning messages, otherwise
   *         {@code false}.
   * @see org.slf4j.Logger#isWarnEnabled(org.slf4j.Marker)
   */
  public boolean isWarnEnabled(Marker m)
  {
    return logger.isWarnEnabled(m);
  }

}
