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
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * A {@link SyslogPublisher} implementation that publishes Syslog messages using the TCP protocol.
 */
public class SyslogTcpPublisher extends SyslogPublisher {

    private static final Logger logger = LoggerFactory.getLogger(SyslogPublisher.class);

    private Socket socket = null;
    private int connectTimeout = 30000; // ms

    public SyslogTcpPublisher(InetSocketAddress socketAddress, int connectTimeout) {
        super(socketAddress);
        this.connectTimeout = connectTimeout;
    }

    @Override
    protected void reconnect() throws IOException {
        if (socket == null) {
            connect();
        } else if (!socket.isConnected() || socket.isClosed()) {
            closeConnection();
            connect();
        }
    }

    private void connect() throws IOException {
        socket = new Socket();
        socket.connect(socketAddress, connectTimeout);
        socket.setKeepAlive(true);
    }

    /**
     * Sends the log record to Syslog over TCP in the correct format.
     *
     * Per <a href="https://tools.ietf.org/html/rfc6587#section-3.4.1">RFC 6587</a> the TCP messages should have the
     * following structure:
     * <pre>
     * {@code
     * SYSLOG-FRAME = MSG-LEN SP SYSLOG-MSG
     * MSG-LEN = NONZERO-DIGIT *DIGIT
     * NONZERO-DIGIT = %d49-57
     * }
     * </pre> Additionally <a href="https://tools.ietf.org/html/rfc5424#section-6">RFC 5424</a> defines the followings:
     * <pre>
     * {@code
     * DIGIT = %d48 / NONZERO-DIGIT
     * SP = %d32
     * }
     * </pre>
     *
     * @param bytes The log record's byte[] representation.
     * @throws IOException {@inheritDoc}
     */
    @Override
    protected void sendLogRecord(byte[] bytes) throws IOException {
        OutputStream out = socket.getOutputStream();
        out.write(String.valueOf(bytes.length).getBytes("UTF-8"));
        out.write(' ');
        out.write(bytes);
        out.flush();
    }

    @Override
    protected void closeConnection() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException closeException) {
                logger.warn("Unable to close Syslog TCP connection", closeException);
            }
        }
        socket = null;
    }
}
