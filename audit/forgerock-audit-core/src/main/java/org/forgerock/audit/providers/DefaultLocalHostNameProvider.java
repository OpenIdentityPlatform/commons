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
package org.forgerock.audit.providers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Default implementation of {@link LocalHostNameProvider} using {@link InetAddress} to lookup host name of local host.
 */
public class DefaultLocalHostNameProvider implements LocalHostNameProvider {

    private static final Logger logger = LoggerFactory.getLogger(DefaultLocalHostNameProvider.class);

    @Override
    public String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
            logger.error("Cannot resolve localhost name", uhe);
            return null;
        }
    }
}
