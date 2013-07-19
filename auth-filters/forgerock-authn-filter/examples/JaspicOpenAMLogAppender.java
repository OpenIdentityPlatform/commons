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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.jaspi.container.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.sun.identity.shared.debug.Debug;

/**
 * Custom Logback Appender to delegate logging calls to the OpenAM Debug logging framework.
 */
public class JaspicOpenAMLogAppender extends AppenderBase<ILoggingEvent> {

    /**
     * The name of the OpenAM Logging file.
     */
    public static final String OPENAM_LOGGING_INSTANCE_NAME = "JaspicAuthN";

    /**
     * Gets the formatted message from the event and calls the OpenAM logging instance.
     *
     * @param eventObject The ILoggingEvent.
     */
    protected void append(ILoggingEvent eventObject) {

        String message = eventObject.getFormattedMessage();

        Debug debug = Debug.getInstance(OPENAM_LOGGING_INSTANCE_NAME);

        if (Level.DEBUG.equals(eventObject.getLevel()) {
            debug.message(message);
        } else if (Level.ERROR.equals(eventObject.getLevel())) {
            debug.error(message);
        } else if (Level.INFO.equals(eventObject.getLevel())) {
            debug.message(message);
        } else if (Level.OFF.equals(eventObject.getLevel())) {
            // Do nothing
        } else if (Level.TRACE.equals(eventObject.getLevel())) {
            debug.message(message);
        } else if (Level.WARN.equals(eventObject.getLevel())) {
            debug.warning(message);
        }
        debug.message(message);
    }
}
