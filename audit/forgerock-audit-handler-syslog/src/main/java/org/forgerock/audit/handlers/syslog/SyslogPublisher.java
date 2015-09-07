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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * This class manages the real socket that is connected to syslog daemon. The socket could be either TCP or UDP socket.
 * If TCP socket is closed, it is (re)opened before sending a message.
 */
public abstract class SyslogPublisher {

    private static final Logger logger = LoggerFactory.getLogger(SyslogPublisher.class);

    protected final SocketAddress socketAddress;

    /**
     * Creates a new SyslogPublisher instance.
     *
     * @param socketAddress The socket address to be used for sending the syslog messages.
     */
    public SyslogPublisher(final InetSocketAddress socketAddress) {
        this.socketAddress = socketAddress;
    }

    /**
     * Flush underlying connection. Sends buffered bytes as UDP datagram or as the next message on TCP stream. The real
     * socket is (re)opened before, if necessary.
     *
     * @param syslogMessages The messages that need to be published to Syslog.
     *
     * @throws IOException If connecting or publishing events to the Syslog daemon failed.
     */
    public void publishSyslogMessages(final List<String> syslogMessages) throws IOException {
        for (String syslogMessage : syslogMessages) {
            byte[] bytes = syslogMessage.getBytes(StandardCharsets.UTF_8);
            try {
                reconnect();
                sendLogRecord(bytes);
            } catch (IOException ex) {
                logger.debug("Unable to publish Syslog message " + syslogMessage, ex);
                closeConnection();
                throw ex;
            }
        }
    }

    /**
     * Checks if the currently opened connection is still valid, and connects/reconnects to the Syslog server if
     * necessary.
     *
     * @throws IOException If there was an IO error while trying to connect to the Syslog server.
     */
    protected abstract void reconnect() throws IOException;

    /**
     * Sends the log record to the syslog server by the means of the chosen protocol (TCP or UDP).
     *
     * @param bytes The logRecord's byte[] representation.
     * @throws IOException If there was an IO error while sending the audit entry to Syslog.
     */
    protected abstract void sendLogRecord(byte[] bytes) throws IOException;

    /**
     * Closes the underlying connection.
     */
    protected abstract void closeConnection();
}
