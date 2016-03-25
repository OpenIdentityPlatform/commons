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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.audit.handlers.jms;

import javax.jms.ConnectionFactory;
import javax.jms.Topic;

import org.forgerock.json.resource.InternalServerErrorException;

/**
 * Interface for retrieving a {@link Topic JMS topic} and a {@link ConnectionFactory JMS connection factory}.
 */
public interface JmsContextManager {

    /**
     * Gets a {@link Topic JMS topic}.
     * @return a {@link Topic JMS topic}.
     * @throws InternalServerErrorException if an error occurs getting the {@link Topic JMS topic}.
     */
    Topic getTopic() throws InternalServerErrorException;

    /**
     * Gets a {@link ConnectionFactory JMS connection factory}.
     * @return a {@link ConnectionFactory JMS connection factory}.
     * @throws InternalServerErrorException if an error occurs getting
     *      the {@link ConnectionFactory JMS connection factory}.
     */
    ConnectionFactory getConnectionFactory() throws InternalServerErrorException;
}
