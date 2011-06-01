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

package org.forgerock.i18n;



import static org.fest.assertions.Assertions.assertThat;

import java.util.Locale;

import org.testng.annotations.Test;



/**
 * Tests the {@code LocalizableMessage} class.
 */
@Test
public final class LocalizableMessageTest
{

  private static final String RESOURCE_NAME = "org.forgerock.i18n.my_test";



  /**
   * Tests retrieval of a message having an ordinal but no arguments.
   */
  @Test
  public void testFirstMessageWithOrdinal()
  {
    final LocalizableMessage message = MyTestMessages.FIRST_MESSAGE_WITH_ORDINAL
        .get();

    assertThat(message.toString())
        .isEqualTo("Message with ordinal 1");
    assertThat(message.toString(Locale.FRANCE)).isEqualTo(
        "French message with ordinal 1");
    assertThat(message.ordinal()).isEqualTo(1);
    assertThat(message.resourceName()).isEqualTo(RESOURCE_NAME);
  }



  /**
   * Tests retrieval of a message having no ordinal or arguments.
   */
  @Test
  public void testMessageWithNoOrdinal()
  {
    final LocalizableMessage message = MyTestMessages.MESSAGE_WITH_NO_ORDINAL
        .get();

    assertThat(message.toString()).isEqualTo(
        "Message with no ordinal");
    assertThat(message.toString(Locale.FRANCE)).isEqualTo(
        "French message with no ordinal");
    assertThat(message.ordinal()).isEqualTo(-1);
    assertThat(message.resourceName()).isEqualTo(RESOURCE_NAME);
  }



  /**
   * Tests retrieval of a message having no ordinal but a single String
   * argument.
   */
  @Test
  public void testMessageWithString()
  {
    final LocalizableMessage message = MyTestMessages.MESSAGE_WITH_STRING
        .get("test");

    assertThat(message.toString()).isEqualTo("Arg1=test");
    assertThat(message.toString(Locale.FRANCE)).isEqualTo(
        "French Arg1=test");
    assertThat(message.ordinal()).isEqualTo(-1);
    assertThat(message.resourceName()).isEqualTo(RESOURCE_NAME);
  }



  /**
   * Tests retrieval of a message having no ordinal but a String and Number
   * argument.
   */
  @Test
  public void testMessageWithStringAndNumber()
  {
    final LocalizableMessage message = MyTestMessages.MESSAGE_WITH_STRING_AND_NUMBER
        .get("test", 123);

    assertThat(message.toString()).isEqualTo("Arg1=test Arg2=123");
    assertThat(message.toString(Locale.FRANCE)).isEqualTo(
        "French Arg1=test Arg2=123");
    assertThat(message.ordinal()).isEqualTo(-1);
    assertThat(message.resourceName()).isEqualTo(RESOURCE_NAME);
  }



  /**
   * Tests retrieval of a message having an ordinal but no arguments.
   */
  @Test
  public void testSecondMessageWithOrdinal()
  {
    final LocalizableMessage message = MyTestMessages.SECOND_MESSAGE_WITH_ORDINAL
        .get();

    assertThat(message.toString())
        .isEqualTo("Message with ordinal 2");
    assertThat(message.toString(Locale.FRANCE)).isEqualTo(
        "French message with ordinal 2");
    assertThat(message.ordinal()).isEqualTo(2);
    assertThat(message.resourceName()).isEqualTo(RESOURCE_NAME);
  }



  /**
   * Tests raw message with no arguments.
   */
  @Test
  public void testRawMessageNoArgs()
  {
    final LocalizableMessage message = LocalizableMessage.raw("hello world");
    assertThat(message.toString()).isEqualTo("hello world");
  }



  /**
   * Tests raw message with no arguments is not formatted.
   */
  @Test
  public void testRawMessageNoArgsNoFormatting()
  {
    final LocalizableMessage message = LocalizableMessage.raw("hello %%");
    assertThat(message.toString()).isEqualTo("hello %%");
  }



  /**
   * Tests raw message with one argument is formatted.
   */
  @Test
  public void testRawMessageOneArg()
  {
    final LocalizableMessage message = LocalizableMessage.raw(
        "hello %s", "world");
    assertThat(message.toString()).isEqualTo("hello world");
  }



  /**
   * Tests raw message with two arguments is formatted.
   */
  @Test
  public void testRawMessageTwoArgs()
  {
    final LocalizableMessage message = LocalizableMessage.raw(
        "%s %s", "hello", "world");
    assertThat(message.toString()).isEqualTo("hello world");
  }



  /**
   * Tests valueOf.
   */
  @Test
  public void testValueOfNoArgs()
  {
    final LocalizableMessage message = LocalizableMessage.valueOf("hello world");
    assertThat(message.toString()).isEqualTo("hello world");
  }



  /**
   * Tests valueOf is not formatted.
   */
  @Test
  public void testValueOfNoFormatting()
  {
    final LocalizableMessage message = LocalizableMessage.valueOf("hello %%");
    assertThat(message.toString()).isEqualTo("hello %%");
  }

}
