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
 * Copyright 2013 Cybernetica AS
 * Portions copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.audit.handlers.syslog;

import org.forgerock.util.Reject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * SyslogPublisher that transmits messages using the current thread.
 */
class SynchronousSyslogPublisher implements SyslogPublisher {

    /** SyslogConnection through which buffered messages are sent. */
    private final SyslogConnection connection;

    /**
     * Construct a new SynchronousSyslogPublisher.
     *
     * @param connection
     *            a SyslogConnection used for output.
     */
    SynchronousSyslogPublisher(final SyslogConnection connection) {
        Reject.ifNull(connection);
        this.connection = connection;
    }

    @Override
    public void publishMessage(String syslogMessage) throws IOException {
        connection.reconnect();
        connection.send(syslogMessage.getBytes(StandardCharsets.UTF_8));
        connection.flush();
    }

    @Override
    public void close() {
        connection.close();
    }

}
