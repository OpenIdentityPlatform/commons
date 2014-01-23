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

package org.forgerock.i18n;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Locale;

import org.testng.annotations.Test;

/**
 * Tests the {@code LocalizableMessage} class.
 */
@Test
public final class LocalizableMessageTest {

    private static final String RESOURCE_NAME = "org.forgerock.i18n.my_test";

    /**
     * Tests that the message file's static resource name can be obtained.
     */
    @Test
    public void testStaticResourceName() {
        assertThat(MyTestMessages.resourceName()).isEqualTo(RESOURCE_NAME);
    }

    /**
     * Tests retrieval of a message having an ordinal but no arguments.
     */
    @Test
    public void testFirstMessageWithOrdinal() {
        final LocalizableMessage message = MyTestMessages.FIRST_MESSAGE_WITH_ORDINAL
                .get();

        assertThat(message.toString()).isEqualTo("Message with ordinal 1");
        assertThat(message.toString(Locale.FRANCE)).isEqualTo(
                "French message with ordinal 1");
        assertThat(message.ordinal()).isEqualTo(1);
        assertThat(message.resourceName()).isEqualTo(RESOURCE_NAME);
    }

    /**
     * Tests retrieval of a message having no ordinal or arguments.
     */
    @Test
    public void testMessageWithNoOrdinal() {
        final LocalizableMessage message = MyTestMessages.MESSAGE_WITH_NO_ORDINAL
                .get();

        assertThat(message.toString()).isEqualTo("Message with no ordinal");
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
    public void testMessageWithString() {
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
    public void testMessageWithStringAndNumber() {
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
    public void testSecondMessageWithOrdinal() {
        final LocalizableMessage message = MyTestMessages.SECOND_MESSAGE_WITH_ORDINAL
                .get();

        assertThat(message.toString()).isEqualTo("Message with ordinal 2");
        assertThat(message.toString(Locale.FRANCE)).isEqualTo(
                "French message with ordinal 2");
        assertThat(message.ordinal()).isEqualTo(2);
        assertThat(message.resourceName()).isEqualTo(RESOURCE_NAME);
    }

    /**
     * Tests retrieval of a message containing a quoted percent character.
     */
    @Test
    public void testMessageWithQuotedPercent() {
        final LocalizableMessage message = MyTestMessages.MESSAGE_WITH_QUOTED_PERCENT
                .get();

        assertThat(message.toString()).isEqualTo("Message with\nquoted %");
        assertThat(message.toString(Locale.FRANCE)).isEqualTo(
                "French message with\nquoted %");
        assertThat(message.ordinal()).isEqualTo(-1);
        assertThat(message.resourceName()).isEqualTo(RESOURCE_NAME);
    }

    /**
     * Tests raw message with no arguments.
     */
    @Test
    public void testRawMessageNoArgs() {
        final LocalizableMessage message = LocalizableMessage
                .raw("hello world");
        assertThat(message.toString()).isEqualTo("hello world");
    }

    /**
     * Tests raw message with no arguments is not formatted.
     */
    @Test
    public void testRawMessageNoArgsNoFormatting() {
        final LocalizableMessage message = LocalizableMessage.raw("hello %%");
        assertThat(message.toString()).isEqualTo("hello %%");
    }

    /**
     * Tests raw message with one argument is formatted.
     */
    @Test
    public void testRawMessageOneArg() {
        final LocalizableMessage message = LocalizableMessage.raw("hello %s",
                "world");
        assertThat(message.toString()).isEqualTo("hello world");
    }

    /**
     * Tests raw message with two arguments is formatted.
     */
    @Test
    public void testRawMessageTwoArgs() {
        final LocalizableMessage message = LocalizableMessage.raw("%s %s",
                "hello", "world");
        assertThat(message.toString()).isEqualTo("hello world");
    }

    /**
     * Tests valueOf.
     */
    @Test
    public void testValueOfNoArgs() {
        final LocalizableMessage message = LocalizableMessage
                .valueOf("hello world");
        assertThat(message.toString()).isEqualTo("hello world");
    }

    /**
     * Tests valueOf is not formatted.
     */
    @Test
    public void testValueOfNoFormatting() {
        final LocalizableMessage message = LocalizableMessage
                .valueOf("hello %%");
        assertThat(message.toString()).isEqualTo("hello %%");
    }

    /** Tests the check for associated descriptor */
    @Test
    public void testHasDescriptor() throws Exception {
        final LocalizableMessage message = MyTestMessages.FIRST_MESSAGE_WITH_ORDINAL.get();
        assertThat(message.hasDescriptor(MyTestMessages.FIRST_MESSAGE_WITH_ORDINAL)).isTrue();
    }

}
