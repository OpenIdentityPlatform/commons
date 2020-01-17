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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * A {@link SyslogConnection} implementation that publishes Syslog messages using the UDP protocol.
 */
class UdpSyslogConnection implements SyslogConnection {

    private final SocketAddress socketAddress;
    private DatagramSocket datagramSocket;

    public UdpSyslogConnection(InetSocketAddress socketAddress) {
        this.socketAddress = socketAddress;
    }

    @Override
    public void reconnect() throws IOException {
        if (datagramSocket == null) {
            datagramSocket = new DatagramSocket();
        }
    }

    @Override
    public void send(byte[] syslogMessage) throws IOException {
        DatagramPacket packet = new DatagramPacket(syslogMessage, syslogMessage.length, socketAddress);
        datagramSocket.send(packet);
    }

    @Override
    public void flush() throws IOException {
        // do nothing
    }

    @Override
    public void close() {
        if (datagramSocket != null) {
            datagramSocket.close();
        }
        datagramSocket = null;
    }
}