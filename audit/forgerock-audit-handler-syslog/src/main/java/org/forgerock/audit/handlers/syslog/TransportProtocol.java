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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.audit.handlers.syslog;

import org.forgerock.audit.handlers.syslog.SyslogAuditEventHandlerConfiguration.EventBufferingConfiguration;

import java.net.InetSocketAddress;

/**
 * Transport protocol over which Syslog messages should be published.
 */
public enum TransportProtocol {

    /**
     * Publish Syslog messages over TCP.
     */
    TCP {
        @Override
        SyslogConnection getSyslogConnection(InetSocketAddress socket, SyslogAuditEventHandlerConfiguration config) {
            return new TcpSyslogConnection(socket, config.getConnectTimeout());
        }
    },

    /**
     * Publish Syslog messages over UDP.
     */
    UDP {
        @Override
        SyslogConnection getSyslogConnection(InetSocketAddress socket, SyslogAuditEventHandlerConfiguration config) {
            return new UdpSyslogConnection(socket);
        }
    };

    /**
     * Get the publisher for the given configuration.
     * @param socket The socket.
     * @param config The configuration.
     * @return The publisher.
     */
    public SyslogPublisher getPublisher(InetSocketAddress socket, SyslogAuditEventHandlerConfiguration config) {
        SyslogConnection syslogConnection = getSyslogConnection(socket, config);
        EventBufferingConfiguration buffering = config.getBuffering();
        if (buffering.isEnabled()) {
            return new AsynchronousSyslogPublisher("SyslogHandler", syslogConnection);
        } else {
            return new SynchronousSyslogPublisher(syslogConnection);
        }
    }

    abstract SyslogConnection getSyslogConnection(InetSocketAddress socket,
            SyslogAuditEventHandlerConfiguration config);

}
